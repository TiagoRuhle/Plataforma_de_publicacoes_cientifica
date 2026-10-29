package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.NovaPublicacaoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Comentario;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Cenário inicial (seed) que cobre todas as possibilidades do sistema:
 * 3 roles + utilizador extra + utilizador suspenso, tags visíveis e oculta, publicações PDF/ZIP,
 * publicação oculta, comentários (incluindo oculto), ratings variados e subscrições de tags.
 * É idempotente: se já existir o administrador, não faz nada (a BD persiste entre deploys).
 * Não chama a IA nem envia e-mails. Os resumos do seed são texto escrito à mão (estado EDITADO);
 * os PDFs têm texto suficiente para o botão "Gerar de novo" produzir um resumo real com o Ollama.
 */
@Startup
@Singleton
public class ConfigBean {

    private static final Logger logger = Logger.getLogger("ejbs.ConfigBean");

    private static final String RESUMO_DEEP_LEARNING = """
            ## Problema
            Os sistemas clássicos de reconhecimento de imagens dependem de características desenhadas à mão e generalizam mal quando as condições de captura mudam (luz, ângulo, ruído).

            ## Solução proposta
            Uma rede neuronal convolucional profunda, treinada de ponta a ponta com 100 mil imagens anotadas, com aumento de dados e validação cruzada em cinco partições.

            ## Resultados
            - Precisão 12% acima do estado da arte nos três conjuntos de teste
            - Menor tempo de inferência numa GPU de gama média

            ## Conclusões
            O método é promissor para o Projeto X, mas depende de grandes volumes de dados anotados.""";

    private static final String RESUMO_MEDICINA = """
            ## Problema
            O diagnóstico por imagem em radiologia consome muito tempo de especialistas e varia entre observadores, sobretudo em exames de rotina com grande volume.

            ## Solução proposta
            Redes convolucionais treinadas com exames anotados de três hospitais e avaliadas com validação externa, para servirem de segunda opinião ao radiologista.

            ## Resultados
            - Sensibilidade superior a 90%
            - Especificidade de 87%
            - Pior desempenho em lesões pequenas (menos de 5 mm)

            ## Conclusões
            O sistema serve de apoio à decisão e não substitui o médico. Os falsos negativos em lesões pequenas são a principal limitação a resolver.""";

    private static final String RESUMO_ML = """
            ## Problema
            A escolha do algoritmo de aprendizagem supervisionada faz-se muitas vezes por hábito e não por evidência, o que leva a modelos mais complexos e caros do que o necessário.

            ## Solução proposta
            Um estudo comparativo controlado de árvores de decisão, máquinas de vetores de suporte e redes neuronais em 20 conjuntos de dados públicos, com a mesma validação e a mesma otimização de hiperparâmetros.

            ## Resultados
            - Nenhum algoritmo domina em todos os conjuntos
            - O gradient boosting é o melhor em dados tabulares pequenos
            - As redes neuronais só compensam acima de cerca de 50 mil amostras

            ## Conclusões
            Convém começar por modelos simples como referência e só aumentar a complexidade se houver ganho mensurável.""";

    @EJB
    private UserBean userBean;

    @EJB
    private TagBean tagBean;

    @EJB
    private PublicacaoBean publicacaoBean;

    @EJB
    private ComentarioBean comentarioBean;

    @EJB
    private SubscricaoBean subscricaoBean;

