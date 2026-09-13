package br.com.portaria.evento;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Um evento presencial: encontro, festival, palestra.
 *
 * A capacidade é o limite físico do espaço, e não a meta de inscrições --
 * é ela que o painel de portaria usa para mostrar a lotação disponível.
 */
@Entity
@Table(name = "evento")
public class Evento {

    /** Horário de Brasília: o servidor de implantação costuma rodar em UTC. */
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(name = "local_evento", nullable = false, length = 200)
    private String local;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime fim;

    @Column(nullable = false)
    private Integer capacidade;

    /** Trecho legível que identifica o evento na URL pública: /e/{identificadorPublico} */
    @Column(name = "identificador_publico", nullable = false, length = 80, unique = true)
    private String identificadorPublico;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(FUSO);

    protected Evento() {
        // Exigido pela JPA; não usar diretamente.
    }

    public Evento(String nome, String descricao, String local, LocalDateTime inicio,
                  LocalDateTime fim, Integer capacidade, String identificadorPublico) {
        this.nome = nome;
        this.descricao = descricao;
        this.local = local;
        this.inicio = inicio;
        this.fim = fim;
        this.capacidade = capacidade;
        this.identificadorPublico = identificadorPublico;
        this.criadoEm = LocalDateTime.now(FUSO);
    }

    // Os prefixos get/set são exigidos pela convenção JavaBean, que a JPA e o
    // Thymeleaf usam para ler as propriedades. Não são traduzíveis.
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getLocal() { return local; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }
    public Integer getCapacidade() { return capacidade; }
    public String getIdentificadorPublico() { return identificadorPublico; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
