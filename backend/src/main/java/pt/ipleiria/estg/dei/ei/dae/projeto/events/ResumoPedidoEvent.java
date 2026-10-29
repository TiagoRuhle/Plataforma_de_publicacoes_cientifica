package pt.ipleiria.estg.dei.ei.dae.projeto.events;

/** Disparado quando uma publicação precisa de (novo) resumo da IA. Só é tratado após o commit. */
public record ResumoPedidoEvent(long publicacaoId) {
}
