package com.erp.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createEncodesPasswordAndSavesUser() {
        UserCreateRequest request = new UserCreateRequest(
                "  vendedor01  ",
                "PasswordSegura123!",
                Role.VENDEDOR
        );

        when(repository.existsByUsername("vendedor01")).thenReturn(false);
        when(passwordEncoder.encode("PasswordSegura123!"))
                .thenReturn("bcrypt-hash");
        when(repository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(request);

        ArgumentCaptor<AppUser> captor =
                ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(captor.capture());

        AppUser saved = captor.getValue();

        assertEquals("vendedor01", saved.getUsername());
        assertEquals("bcrypt-hash", saved.getPassword());
        assertEquals(Role.VENDEDOR, saved.getRole());
        assertEquals("vendedor01", response.username());
        assertEquals(Role.VENDEDOR, response.role());
        verify(passwordEncoder).encode("PasswordSegura123!");
    }

    @Test
    void createRejectsDuplicateUsername() {
        UserCreateRequest request = new UserCreateRequest(
                "admin",
                "PasswordSegura123!",
                Role.VENDEDOR
        );

        when(repository.existsByUsername("admin")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.create(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any(AppUser.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void findAllReturnsUserResponsesWithoutPasswords() {
        when(repository.findAll()).thenReturn(List.of(
                new AppUser("vendedor01", "hash-privado", Role.VENDEDOR),
                new AppUser("contador01", "otro-hash", Role.CONTADOR)
        ));

        List<UserResponse> responses = userService.findAll();

        assertEquals(2, responses.size());
        assertEquals("vendedor01", responses.get(0).username());
        assertEquals(Role.VENDEDOR, responses.get(0).role());
        assertEquals("contador01", responses.get(1).username());
        assertEquals(Role.CONTADOR, responses.get(1).role());

        verify(repository).findAll();
    }

    @Test
    void setActiveDeactivatesAndReactivatesUser() {
        AppUser user = new AppUser(
                "vendedor01", "hash", Role.VENDEDOR);

        when(repository.findById(7L)).thenReturn(java.util.Optional.of(user));
        when(repository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse deactivated = userService.setActive(7L, false);

        assertFalse(user.isEnabled());
        assertFalse(deactivated.activo());

        UserResponse reactivated = userService.setActive(7L, true);

        assertTrue(user.isEnabled());
        assertTrue(reactivated.activo());
        verify(repository, times(2)).save(user);
    }

    @Test
    void setActiveReturnsNotFoundForUnknownUser() {
        when(repository.findById(999L))
                .thenReturn(java.util.Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.setActive(999L, false)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(repository, never()).save(any(AppUser.class));
    }

}