package pt.ipleiria.estg.dei.ei.dae.projeto.security;

import java.util.Set;

public final class Roles {
    public static final String COLABORADOR = "COLABORADOR";
    public static final String RESPONSAVEL = "RESPONSAVEL";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    private static final Set<String> VALIDOS = Set.of(COLABORADOR, RESPONSAVEL, ADMINISTRADOR);

    private Roles() {
    }

    public static boolean isValid(String role) {
        return role != null && VALIDOS.contains(role.trim().toUpperCase());
    }

    public static String normalizar(String role) {
        return role == null ? null : role.trim().toUpperCase();
    }
}
