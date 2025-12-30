package com.example.top_hog_server.security.jwt;

import com.example.top_hog_server.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthTokenFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthTokenFilter authTokenFilter;

    @BeforeEach
    public void setUp() {
        // Since AuthTokenFilter uses field injection, we might need to set them manually if InjectMocks doesn't handle it or if we want to be sure.
        // But InjectMocks should work for the fields.
    }

    @Test
    public void testDoFilterInternal_WithAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer test.token.jwt");
        when(request.getServletPath()).thenReturn("/api/some/protected/resource");
        when(jwtUtils.validateJwtToken("test.token.jwt")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("test.token.jwt")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        authTokenFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtils, times(1)).validateJwtToken("test.token.jwt");
        verify(userDetailsService, times(1)).loadUserByUsername("testuser");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternal_WithAuthHeader_Success() throws Exception {
        // Now expecting success
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("auth")).thenReturn("test.token.jwt"); // Mocking the 'auth' header
        when(request.getServletPath()).thenReturn("/api/some/protected/resource");
        when(jwtUtils.validateJwtToken("test.token.jwt")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("test.token.jwt")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        authTokenFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtils, times(1)).validateJwtToken("test.token.jwt");
        verify(userDetailsService, times(1)).loadUserByUsername("testuser");
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
