package pt.ipleiria.estg.dei.ei.dae.projeto.security;

import io.jsonwebtoken.Jwts;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TokenIssuer {
    // protected static final byte[] SECRET_KEY =
    // "veRysup3rstr0nginv1ncible5ecretkeY@academics.dae.ipleiria".getBytes();
    protected static final byte[] SECRET_KEY = loadSecret();

    protected static final String ALGORITHM = "HMACSHA384";
    public static final long EXPIRY_MINS = 60L;

    public static String issue(String username, String role) {
        var expiryPeriod = LocalDateTime.now().plusMinutes(EXPIRY_MINS);
        var expirationDateTime = Date.from(
                expiryPeriod.atZone(ZoneId.systemDefault()).toInstant());
        Key key = new SecretKeySpec(SECRET_KEY, ALGORITHM);
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(expirationDateTime)
                .signWith(key)
                .compact();
    }

    private static byte[] loadSecret() {
        String s = System.getenv("JWT_SECRET");
        if (s == null || s.length() < 48) { // HS384 exige >= 384 bits
            throw new IllegalStateException("Defina JWT_SECRET com pelo menos 48 caracteres");
        }
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
