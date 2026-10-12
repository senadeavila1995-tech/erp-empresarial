package com.erp.inventory;

import com.erp.auth.JwtService;
import com.erp.users.UserRepository;
import com.erp.users.AppUser;
import com.erp.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MovimientoInventarioAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @MockitoBean
    private MovimientoInventarioService service;

    private String token(String username, Role role) {
        AppUser user = new AppUser(username, "password-no-utilizada", role);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        return jwtService.generateToken(user);
    }

    private org.springframework.test.web.servlet.ResultActions registrar(
            String username, Role role, String tipo) throws Exception {
        return mockMvc.perform(post("/api/products/1/movements")
                .header("Authorization", "Bearer " + token(username, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "tipo": "%s",
                          "cantidad": 1,
                          "motivo": "Prueba de permisos"
                        }
                        """.formatted(tipo)));
    }

    @Test
    void vendedorNoPuedeRegistrarEntrada() throws Exception {
        registrar("vendedor-test", Role.VENDEDOR, "ENTRADA")
                .andExpect(status().isForbidden());
    }

    @Test
    void almacenNoPuedeRegistrarAjustes() throws Exception {
        registrar("almacen-test", Role.ALMACEN, "AJUSTE")
                .andExpect(status().isForbidden());
    }

    @Test
    void gerentePuedeSolicitarAjuste() throws Exception {
        registrar("gerente-test", Role.GERENTE, "AJUSTE")
                .andExpect(status().isCreated());

        verify(service).registrar(
                eq(1L), any(MovimientoInventarioRequest.class),
                eq("gerente-test"));
    }
}
