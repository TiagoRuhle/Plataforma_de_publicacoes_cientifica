package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.ResumoEstado;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.TipoAcao;

/**
 * Transações curtas para ler/gravar o estado do resumo. Separado do ResumoAsyncBean para que a
 * chamada (lenta) ao LLM nunca fique dentro de uma transação de base de dados.
 */
@Stateless
public class ResumoStoreBean {

    public record DadosResumo(String filepath, String mimeType) {
    }

    @PersistenceContext
    private EntityManager entityManager;

    @EJB
    private HistoricoBean historicoBean;

    public DadosResumo carregar(long publicacaoId) {
        Publicacao p = entityManager.find(Publicacao.class, publicacaoId);
        return p == null ? null : new DadosResumo(p.getFilepath(), p.getMimeType());
    }

    public void guardarSucesso(long publicacaoId, String resumo) {
        Publicacao p = entityManager.find(Publicacao.class, publicacaoId);
        if (p == null || p.getResumoEstado() == ResumoEstado.EDITADO) {
            return; // não sobrepor uma correção manual
        }
        p.setResumo(resumo);
        p.setResumoEstado(ResumoEstado.CONCLUIDO);
        if (p.getSubmetidoPor() != null) {
            historicoBean.registar(TipoAcao.RESUMO_GERADO, p.getSubmetidoPor(), p, "Resumo gerado pela IA");
        }
    }

    public void guardarErro(long publicacaoId, String motivo) {
        Publicacao p = entityManager.find(Publicacao.class, publicacaoId);
        if (p == null || p.getResumoEstado() == ResumoEstado.EDITADO) {
            return;
        }
        String m = motivo == null ? "erro desconhecido" : motivo;
        if (m.length() > 400) {
            m = m.substring(0, 400) + "...";
        }
        p.setResumoEstado(ResumoEstado.ERRO);
        p.setResumo("Não foi possível gerar o resumo automático (" + m + "). Pode tentar novamente.");
    }
}
