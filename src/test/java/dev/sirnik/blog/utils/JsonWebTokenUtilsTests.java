package dev.sirnik.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;

import dev.sirnik.blog.utils.JsonWebTokenUtils.JWTPayload;

/**
 * Pins the contract of the registration link token:
 * <ul>
 * <li>{@code encodePayload(key, payload)} returns a compact HS256 token.</li>
 * <li>{@code parsePayload(key, token)} returns the payload of a genuine token.
 * A well-formed token that fails verification (wrong key, edited, wrong claims)
 * gives {@code null}; text that is not a JWS at all throws
 * {@link ParseException}; a key under 256 bits throws
 * {@link JOSEException}.</li>
 * </ul>
 * Plain unit tests: no Spring context, so they run in milliseconds.
 */
class JsonWebTokenUtilsTests {

    // 32 characters = 256 bits, the minimum Nimbus accepts for HS256.
    private static final String KEY = "0123456789abcdef0123456789abcdef";
    private static final String OTHER_KEY = "fedcba9876543210fedcba9876543210";
    private static final JWTPayload PAYLOAD = new JWTPayload(
        "ada",
        "ada@example.com",
        1_780_000_000_000L
    );

    @Test
    void encodedTokenIsACompactJwsSignedWithHs256() throws Exception {
        String token = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);

        String[] parts = token.split("\\.", -1);