    @PostConstruct
    public void populateDB() {
        if (userBean.findByUsername("administrador") != null) {
            logger.info("Base de dados já populada; seed ignorado.");
            return;
        }
        try {
            // ---------- utilizadores (a password é igual ao username, só para demonstração) ----------
            userBean.create("administrador", "administrador", "Administrador", "administrador@projeto.com", "ADMINISTRADOR");
            userBean.create("responsavel", "responsavel", "Responsável", "responsavel@projeto.com", "RESPONSAVEL");
            userBean.create("colaborador", "colaborador", "João A", "colaborador@projeto.com", "COLABORADOR");
            userBean.create("colaborador2", "colaborador2", "Joana B", "colaborador2@projeto.com", "COLABORADOR");
            userBean.create("suspenso", "suspenso1", "Conta Suspensa", "suspenso@projeto.com", "COLABORADOR");
            userBean.updateStatus("suspenso", false);

            User admin = userBean.findByUsername("administrador");
            User responsavel = userBean.findByUsername("responsavel");
            User colaborador = userBean.findByUsername("colaborador");
            User colaborador2 = userBean.findByUsername("colaborador2");

            // ---------- tags (uma oculta) ----------
            tagBean.create("Projeto X", true);
            tagBean.create("Projeto Y", true);
            tagBean.create("Inteligencia Artificial", true);
            tagBean.create("Machine Learning", true);
            tagBean.create("Redes Neuronais", true);
            tagBean.create("Ciencia de Dados", true);
            tagBean.create("tag-interna", false);

            // ---------- publicações ----------
            Publicacao pub1 = publicacaoBean.criar(nova("Deep Learning para Reconhecimento de Imagens",
                    "Artigo científico", "Inteligência Artificial", "Dr. Angelo Silva", 2024,
                    "deep_learning_2024.pdf", pdf("Deep Learning para Reconhecimento de Imagens",
                    "Resumo. O reconhecimento automatico de imagens e uma tarefa central em visao computacional. "
                            + "Os metodos classicos usam caracteristicas desenhadas manualmente, que generalizam mal quando mudam "
                            + "a iluminacao, o angulo de captura ou o nivel de ruido.",
                    "Propomos uma rede neuronal convolucional profunda treinada de ponta a ponta. O treino usa 100 mil imagens "
                            + "anotadas, aumento de dados (rotacoes, cortes e variacoes de brilho) e validacao cruzada em cinco particoes.",
                    "Resultados. Em tres conjuntos de teste independentes a precisao melhora 12 por cento face ao estado da arte. "
                            + "O tempo de inferencia diminui numa placa grafica de gama media, o que permite uso em tempo quase real.",
                    "Conclusoes. O metodo e promissor para o Projeto X. A principal limitacao e a dependencia de grandes volumes "
                            + "de dados anotados, cuja anotacao e cara e demorada. Como trabalho futuro, pretende-se reduzir essa "
                            + "dependencia com aprendizagem semi-supervisionada."),
                    "Inteligencia Artificial", "Machine Learning", "Projeto X"),
                    admin, true, RESUMO_DEEP_LEARNING, false);

            Publicacao pub2 = publicacaoBean.criar(nova("Redes Neuronais Convolucionais na Medicina",
                    "Tese de mestrado", "Ciência de Dados", "Maria Santos", 2023,
                    "redes_neuronais_medicina.pdf", pdf("Redes Neuronais Convolucionais na Medicina",
                    "Resumo. O diagnostico por imagem em radiologia consome muito tempo de especialistas e varia entre "
                            + "observadores, sobretudo nos exames de rotina com grande volume.",
                    "Esta tese avalia redes neuronais convolucionais como segunda opiniao para o radiologista. Os modelos foram "
                            + "treinados com exames anotados de tres hospitais e avaliados com validacao externa, em dados nunca "
                            + "vistos durante o treino.",
                    "Resultados. A sensibilidade foi superior a 90 por cento e a especificidade de 87 por cento. O desempenho "
                            + "e pior em lesoes pequenas, com menos de 5 milimetros, onde se concentram os falsos negativos.",
                    "Conclusoes. O sistema serve de apoio a decisao e nao substitui o medico. Sao necessarios mais exemplos "
                            + "de lesoes pequenas para melhorar a deteccao."),
                    "Redes Neuronais", "Ciencia de Dados", "Projeto Y"),
                    colaborador, false, RESUMO_MEDICINA, false);

            Publicacao pub3 = publicacaoBean.criar(nova("Algoritmos de Machine Learning Supervisionado",
                    "Artigo científico", "Machine Learning", "Prof. Carlos Pereira", 2025,
                    "ml_supervisionado.pdf", pdf("Algoritmos de Machine Learning Supervisionado",
                    "Resumo. A escolha do algoritmo de aprendizagem supervisionada faz-se muitas vezes por habito e nao por "
                            + "evidencia, o que leva a modelos mais complexos e mais caros do que o necessario.",
                    "Apresentamos um estudo comparativo controlado de arvores de decisao, maquinas de vetores de suporte e "
                            + "redes neuronais em 20 conjuntos de dados publicos. Todos os algoritmos usam a mesma validacao e a "
                            + "mesma otimizacao de hiperparametros.",
                    "Resultados. Nenhum algoritmo domina em todos os conjuntos. O gradient boosting e o melhor em dados "
                            + "tabulares pequenos. As redes neuronais so compensam acima de cerca de 50 mil amostras.",
                    "Conclusoes. Recomenda-se comecar por modelos simples como referencia e so aumentar a complexidade quando "
                            + "o ganho for mensuravel."),
                    "Machine Learning", "Projeto X", "tag-interna"),
                    responsavel, true, RESUMO_ML, false);

            Publicacao pub4 = publicacaoBean.criar(nova("Relatório Técnico Confidencial",
                    "Relatório técnico", "Ciência de Dados", "Equipa Interna", 2025,
                    "relatorio_confidencial.pdf", pdf("Relatorio Tecnico Confidencial",
                    "Documento interno. Analisa o desempenho dos modelos em producao e contem dados nao publicos do Centro, "
                            + "por isso nao esta disponivel para pessoas externas."),
                    "Projeto Y"),
                    colaborador2, false,
                    "## Problema\nAvaliar o desempenho dos modelos em produção.\n\n## Nota\nRelatório interno com dados não públicos do Centro.",
                    false);
            publicacaoBean.ocultar(pub4.getId(), responsavel);

            Publicacao pub5 = publicacaoBean.criar(nova("Dataset de Sensores Industriais",
                    "Dataset", "Ciência de Dados", "Laboratório de Materiais", 2025,
                    "sensores_dataset.zip", zip(), "Ciencia de Dados"),
                    colaborador2, false, null, false);

            // ---------- comentários (um oculto) ----------
            comentarioBean.create(pub1.getId(), colaborador, "Excelente trabalho! Muito bem fundamentado.", false);
            comentarioBean.create(pub1.getId(), responsavel, "Gostaria de ver mais detalhes sobre a metodologia.", false);
            comentarioBean.create(pub1.getId(), colaborador2,
                    "ATENÇÃO: encontrei um erro no algoritmo da secção 3; o artigo pode vir a ser retirado.", false);
            Comentario spam = comentarioBean.create(pub1.getId(), colaborador, "Comentário a rever (conteúdo impróprio).", false);
            comentarioBean.ocultar(spam.getId(), responsavel);
            comentarioBean.create(pub2.getId(), admin, "Aplicação muito interessante na área médica!", false);
            comentarioBean.create(pub3.getId(), colaborador2, "Boa comparação, falta o custo computacional.", false);

            // ---------- ratings ----------
            publicacaoBean.addOrUpdateRating(pub1.getId(), colaborador, 5);
            publicacaoBean.addOrUpdateRating(pub1.getId(), responsavel, 4);
            publicacaoBean.addOrUpdateRating(pub1.getId(), colaborador2, 2);
            publicacaoBean.addOrUpdateRating(pub2.getId(), admin, 5);
            publicacaoBean.addOrUpdateRating(pub2.getId(), responsavel, 5);
            publicacaoBean.addOrUpdateRating(pub3.getId(), admin, 4);
            publicacaoBean.addOrUpdateRating(pub3.getId(), colaborador, 4);
            publicacaoBean.addOrUpdateRating(pub3.getId(), responsavel, 3);
            publicacaoBean.addOrUpdateRating(pub5.getId(), colaborador, 3);

            // ---------- subscrições de tags ----------
            subscricaoBean.subscrever("colaborador", "Projeto X", false);
            subscricaoBean.subscrever("colaborador", "Machine Learning", false);
            subscricaoBean.subscrever("colaborador2", "Projeto Y", false);
            subscricaoBean.subscrever("responsavel", "tag-interna", true);

            logger.info("Base de dados populada com sucesso!");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Erro ao popular a BD", ex);
            // falha rápida: evita uma BD parcialmente populada e escondida
            throw new IllegalStateException("Falha ao popular a base de dados: " + ex.getMessage(), ex);
        }
    }

