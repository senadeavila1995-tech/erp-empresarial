package com.erp.users;

import com.erp.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sellerTokenReturns403() throws Exception {
        AppUser seller = new AppUser(
                "vendedor-test",
                "password-no-utilizada",
                Role.VENDEDOR
        );

        when(userRepository.findByUsername(seller.getUsername()))
                .thenReturn(Optional.of(seller));
        String token = jwtService.generateToken(seller);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenReturns200() throws Exception {
        AppUser admin = new AppUser(
                "admin",
                "password-no-utilizada",
                Role.ADMIN
        );

        when(userRepository.findByUsername(admin.getUsername()))
                .thenReturn(Optional.of(admin));
        String token = jwtService.generateToken(admin);
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void inactiveAdminTokenReturns401() throws Exception {
        AppUser admin = new AppUser(
                "admin-inactivo",
                "password-no-utilizada",
                Role.ADMIN
        );
        admin.setActivo(false);

        when(userRepository.findByUsername(admin.getUsername()))
                .thenReturn(Optional.of(admin));
        String token = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

}
