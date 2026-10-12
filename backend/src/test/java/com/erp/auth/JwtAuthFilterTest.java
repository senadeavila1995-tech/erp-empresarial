package com.erp.auth;

import com.erp.users.AppUser;
import com.erp.users.Role;
import com.erp.users.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private JwtService jwtService;
    private UserRepository userRepository;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userRepository = mock(UserRepository.class);
        filter = new JwtAuthFilter(jwtService, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenUsesCurrentDatabaseRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtService.extractClaims("seller-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("vendedor01");

        // El token dice ADMIN, pero MySQL tiene al usuario como VENDEDOR.
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(userRepository.findByUsername("vendedor01"))
                .thenReturn(Optional.of(
                        new AppUser("vendedor01", "hash", Role.VENDEDOR)));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer seller-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals("vendedor01", authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VENDEDOR")));
        assertFalse(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(userRepository).findByUsername("vendedor01");
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenIsNotAcceptedWhenUserNoLongerExists() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtService.extractClaims("old-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("usuarioeliminado");
        when(userRepository.findByUsername("usuarioeliminado"))
                .thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer old-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTokenDoesNotAuthenticateUser() throws Exception {
        when(jwtService.extractClaims("invalid-token"))
                .thenThrow(new io.jsonwebtoken.JwtException("Token inválido"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void requestWithoutTokenRemainsUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void inactiveUserIsNotAuthenticatedEvenWithValidToken() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtService.extractClaims("inactive-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("usuario-inactivo");

        AppUser user = new AppUser(
                "usuario-inactivo", "hash", Role.VENDEDOR);
        user.setActivo(false);

        when(userRepository.findByUsername("usuario-inactivo"))
                .thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer inactive-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userRepository).findByUsername("usuario-inactivo");
        verify(chain).doFilter(request, response);
    }

}
