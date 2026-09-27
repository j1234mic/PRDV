package com.prdv.rdv.profile.adapter.out.external;

import com.prdv.rdv.profile.application.port.output.DmpGatewayPort;
import com.prdv.rdv.profile.config.ProfileProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Passerelle DMP de developpement (Anti-Corruption Layer).
 *
 * <p>Simule les echanges avec « Mon espace sante » (CNAM) : identifiants
 * stables, references de publication et journalisation des appels. En
 * production, un adapteur appele l'API reelle (authentification CPS /
 * consentement patient) en implementant le meme port : ni le domaine ni les
 * cas d'usage ne changent.
 */
@Component
public class StubDmpGatewayAdapter implements DmpGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StubDmpGatewayAdapter.class);

    private final ProfileProperties properties;

    public StubDmpGatewayAdapter(ProfileProperties properties) {
        this.properties = properties;
    }

    /**
     * {@code prdv.profile.dmp.enabled=true} annonce une passerelle reelle : comme
     * aucune n'est livree dans ce module, on le signale explicitement plutot que
     * de laisser croire que les echanges ont vraiment eu lieu.
     */
    private void warnIfRealGatewayExpected(String operation) {
        if (properties.getDmp().isEnabled()) {
            log.warn("[DMP-STUB] prdv.profile.dmp.enabled=true mais aucune passerelle reelle n'est "
                    + "enregistree : operation '{}' simulee", operation);
        }
    }

    @Override
    public String linkPatient(Long patientUserId, String socialSecurityNumberToken) {
        warnIfRealGatewayExpected("linkPatient");
        String identifier = "DMP-" + patientUserId + "-"
                + UUID.nameUUIDFromBytes(("dmp|" + patientUserId).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString().substring(0, 8);
        log.info("[DMP-STUB] Rattachement du patient {} (NIR tokenise {}) : {}", patientUserId,
                socialSecurityNumberToken == null ? "absent" : "present", identifier);
        return identifier;
    }

    @Override
    public String publishDocument(Long patientUserId, String dmpIdentifier, String documentTitle,
                                  String contentType) {
        warnIfRealGatewayExpected("publishDocument");
        String reference = "DMPDOC-" + UUID.randomUUID().toString().substring(0, 12);
        log.info("[DMP-STUB] Publication du document « {} » ({}) dans {} : {}", documentTitle, contentType,
                dmpIdentifier, reference);
        return reference;
    }

    @Override
    public int pullUpdates(Long patientUserId, String dmpIdentifier) {
        warnIfRealGatewayExpected("pullUpdates");
        log.info("[DMP-STUB] Synchronisation du DMP {} du patient {} : aucun element distant simule",
                dmpIdentifier, patientUserId);
        return 0;
    }

    @Override
    public void revokeSharing(Long patientUserId, String dmpIdentifier) {
        warnIfRealGatewayExpected("revokeSharing");
        log.info("[DMP-STUB] Retrait du consentement de partage pour le DMP {} du patient {}",
                dmpIdentifier, patientUserId);
    }
}
