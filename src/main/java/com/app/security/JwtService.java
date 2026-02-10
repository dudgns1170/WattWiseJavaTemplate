package com.app.security;

import com.app.config.AppProps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 토큰 발급 및 검증 서비스
 * 
 * Access Token과 Refresh Token의 생성, 파싱, 검증을 담당합니다.
 */
@Service
public class JwtService {
    private final Key key;
    private final String issuer;
    private final long accessTtlMin;
    private final long refreshTtlDays;

    public JwtService(AppProps props) {
        var jwt = props.getJwt();
        this.issuer = jwt.getIssuer();
        this.accessTtlMin = jwt.getAccessTtlMinutes();
        this.refreshTtlDays = jwt.getRefreshTtlDays();
        String sec = ensureBase64(jwt.getSecret());
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(sec));
    }

    private String ensureBase64(String raw) {
        try { Decoders.BASE64.decode(raw); return raw; }
        catch (Exception e) { return io.jsonwebtoken.io.Encoders.BASE64.encode(raw.getBytes()); }
    }

    public String issueAccess(String subject, Map<String,Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(subject).setIssuer(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(accessTtlMin * 60)))
            .addClaims(claims)
            .claim("typ","at").claim("jti", UUID.randomUUID().toString())
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public String issueRefresh(String subject, String familyId, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(subject).setIssuer(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(refreshTtlDays * 24 * 3600)))
            .claim("typ","rt").claim("fid", familyId).claim("jti", jti)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .requireIssuer(issuer)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
