package com.erp.categories;

public record CategoriaResponse(
    Long id,
    String nombre,
    String descripcion,
    Boolean activo
) {
    public static CategoriaResponse from(Categoria c) {
        return new CategoriaResponse(
            c.getId(), c.getNombre(), c.getDescripcion(), c.getActivo()
        );
    }
}
