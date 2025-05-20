package com.topaz.back.controllers;

import com.topaz.back.dtos.AuthRequest;
import com.topaz.back.dtos.AuthResponse;
import com.topaz.back.dtos.RegisterRequest;
import com.topaz.back.entities.User;
import com.topaz.back.services.JwtService;
import com.topaz.back.services.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        logger.info("Login attempt for user: {}", request.getUsername());
        
        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            // Get user details from the authenticated user
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Create claims with roles
            Map<String, Object> claims = new HashMap<>();
            List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
            claims.put("roles", roles);
            
            // Generate token with claims
            String token = jwtService.generateToken(claims, userDetails);
            
            logger.info("Login successful for user: {}", request.getUsername());
            
            // Return the token and user info
            return ResponseEntity.ok(new AuthResponse(
                token,
                userDetails.getUsername(),
                roles
            ));
            
        } catch (BadCredentialsException e) {
            logger.warn("Login failed for user {}: Bad credentials", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            logger.error("Login error for user {}: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        logger.info("Registration attempt for user: {}", request.getUsername());
        
        try {
            User user = userService.register(request);
            logger.info("Registration successful for user: {}", request.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Registration error for user {}: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        logger.info("Token validation request");
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Invalid authorization header"));
            }
            
            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            
            if (username != null) {
                return ResponseEntity.ok(Map.of("valid", true, "username", username));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Invalid token"));
            }
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}
