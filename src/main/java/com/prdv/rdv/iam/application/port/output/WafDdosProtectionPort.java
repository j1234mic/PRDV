package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Port de supervision et de pilotage du pare-feu applicatif (WAF) et de la
 * protection anti-DDoS (blocage d'IP en cas de rafale).
 */
public interface WafDdosProtectionPort {

    boolean isWafEnabled();

    int activeWafRulesCount();

    long blockedWafRequestsCount();

    List<Views.WafEventView> recentWafEvents();

    List<String> blockedIpAddresses();

    void unblockIp(String ipAddress);
}
