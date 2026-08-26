package com.petcare.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class RestSecurityHandlersTest {
 @Test void entryPoint_WritesUnauthorizedJsonResponse() throws Exception {MockHttpServletResponse response=new MockHttpServletResponse();new RestAuthenticationEntryPoint(new ObjectMapper()).commence(new MockHttpServletRequest(),response,new BadCredentialsException("bad"));assertThat(response.getStatus()).isEqualTo(401);assertThat(response.getContentType()).contains("application/json");assertThat(response.getContentAsString()).contains("Bạn cần đăng nhập");}
 @Test void accessDenied_WritesForbiddenJsonResponse() throws Exception {MockHttpServletResponse response=new MockHttpServletResponse();new RestAccessDeniedHandler(new ObjectMapper()).handle(new MockHttpServletRequest(),response,new AccessDeniedException("no"));assertThat(response.getStatus()).isEqualTo(403);assertThat(response.getContentType()).contains("application/json");assertThat(response.getContentAsString()).contains("Bạn không có quyền");}
}
