package com.topaz.back.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);
    
    // Use a fixed, strong secret key for consistency
    private static final String DEFAULT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    @Value("${jwt.secret:" + DEFAULT_SECRET + "}")
    private String secretKey;
    
    private Key key;
    
    @PostConstruct
    public void init() {
        LOGGER.info("Initializing JWT service");
        try {
            // Use the configured secret key or the default one
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            key = Keys.hmacShaKeyFor(keyBytes);
            LOGGER.info("JWT key initialized successfully");
        } catch (Exception e) {
            LOGGER.warn("Error initializing with provided key, using default key: {}", e.getMessage());
            // Fall back to default key if there's an issue
            byte[] keyBytes = Decoders.BASE64.decode(DEFAULT_SECRET);
            key = Keys.hmacShaKeyFor(keyBytes);
            LOGGER.info("JWT key initialized with default key");
        }
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }
    
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        LOGGER.debug("Generating token for user: {}", userDetails.getUsername());
        try {
            long now = System.currentTimeMillis();
            String token = Jwts.builder()
                    .setClaims(extraClaims)
                    .setSubject(userDetails.getUsername())
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(now + 1000 * 60 * 60 * 24)) // 24 hours
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
            
            LOGGER.debug("Token generated successfully");
            return token;
        } catch (Exception e) {
            LOGGER.error("Failed to generate token: {}", e.getMessage(), e);
            throw new RuntimeException("Could not generate token", e);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            LOGGER.warn("Token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            LOGGER.error("JWT error: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            LOGGER.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
