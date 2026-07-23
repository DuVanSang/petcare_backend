package com.petcare.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.petcare.backend.model.User;
import com.petcare.backend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserPrincipalAndDetailsServiceTest {
 @Mock UserRepository users;
 private User user(String role,String status){User u=new User();u.setId(1L);u.setEmail("user@test.com");u.setPasswordHash("hash");u.setRole(role);u.setStatus(status);return u;}
 @Test void principal_MapsRoleDefaultAndBannedLockState(){UserPrincipal admin=UserPrincipal.from(user("admin","active"));assertThat(admin.getUsername()).isEqualTo("user@test.com");assertThat(admin.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");assertThat(admin.isAccountNonLocked()).isTrue();UserPrincipal defaultRole=UserPrincipal.from(user(null,"BANNED"));assertThat(defaultRole.getAuthorities()).extracting(Object::toString).contains("ROLE_USER");assertThat(defaultRole.isAccountNonLocked()).isFalse();}
 @Test void detailsService_LoadsUserAndThrowsForMissingEmail(){CustomUserDetailsService service=new CustomUserDetailsService(users);when(users.findByEmail("user@test.com")).thenReturn(Optional.of(user("user","active")));assertThat(service.loadUserByUsername("user@test.com").getPassword()).isEqualTo("hash");when(users.findByEmail("missing@test.com")).thenReturn(Optional.empty());assertThatThrownBy(()->service.loadUserByUsername("missing@test.com")).isInstanceOf(UsernameNotFoundException.class);}
}
