package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Subscricao;

import java.util.List;
import java.util.stream.Collectors;

public class SubscricaoDTO {

    private String username;
    private String nome;

    public SubscricaoDTO() {}

    public SubscricaoDTO(String username, String nome) {
        this.username = username;
        this.nome = nome;
    }

    public static SubscricaoDTO from(Subscricao subscricao) {
        return new SubscricaoDTO(
                subscricao.getUser().getUsername(),
                subscricao.getTag().getTag()
        );
    }

    public static List<SubscricaoDTO> from(List<Subscricao> subscricoes) {
        return subscricoes.stream()
                .map(SubscricaoDTO::from)
                .collect(Collectors.toList());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
