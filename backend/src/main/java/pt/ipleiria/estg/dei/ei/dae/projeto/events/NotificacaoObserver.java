package pt.ipleiria.estg.dei.ei.dae.projeto.events;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.NotificacaoAsyncBean;

@ApplicationScoped
public class NotificacaoObserver {

    @EJB
    private NotificacaoAsyncBean notificacaoAsyncBean;

    public void aposCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) NotificacaoEvent evento) {
        notificacaoAsyncBean.notificar(evento.publicacaoId(), evento.descricao(), evento.actor());
    }
}
