package uk.gov.defra.trade.imports.stubs.federated;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The RSA keypairs the three EUDPA-390 simulators sign and verify with (plan Step 6, D6). One
 * pair stands in for AWS STS, one for Entra — generated fresh at process startup, never
 * committed, matching every other hop's "the simulators validate rather than echo" design.
 */
@Component
class SimulatorSigningKeys {

    private final RSAKey stsKey;
    private final RSAKey entraKey;

    SimulatorSigningKeys() {
        this.stsKey = generate("sts-simulator-" + UUID.randomUUID());
        this.entraKey = generate("entra-simulator-" + UUID.randomUUID());
    }

    RSAKey stsKey() {
        return stsKey;
    }

    RSAKey entraKey() {
        return entraKey;
    }

    JWKSet stsJwkSet() {
        return new JWKSet(stsKey.toPublicJWK());
    }

    JWKSet entraJwkSet() {
        return new JWKSet(entraKey.toPublicJWK());
    }

    private static RSAKey generate(String keyId) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(keyId)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to generate an RSA keypair for the federated-credential simulators", ex);
        }
    }
}
