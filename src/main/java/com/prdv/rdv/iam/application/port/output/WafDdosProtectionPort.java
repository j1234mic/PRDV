package com.prdv.rdv.iam.application.port.output;

import java.util.List;

/**
 * Port de supervision et de pilotage du pare-feu applicatif (WAF) et de la
 * protection anti-DDoS (blocage d'IP en cas de rafale).
 */
public interface WafDdosProtectionPort {

    boolean isWafEnabled();

    int activeWafRulesCount();

    long blockedWafRequestsCount();

    List<String> recentWafEvents();

    List<String> blockedIpAddresses();

    void unblockIp(String ipAddress);
}
