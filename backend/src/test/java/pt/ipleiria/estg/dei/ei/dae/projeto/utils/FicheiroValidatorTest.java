package pt.ipleiria.estg.dei.ei.dae.projeto.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FicheiroValidatorTest {

    private static final byte[] PDF = "%PDF-1.4 conteudo".getBytes();
    private static final byte[] ZIP = {'P', 'K', 3, 4, 0, 0};

    @Test
    void aceitaPdfEZipValidos() {
        assertEquals(FicheiroValidator.Tipo.PDF, FicheiroValidator.validar("a.pdf", PDF));
        assertEquals(FicheiroValidator.Tipo.ZIP, FicheiroValidator.validar("A.ZIP", ZIP));
    }

    @Test
    void rejeitaExtensaoNaoPermitida() {
        assertThrows(IllegalArgumentException.class, () -> FicheiroValidator.validar("virus.exe", PDF));
    }

    @Test
    void rejeitaConteudoQueNaoCorrespondeAExtensao() {
        assertThrows(IllegalArgumentException.class, () -> FicheiroValidator.validar("a.pdf", ZIP));
        assertThrows(IllegalArgumentException.class, () -> FicheiroValidator.validar("a.zip", PDF));
        assertThrows(IllegalArgumentException.class, () -> FicheiroValidator.validar("a.pdf", "abc".getBytes()));
    }

    @Test
    void rejeitaFicheiroVazio() {
        assertThrows(IllegalArgumentException.class, () -> FicheiroValidator.validar("a.pdf", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> FicheiroValidator.validar("a.pdf", null));
    }

    @Test
    void sanitizaNomesPerigosos() {
        assertEquals("passwd", FicheiroValidator.sanitizarNome("../../etc/passwd"));
        assertEquals("y.pdf", FicheiroValidator.sanitizarNome("C:\\x\\y.pdf"));
        assertEquals("ficheiro", FicheiroValidator.sanitizarNome(null));
        assertFalse(FicheiroValidator.sanitizarNome("a\"b.pdf").contains("\""));
    }
}
