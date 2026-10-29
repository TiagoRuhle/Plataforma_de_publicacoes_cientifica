package pt.ipleiria.estg.dei.ei.dae.projeto.events;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.ResumoAsyncBean;

/**
 * Só depois de a transação que criou/alterou a publicação fazer commit é que se lança a
 * geração do resumo. Resolve a corrida em que a thread assíncrona corria antes do commit
 * e não encontrava a publicação.
 */
@ApplicationScoped
public class ResumoPedidoObserver {

    @EJB
    private ResumoAsyncBean resumoAsyncBean;

    public void aposCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) ResumoPedidoEvent evento) {
        resumoAsyncBean.gerarResumo(evento.publicacaoId());
    }
}
