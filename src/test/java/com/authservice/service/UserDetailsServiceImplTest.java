package com.authservice.service;

import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl – Unit Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    private User buildUser(UUID id, boolean enabled, boolean locked) {
        Role role = Role.builder().id(UUID.randomUUID()).name("ROLE_USER").build();
        return User.builder()
                .id(id)
                .username("testuser")
                .email("test@example.com")
                .password("hashed")
                .enabled(enabled)
                .locked(locked)
                .roles(Set.of(role))
                .build();
    }

    @Test
    @DisplayName("loads user by email successfully")
    void loadByEmail_success() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, true, false);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertThat(details.getUsername()).isEqualTo(id.toString());
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    @DisplayName("loads user by username when email lookup misses")
    void loadByUsername_fallback() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, true, false);
        given(userRepository.findByEmail("testuser")).willReturn(Optional.empty());
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("testuser");

        assertThat(details.getUsername()).isEqualTo(id.toString());
    }

    @Test
    @DisplayName("throws UsernameNotFoundException when user not found")
    void loadUser_notFound_throws() {
        given(userRepository.findByEmail("nobody")).willReturn(Optional.empty());
        given(userRepository.findByUsername("nobody")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("returns locked=true for a locked user")
    void loadUser_lockedUser_accountNonLockedFalse() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, true, true);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("returns disabled user correctly")
    void loadUser_disabledUser() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, false, false);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertThat(details.isEnabled()).isFalse();
    }
}
