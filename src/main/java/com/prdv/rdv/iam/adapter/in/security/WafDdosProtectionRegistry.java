package com.prdv.rdv.iam.adapter.in.security;

import com.prdv.rdv.iam.application.port.output.WafDdosProtectionPort;
import com.prdv.rdv.iam.application.result.Views;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Registre temps-reel du WAF applicatif et de la protection anti-DDoS
 * (compteurs d'attaques bloquees, journal des derniers incidents WAF,
 * bannissement automatique temporaire d'adresses IP en rafale).
 */
@Component
public class WafDdosProtectionRegistry implements WafDdosProtectionPort {

    private static final int MAX_RECENT_EVENTS = 100;
    private static final int WAF_VIOLATIONS_BEFORE_BAN = 3;
    private static final Duration AUTO_BAN_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final AtomicLong blockedWafRequests = new AtomicLong();
    private final Deque<Views.WafEventView> recentEvents = new ConcurrentLinkedDeque<>();
    private final Map<String, Instant> blockedIps = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> violationsByIp = new ConcurrentHashMap<>();
    private volatile int activeRulesCount = 6;

    public WafDdosProtectionRegistry(Clock clock) {
        this.clock = clock;
    }

    public void setActiveRulesCount(int count) {
        this.activeRulesCount = count;
    }

    public void recordWafBlock(String ipAddress, String method, String uri, String matchedRule) {
        blockedWafRequests.incrementAndGet();
        Instant now = clock.instant();
        recentEvents.addFirst(new Views.WafEventView(
                ipAddress != null ? ipAddress : "unknown",
                method != null ? method : "UNKNOWN",
                uri != null ? uri : "/",
                matchedRule,
                now));
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.pollLast();
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            int violations = violationsByIp
                    .computeIfAbsent(ipAddress.trim(), k -> new AtomicInteger())
                    .incrementAndGet();
            if (violations >= WAF_VIOLATIONS_BEFORE_BAN) {
                blockIpTemporarily(ipAddress.trim(), AUTO_BAN_DURATION);
            }
        }
    }

    public void blockIpTemporarily(String ipAddress, Duration duration) {
        if (ipAddress != null && !ipAddress.isBlank()) {
            blockedIps.put(ipAddress.trim(), clock.instant().plus(duration));
        }
    }

    public boolean isIpBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        String key = ipAddress.trim();
        Instant until = blockedIps.get(key);
        if (until == null) {
            return false;
        }
        if (clock.instant().isAfter(until)) {
            blockedIps.remove(key);
            violationsByIp.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public boolean isWafEnabled() {
        return true;
    }

    @Override
    public int activeWafRulesCount() {
        return activeRulesCount;
    }

    @Override
    public long blockedWafRequestsCount() {
        return blockedWafRequests.get();
    }

    @Override
    public List<Views.WafEventView> recentWafEvents() {
        return new ArrayList<>(recentEvents);
    }

    @Override
    public List<String> blockedIpAddresses() {
        Instant now = clock.instant();
        blockedIps.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        return new ArrayList<>(new TreeSet<>(blockedIps.keySet()));
    }

    @Override
    public void unblockIp(String ipAddress) {
        if (ipAddress != null) {
            String key = ipAddress.trim();
            blockedIps.remove(key);
            violationsByIp.remove(key);
        }
    }
}
