package pt.ipleiria.estg.dei.ei.dae.projeto.entities;

/** Ciclo de vida do resumo de uma publicação. */
public enum ResumoEstado {
    PENDENTE,        // à espera da IA
    CONCLUIDO,       // gerado pela IA
    ERRO,            // a IA falhou (pode regenerar)
    EDITADO,         // corrigido manualmente por um utilizador
    NAO_APLICAVEL    // ex.: ficheiros ZIP
}
