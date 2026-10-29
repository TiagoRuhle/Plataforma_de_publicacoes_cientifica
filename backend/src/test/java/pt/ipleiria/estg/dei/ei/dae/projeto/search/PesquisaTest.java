package pt.ipleiria.estg.dei.ei.dae.projeto.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PesquisaTest {

    @Test
    void escapaCaracteresEspeciaisDoLike() {
        assertEquals("%deep!_learning 100!%%", Pesquisa.padraoLike("  Deep_Learning 100% "));
        assertEquals("%a!!b%", Pesquisa.padraoLike("a!b"));
    }

    @Test
    void ordenacaoUsaApenasChavesDaWhitelist() {
        String injecao = Pesquisa.orderBy("p.id; DROP TABLE users;--", "asc");
        assertFalse(injecao.contains("DROP"));
        assertTrue(injecao.contains("p.dataSubmissao"));
    }

    @Test
    void ordenaPorRatingEComentarios() {
        assertTrue(Pesquisa.orderBy("rating", "asc").contains("AVG(r2.valor)"));
        assertTrue(Pesquisa.orderBy("rating", "asc").contains("ASC"));
        assertTrue(Pesquisa.orderBy("comentarios", "qualquer").endsWith("DESC"));
    }

    @Test
    void limitaTamanhoEPagina() {
        assertEquals(100, Pesquisa.limitarTamanho(500, 100));
        assertEquals(1, Pesquisa.limitarTamanho(0, 100));
        assertEquals(0, Pesquisa.paginaValida(-3));
    }
}
