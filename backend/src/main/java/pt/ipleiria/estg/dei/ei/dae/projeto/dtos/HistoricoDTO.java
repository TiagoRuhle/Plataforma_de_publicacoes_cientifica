package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Historico;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.TipoAcao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class HistoricoDTO {
    private Long id;
    private TipoAcao tipoAcao;
    private LocalDateTime dataHora;
    private String username;
    private String nomeUser;
    private Long publicacaoId;
    private String tituloPublicacao;
    private String descricao;
    private String detalhes;

    public HistoricoDTO() {}

    public HistoricoDTO(Long id, TipoAcao tipoAcao, LocalDateTime dataHora,
                        String username, String nomeUser,
                        Long publicacaoId, String tituloPublicacao,
                        String descricao, String detalhes) {
        this.id = id;
        this.tipoAcao = tipoAcao;
        this.dataHora = dataHora;
        this.username = username;
        this.nomeUser = nomeUser;
        this.publicacaoId = publicacaoId;
        this.tituloPublicacao = tituloPublicacao;
        this.descricao = descricao;
        this.detalhes = detalhes;
    }

    // Conversão de Entidade para DTO
    public static HistoricoDTO from(Historico historico) {
        return new HistoricoDTO(
                historico.getId(),
                historico.getTipoAcao(),
                historico.getDataHora(),
                historico.getUser().getUsername(),
                historico.getUser().getName(),
                historico.getPublicacao() != null ? historico.getPublicacao().getId() : null,
                historico.getPublicacao() != null ? historico.getPublicacao().getTitulo() : null,
                historico.getDescricao(),
                historico.getDetalhes()
        );
    }

    // Conversão de lista
    public static List<HistoricoDTO> from(List<Historico> historicos) {
        return historicos.stream()
                .map(HistoricoDTO::from)
                .collect(Collectors.toList());
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoAcao getTipoAcao() {
        return tipoAcao;
    }

    public void setTipoAcao(TipoAcao tipoAcao) {
        this.tipoAcao = tipoAcao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNomeUser() {
        return nomeUser;
    }

    public void setNomeUser(String nomeUser) {
        this.nomeUser = nomeUser;
    }

    public Long getPublicacaoId() {
        return publicacaoId;
    }

    public void setPublicacaoId(Long publicacaoId) {
        this.publicacaoId = publicacaoId;
    }

    public String getTituloPublicacao() {
        return tituloPublicacao;
    }

    public void setTituloPublicacao(String tituloPublicacao) {
        this.tituloPublicacao = tituloPublicacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }
}