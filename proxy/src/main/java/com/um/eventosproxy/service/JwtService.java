package com.um.eventosproxy.service;

import com.um.eventosproxy.config.ProxyProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String AUTHORITIES_CLAIM = "auth";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ProxyProperties proxyProperties;

    public JwtService(ProxyProperties proxyProperties) {
        this.proxyProperties = proxyProperties;
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(proxyProperties.getBackend().getJwt().getTokenValidityInSeconds());

        SecretKey key = getSigningKey();

        return Jwts.builder()
            .subject(username)
            .claim(AUTHORITIES_CLAIM, ROLE_ADMIN)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(key)
            .compact();
    }

    private SecretKey getSigningKey() {
        String secret = proxyProperties.getBackend().getJwt().getSecret();
        // El secreto viene en Base64 desde la configuración del backend
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }
}

