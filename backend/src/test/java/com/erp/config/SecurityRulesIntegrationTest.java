package com.erp.config;

import com.erp.auth.JwtService;
import com.erp.users.AppUser;
import com.erp.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private com.erp.purchases.CompraService compraService;

    @MockitoBean
    private com.erp.sales.VentaService ventaService;

    private String token(String username, Role role) {
        return jwtService.generateToken(
                new AppUser(username, "password-no-utilizada", role));
    }

    @Test
    void comprasSinTokenDevuelven401() throws Exception {
        mockMvc.perform(get("/api/purchases"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorNoPuedeConsultarCompras() throws Exception {
        mockMvc.perform(get("/api/purchases")
                        .header("Authorization",
                                "Bearer " + token("vendedor-test", Role.VENDEDOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void contadorPuedeConsultarCompras() throws Exception {
        when(compraService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/purchases")
                        .header("Authorization",
                                "Bearer " + token("contador-test", Role.CONTADOR)))
                .andExpect(status().isOk());
    }

    @Test
    void rutaDesconocidaQuedaDenegada() throws Exception {
        mockMvc.perform(get("/api/ruta-no-configurada")
                        .header("Authorization",
                                "Bearer " + token("admin-test", Role.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void informacionActuatorRequiereAdmin() throws Exception {
        mockMvc.perform(get("/actuator/info")
                        .header("Authorization",
                                "Bearer " + token("vendedor-test", Role.VENDEDOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ventasSinTokenDevuelven401() throws Exception {
        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorPuedeConsultarVentas() throws Exception {
        when(ventaService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/sales")
                        .header("Authorization",
                                "Bearer " + token("vendedor-test", Role.VENDEDOR)))
                .andExpect(status().isOk());
    }

    @Test
    void contadorPuedeConsultarVentas() throws Exception {
        when(ventaService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/sales")
                        .header("Authorization",
                                "Bearer " + token("contador-test", Role.CONTADOR)))
                .andExpect(status().isOk());
    }

    @Test
    void contadorNoPuedeRegistrarVentas() throws Exception {
        mockMvc.perform(post("/api/sales")
                        .header("Authorization",
                                "Bearer " + token("contador-test", Role.CONTADOR))
                        .contentType("application/json")
                        .content("""
                            {
                              "numeroFactura": "TEST-VENTA-001",
                              "fechaVenta": "2026-10-10",
                              "lineas": [{"productoId": 1, "cantidad": 1}]
                            }
                            """))
                .andExpect(status().isForbidden());
    }

}
