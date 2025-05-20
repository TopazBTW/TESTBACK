package com.topaz.back.services;

import com.topaz.back.entities.User;
import com.topaz.back.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            LOGGER.error("Username is null or empty");
            throw new UsernameNotFoundException("Username cannot be null or empty");
        }

        LOGGER.debug("Loading user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    LOGGER.error("User not found: {}", username);
                    return new UsernameNotFoundException("Utilisateur non trouvé: " + username);
                });

        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            LOGGER.warn("User {} has no role assigned, defaulting to ROLE_USER", username);
            user.setRole("USER");
        }

        LOGGER.debug("User found: {}, role: {}, authorities: {}",
                username, user.getRole(), user.getAuthorities());
        return user; // Assumes User implements UserDetails
    }
}
