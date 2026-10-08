package dev.sirnik.blog.utils;

import java.text.ParseException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;

public class JsonWebTokenUtils {

    // Also the lifetime of an unapproved registration: the startup cleanup
    // deletes pending users older than this.
    public static final long MAX_TIME_VERIFY = Duration.ofHours(1).toMillis();

    private JsonWebTokenUtils() {
    }

    public static String encodePayload(
        String privateKey,
        JWTPayload payload
    ) throws JOSEException {
        Map<String, Object> map = Map
            .of(
                "name", payload.username(), "email", payload.email(),
                "timestamp", payload.creationTimestamp()
            );

        JWSSigner signer = new MACSigner(privateKey);
        JWSObject jwsObject = new JWSObject(
            new JWSHeader(JWSAlgorithm.HS256),
            new Payload(map)
        );

        jwsObject.sign(signer);

        return jwsObject.serialize();
    }

    public static JWTPayload parsePayload(
        String privateKey,
        String payload
    ) throws JOSEException, ParseException {
        JWSObject jwsObject = JWSObject.parse(payload);
        JWSVerifier verifier = new MACVerifier(privateKey);

        if (!jwsObject.verify(verifier)) {
            return null;
        }

        Map<String, Object> map = jwsObject.getPayload().toJSONObject();

        if (map.size() != 3) {
            return null;
        }

        JWTClaimsSet claims = JWTClaimsSet.parse(map);

        String name = claims.getStringClaim("name");
        String email = claims.getStringClaim("email");
        Long timestamp = claims.getLongClaim("timestamp");

        if (name == null || email == null || timestamp == null) {
            return null;
        }

        return new JWTPayload(
            name,
            email,
            timestamp
        );
    }

    public static boolean verifyPayload(
        JWTPayload payload,
        AdminUserRepository adminRepo,
        long currentTimestamp
    ) {
        Optional<AdminUser> user = adminRepo
            .findInactiveForVerification(payload.username(), payload.email());

        if (user.isEmpty()) {
            return false;
        }

        AdminUser unwrapped = user.get();

        if (unwrapped.getCreatedAt().toEpochMilli() != payload
            .creationTimestamp()) {
            return false;
        }

        return unwrapped.getCreatedAt().toEpochMilli()
            + MAX_TIME_VERIFY >= currentTimestamp;
    }

    public record JWTPayload(
        String username,
        String email,
        long creationTimestamp
    ) {
    }
}
