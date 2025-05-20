package com.topaz.back.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // Changed from "user" to "users" to avoid reserved keyword issues
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // Added ID field as primary key
    
    @Column(unique = true, nullable = false)
    private String username; // Username is now a unique column, not the primary key
    
    @Column(nullable = false)
    private String password;
    
    private String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role != null && !role.isEmpty() 
            ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            : List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
