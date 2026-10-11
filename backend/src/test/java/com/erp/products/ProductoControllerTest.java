package com.erp.products;

import com.erp.auth.JwtService;
import com.erp.users.AppUser;
import com.erp.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ProductoService productoService;

    private String token(Role role) {
        AppUser user = new AppUser(
                "prueba-" + role.name().toLowerCase(),
                "password-no-utilizada",
                role
        );
        return jwtService.generateToken(user);
    }

    private ProductoResponse producto(Long id, String sku, boolean activo) {
        return new ProductoResponse(
                id, sku, "Producto de prueba", "Descripción",
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00"),
                10, 2, activo
        );
    }

    private String jsonProducto(String sku, String nombre) {
        return """
                {
                  "sku": "%s",
                  "nombre": "%s",
                  "descripcion": "Prueba automatizada",
                  "precioCompra": 1000.00,
                  "precioVenta": 1500.00,
                  "stockActual": 10,
                  "stockMinimo": 2
                }
                """.formatted(sku, nombre);
    }

    @Test
    void consultarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorPuedeConsultarProductos() throws Exception {
        when(productoService.listar())
                .thenReturn(List.of(producto(1L, "TEST-001", true)));

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token(Role.VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("TEST-001"));
    }

    @Test
    void vendedorNoPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token(Role.VENDEDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("TEST-002", "Producto")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeCrearProductoYRecibe201() throws Exception {
        when(productoService.crear(any(ProductoRequest.class), any(String.class)))
                .thenReturn(producto(10L, "TEST-002", true));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("TEST-002", "Producto")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.sku").value("TEST-002"));
    }

    @Test
    void skuDuplicadoDevuelve409() throws Exception {
        when(productoService.crear(any(ProductoRequest.class), any(String.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT, "El SKU ya existe"));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("TEST-001", "Duplicado")))
                .andExpect(status().isConflict());
    }

    @Test
    void datosInvalidosDevuelven400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "",
                                  "nombre": "",
                                  "precioCompra": -1,
                                  "precioVenta": 1500,
                                  "stockActual": -1,
                                  "stockMinimo": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarProductoPorIdDevuelve200() throws Exception {
        when(productoService.obtener(1L))
                .thenReturn(producto(1L, "TEST-001", true));

        mockMvc.perform(get("/api/products/1")
                        .header("Authorization", "Bearer " + token(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void actualizarProductoDevuelve200() throws Exception {
        when(productoService.actualizar(eq(1L), any(ProductoRequest.class)))
                .thenReturn(producto(1L, "TEST-001", true));

        mockMvc.perform(put("/api/products/1")
                        .header("Authorization", "Bearer " + token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProducto("TEST-001", "Producto actualizado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void desactivarProductoDevuelveActivoFalse() throws Exception {
        when(productoService.desactivar(1L))
                .thenReturn(producto(1L, "TEST-001", false));

        mockMvc.perform(patch("/api/products/1/deactivate")
                        .header("Authorization", "Bearer " + token(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        when(productoService.obtener(999999L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado"));

        mockMvc.perform(get("/api/products/999999")
                        .header("Authorization", "Bearer " + token(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }
}
