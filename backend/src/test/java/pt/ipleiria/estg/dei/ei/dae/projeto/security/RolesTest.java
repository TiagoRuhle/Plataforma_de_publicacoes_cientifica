package pt.ipleiria.estg.dei.ei.dae.projeto.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RolesTest {
    @Test
    void validaOsTresRolesIgnorandoMaiusculas() {
        assertTrue(Roles.isValid("COLABORADOR"));
        assertTrue(Roles.isValid("responsavel"));
        assertTrue(Roles.isValid(" Administrador "));
    }

    @Test
    void rejeitaRolesInvalidos() {
        assertFalse(Roles.isValid("xyz"));
        assertFalse(Roles.isValid(""));
        assertFalse(Roles.isValid(null));
    }
}