    private NovaPublicacaoDTO nova(String titulo, String tipo, String area, String autor, int ano,
                                   String filename, byte[] conteudo, String... tags) {
        NovaPublicacaoDTO d = new NovaPublicacaoDTO();
        d.setTitulo(titulo);
        d.setTipo(tipo);
        d.setAreaCientifica(area);
        d.setAutor(autor);
        d.setAno(ano);
        d.setFilename(filename);
        d.setConteudo(conteudo);
        d.setTags(List.of(tags));
        return d;
    }

    /**
     * PDF válido, com texto extraível e várias linhas/páginas (o texto é quebrado à mão porque o PDFBox não faz
     * quebra de linha). O texto do seed usa apenas caracteres ASCII, para a fonte padrão nunca falhar.
     */
    private byte[] pdf(String titulo, String... paragrafos) throws IOException {
        List<String> linhas = new ArrayList<>();
        linhas.add(sanitizar(titulo));
        linhas.add("");
        for (String paragrafo : paragrafos) {
            linhas.addAll(quebrar(sanitizar(paragrafo), 88));
            linhas.add("");
        }

        final int linhasPorPagina = 46;
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int inicio = 0; inicio < linhas.size(); inicio += linhasPorPagina) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.newLineAtOffset(50, 780);
                    for (int i = inicio; i < Math.min(inicio + linhasPorPagina, linhas.size()); i++) {
                        boolean tituloDoc = i == 0;
                        cs.setFont(tituloDoc ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, tituloDoc ? 16 : 11);
                        cs.showText(linhas.get(i));
                        cs.newLineAtOffset(0, tituloDoc ? -26 : -15);
                    }
                    cs.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static List<String> quebrar(String texto, int maximo) {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            if (atual.length() > 0 && atual.length() + 1 + palavra.length() > maximo) {
                linhas.add(atual.toString());
                atual.setLength(0);
            }
            if (atual.length() > 0) {
                atual.append(' ');
            }
            atual.append(palavra);
        }
        if (atual.length() > 0) {
            linhas.add(atual.toString());
        }
        return linhas;
    }

    /** Remove tudo o que a fonte padrão do PDF não consegue desenhar (evita falhar o arranque por um carácter). */
    private static String sanitizar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            sb.append(c >= 32 && c <= 126 ? c : ' ');
        }
        return sb.toString().replaceAll(" +", " ").trim();
    }

    private byte[] zip() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("LEIAME.txt"));
            zip.write("Dataset de exemplo com leituras de sensores industriais.\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("leituras.csv"));
            zip.write("timestamp,sensor,valor\n2025-01-01T00:00:00,S1,21.5\n2025-01-01T00:01:00,S1,21.7\n"
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            return out.toByteArray();
        }
    }
}
