package br.com.portaria.inscricao;

import br.com.portaria.evento.Evento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A inscrição de uma pessoa em um evento.
 *
 * O código é o que ela apresenta na portaria. Motoclube e cidade são opcionais,
 * mas alimentam a premiação típica de encontro de motociclistas -- grupo com
 * mais presentes, quem veio de mais longe.
 */
@Entity
@Table(name = "inscricao")
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String telefone;

    @Column(length = 120)
    private String motoclube;

    @Column(length = 120)
    private String cidade;

    @Column(nullable = false, length = 6, unique = true)
    private String codigo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    protected Inscricao() {
        // Exigido pela JPA; não usar diretamente.
    }

    public Inscricao(Evento evento, String nome, String email, String telefone,
                     String motoclube, String cidade, String codigo) {
        this.evento = evento;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.motoclube = motoclube;
        this.cidade = cidade;
        this.codigo = codigo;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Evento getEvento() { return evento; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getMotoclube() { return motoclube; }
    public String getCidade() { return cidade; }
    public String getCodigo() { return codigo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
