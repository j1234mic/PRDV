package com.prdv.rdv.iam.adapter.out.security;

import com.prdv.rdv.iam.application.port.output.TokenizationPort;
import com.prdv.rdv.iam.config.IamProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Tokenisation deterministe HMAC-SHA256 : meme valeur + meme namespace => meme
 * jeton (jonctions et verifications d'unicite possibles) sans reversibilite.
 * En production, un adapteur vers un coffre Vault/HSM remplace cette classe
 * sans impacter le domaine.
 */
@Component
public class HmacTokenizationAdapter implements TokenizationPort {

    private final byte[] hmacKey;

    public HmacTokenizationAdapter(IamProperties properties) {
        this.hmacKey = properties.getSecurity().getCipher().getSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String tokenize(String sensitiveValue, String namespace) {
        if (sensitiveValue == null) {
            return null;
        }
        String payload = namespace + "|" + sensitiveValue.trim().toUpperCase().replaceAll("\\s+", "");
        return "tok_" + hmacHex(payload).substring(0, 40);
    }

    @Override
    public String mask(String sensitiveValue) {
        if (sensitiveValue == null) {
            return null;
        }
        String normalized = sensitiveValue.replaceAll("\\s+", "").toUpperCase();
        if (normalized.length() <= 8) {
            return "****";
        }
        return normalized.substring(0, 4) + " **** " + normalized.substring(normalized.length() - 4);
    }

    private String hmacHex(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Tokenisation HMAC impossible", e);
        }
    }
}
