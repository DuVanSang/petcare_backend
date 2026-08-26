package com.petcare.backend.security;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.model.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
 @Mock JwtService jwt;@Mock CustomUserDetailsService details;@Mock FilterChain chain;
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 private UserPrincipal principal(){User u=new User();u.setId(1L);u.setEmail("user@test.com");u.setStatus("active");return UserPrincipal.from(u);}
 private JwtAuthenticationFilter filter(){return new JwtAuthenticationFilter(jwt,details,new ObjectMapper());}
 @Test void missingNonBearerAndExistingAuthentication_OnlyContinueChain() throws Exception {MockHttpServletRequest req=new MockHttpServletRequest();MockHttpServletResponse res=new MockHttpServletResponse();filter().doFilterInternal(req,res,chain);req.addHeader("Authorization","Basic abc");filter().doFilterInternal(req,res,chain);SecurityContextHolder.getContext().setAuthentication(mock(org.springframework.security.core.Authentication.class));req.removeHeader("Authorization");req.addHeader("Authorization","Bearer valid");filter().doFilterInternal(req,res,chain);verify(chain,times(3)).doFilter(req,res);verifyNoInteractions(jwt,details);}
 @Test void validBearer_SetsAuthenticationAndContinuesChain() throws Exception {MockHttpServletRequest req=new MockHttpServletRequest();req.addHeader("Authorization","Bearer valid");MockHttpServletResponse res=new MockHttpServletResponse();UserDetails user=principal();when(jwt.extractUsername("valid")).thenReturn("user@test.com");when(details.loadUserByUsername("user@test.com")).thenReturn(user);when(jwt.isTokenValid("valid",user)).thenReturn(true);filter().doFilterInternal(req,res,chain);org.assertj.core.api.Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(user);verify(chain).doFilter(req,res);}
 @Test void invalidToken_ClearsContextWritesUnauthorizedAndDoesNotContinue() throws Exception {MockHttpServletRequest req=new MockHttpServletRequest();req.addHeader("Authorization","Bearer invalid");MockHttpServletResponse res=new MockHttpServletResponse();when(jwt.extractUsername("invalid")).thenThrow(new JwtException("bad"){});filter().doFilterInternal(req,res,chain);org.assertj.core.api.Assertions.assertThat(res.getStatus()).isEqualTo(401);org.assertj.core.api.Assertions.assertThat(res.getContentType()).contains("application/json");verifyNoInteractions(chain);}
}