        assertThat(parts).hasSize(3);
        assertThat(parts).allSatisfy(part -> assertThat(part).isNotBlank());
        assertThat(decodeBase64Url(parts[0])).contains("\"alg\":\"HS256\"");
    }

    @Test
    void parseReturnsThePayloadThatWasEncoded() throws Exception {
        String token = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);

        assertThat(JsonWebTokenUtils.parsePayload(KEY, token))
            .isEqualTo(PAYLOAD);
    }

    @Test
    void unicodeAndPunctuationSurviveTheRoundTrip() throws Exception {
        JWTPayload tricky = new JWTPayload(
            "Zoë \"Z\" O'Brien",
            "zoe+blog@example.com",
            PAYLOAD.creationTimestamp()
        );

        String token = JsonWebTokenUtils.encodePayload(KEY, tricky);

        assertThat(JsonWebTokenUtils.parsePayload(KEY, token))
            .isEqualTo(tricky);
    }

    @Test
    void sameInputsAndKeyGiveTheSameToken() throws Exception {
        // HMAC is deterministic. If this ever fails, something random (a nonce,
        // an issued-at, a map ordering issue) has crept into the payload.
        String first = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);
        String second = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void differentPayloadsGiveDifferentTokens() throws Exception {
        String token = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);
        String otherEmail = JsonWebTokenUtils
            .encodePayload(
                KEY, new JWTPayload(
                    PAYLOAD.username(),
                    "grace@example.com",
                    PAYLOAD.creationTimestamp()
                )
            );
        String otherTime = JsonWebTokenUtils
            .encodePayload(
                KEY, new JWTPayload(
                    PAYLOAD.username(),
                    PAYLOAD.email(),
                    PAYLOAD.creationTimestamp() + 1
                )
            );

        assertThat(otherEmail).isNotEqualTo(token);
        assertThat(otherTime).isNotEqualTo(token);
    }

    @Test
    void parseReturnsNullForATokenSignedWithADifferentKey() throws Exception {
        String token = JsonWebTokenUtils.encodePayload(OTHER_KEY, PAYLOAD);

        assertThat(JsonWebTokenUtils.parsePayload(KEY, token)).isNull();
    }

    @Test
    void parseReturnsNullForATokenWhosePayloadWasEdited() throws Exception {
        String token = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);
        String[] parts = token.split("\\.");

        // Keep the genuine header and signature, swap in a payload that names
        // someone else. Only the signature check can catch this.
        String forgedPayload = encodeBase64Url(
            "{\"name\":\"ada\",\"email\":\"attacker@evil.com\","
                + "\"timestamp\":" + PAYLOAD.creationTimestamp() + "}"
        );
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThat(JsonWebTokenUtils.parsePayload(KEY, forged)).isNull();
    }

    @Test
    void parseReturnsNullForAFlippedSignature() throws Exception {
        String token = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);
        String[] parts = token.split("\\.");

        // Change the first signature character to a different valid one.
        char first = parts[2].charAt(0);
        char swapped = first == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + swapped
            + parts[2].substring(1);

        assertThat(JsonWebTokenUtils.parsePayload(KEY, tampered)).isNull();
    }

    @Test
    void parseThrowsForATokenWithTheSignatureRemoved() throws Exception {
        String token = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);
        String[] parts = token.split("\\.");

        String stripped = parts[0] + "." + parts[1] + ".";

        assertThatThrownBy(() -> JsonWebTokenUtils.parsePayload(KEY, stripped))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void parseThrowsForAnUnsignedAlgNoneToken() {
        // The classic JWT attack: claim "alg":"none" so a lazy verifier skips
        // the signature check entirely.
        String header = encodeBase64Url("{\"alg\":\"none\"}");
        String payload = encodeBase64Url(
            "{\"name\":\"ada\",\"email\":\"attacker@evil.com\","
                + "\"timestamp\":" + PAYLOAD.creationTimestamp() + "}"
        );
        String unsigned = header + "." + payload + ".";

        assertThatThrownBy(() -> JsonWebTokenUtils.parsePayload(KEY, unsigned))
            .isInstanceOf(ParseException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "garbage", "a.b", "a.b.c", "a.b.c.d",
        "...",})
    void parseThrowsForMalformedInput(String malformed) {
        assertThatThrownBy(() -> JsonWebTokenUtils.parsePayload(KEY, malformed))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void parseReturnsNullWhenAGenuinelySignedTokenHasTooFewClaims()
        throws Exception {
        String token = signedToken(
            KEY, Map.of("name", "ada", "email", "ada@example.com")
        );

        assertThat(JsonWebTokenUtils.parsePayload(KEY, token)).isNull();
    }

    @Test
    void parseReturnsNullWhenAGenuinelySignedTokenHasTooManyClaims()
        throws Exception {
        String token = signedToken(
            KEY,
            Map
                .of(
                    "name", "ada", "email", "ada@example.com", "timestamp", 1L,
                    "admin", true
                )
        );

        assertThat(JsonWebTokenUtils.parsePayload(KEY, token)).isNull();
    }

    @Test
    void parseReturnsNullWhenAGenuinelySignedTokenHasTheWrongClaimNames()
        throws Exception {
        // Three claims, so the size check passes, but "timestamp" is missing.
        String token = signedToken(
            KEY, Map.of("name", "ada", "email", "ada@example.com", "time", 1L)
        );

        assertThat(JsonWebTokenUtils.parsePayload(KEY, token)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "0123456789abcdef0123456789abcde",})
    void aKeyUnder256BitsIsRejectedWhenEncoding(String weakKey) {
        // 31 characters is the boundary case: one byte short of HS256's
        // minimum.
        assertThatThrownBy(
            () -> JsonWebTokenUtils.encodePayload(weakKey, PAYLOAD)
        ).isInstanceOf(JOSEException.class);
    }

    @Test
    void aKeyUnder256BitsIsRejectedWhenParsing() throws Exception {
        String token = JsonWebTokenUtils.encodePayload(KEY, PAYLOAD);

        assertThatThrownBy(() -> JsonWebTokenUtils.parsePayload("short", token))
            .isInstanceOf(JOSEException.class);
    }

    /** Signs arbitrary claims, bypassing encodePayload's fixed three claims. */
    private static String signedToken(
        String key,
        Map<String, Object> claims
    ) throws JOSEException {
        JWSObject jws = new JWSObject(
            new JWSHeader(JWSAlgorithm.HS256),
            new Payload(claims)
        );
        jws.sign(new MACSigner(key));
        return jws.serialize();
    }

    private static String encodeBase64Url(String json) {
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeBase64Url(String part) {
        return new String(
            Base64.getUrlDecoder().decode(part),
            StandardCharsets.UTF_8
        );
    }
}
