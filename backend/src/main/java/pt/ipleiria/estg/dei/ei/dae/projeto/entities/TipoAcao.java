package pt.ipleiria.estg.dei.ei.dae.projeto.entities;

public enum TipoAcao {
    // Publicações
    PUBLICACAO_CRIADA,
    PUBLICACAO_ATUALIZADA,
    PUBLICACAO_OCULTADA,
    PUBLICACAO_VISIVEL,
    PUBLICACAO_VISUALIZADA,

    // Resumos
    RESUMO_GERADO,
    RESUMO_EDITADO_MANUALMENTE,

    // Tags
    TAG_ADICIONADA,
    TAG_REMOVIDA,
    TAG_CRIADA,
    TAG_ATUALIZADA,

    // Comentários
    COMENTARIO_ADICIONADO,
    COMENTARIO_REMOVIDO,
    COMENTARIO_EDITADO,

    // Ratings
    RATING_ADICIONADO,
    RATING_ATUALIZADO,
    RATING_REMOVIDO,

    // Autenticação
    LOGIN,
    LOGOUT,

    // User
    PERFIL_ATUALIZADO,
    SENHA_ALTERADA,
    USER_CRIADO,

    // Subscrições
    TAG_SUBSCRITA,
    TAG_DESSUBSCRITA
}