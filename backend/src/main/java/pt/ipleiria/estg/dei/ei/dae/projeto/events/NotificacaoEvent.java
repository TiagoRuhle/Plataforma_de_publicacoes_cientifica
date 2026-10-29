package pt.ipleiria.estg.dei.ei.dae.projeto.events;

/**
 * Algo mudou numa publicação: os subscritores das suas tags devem ser avisados por e-mail.
 *
 * @param actor username de quem fez a alteração (não é notificado a si próprio)
 */
public record NotificacaoEvent(long publicacaoId, String descricao, String actor) {
}
