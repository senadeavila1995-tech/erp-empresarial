package com.erp.suppliers;

import com.erp.auth.JwtService;
import com.erp.users.AppUser;
import com.erp.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProveedorAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ProveedorService service;

    private String token(String username, Role role) {
        return jwtService.generateToken(
            new AppUser(username, "password-no-utilizada", role)
        );
    }

    @Test
    void consultarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/suppliers"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorNoPuedeCrearProveedor() throws Exception {
        mockMvc.perform(post("/api/suppliers")
                .header("Authorization",
                    "Bearer " + token("vendedor-test", Role.VENDEDOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoIdentificacion": "NIT",
                      "identificacion": "900123456",
                      "nombre": "Proveedor de prueba"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void almacenPuedeConsultarProveedores() throws Exception {
        when(service.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/suppliers")
                .header("Authorization",
                    "Bearer " + token("almacen-test", Role.ALMACEN)))
            .andExpect(status().isOk());
    }

    @Test
    void gerentePuedeCrearProveedor() throws Exception {
        ProveedorResponse response = new ProveedorResponse(
            1L, "NIT", "900123456", "Proveedor de prueba",
            null, null, null, null, true
        );

        when(service.crear(any(ProveedorRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/api/suppliers")
                .header("Authorization",
                    "Bearer " + token("gerente-test", Role.GERENTE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoIdentificacion": "NIT",
                      "identificacion": "900123456",
                      "nombre": "Proveedor de prueba"
                    }
                    """))
            .andExpect(status().isCreated());

        verify(service).crear(any(ProveedorRequest.class));
    }


    @Test
    void vendedorNoPuedeActualizarProveedor() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/suppliers/1")
                .header("Authorization",
                    "Bearer " + token("vendedor-put-test", Role.VENDEDOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoIdentificacion": "NIT",
                      "identificacion": "900123456",
                      "nombre": "Proveedor actualizado"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void contadorNoPuedeDesactivarProveedor() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .patch("/api/suppliers/1/deactivate")
                    .header("Authorization",
                        "Bearer " + token("contador-patch-test", Role.CONTADOR)))
            .andExpect(status().isForbidden());
    }

}
