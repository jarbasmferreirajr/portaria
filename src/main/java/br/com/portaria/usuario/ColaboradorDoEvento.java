package br.com.portaria.usuario;

import br.com.portaria.evento.Evento;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Vínculo entre uma pessoa e um evento em que ela trabalha.
 *
 * Note que o vínculo é com o EVENTO, e não um papel global do usuário: quem
 * ajudou num encontro não fica com acesso aos próximos. Isso também permite que
 * a mesma pessoa seja organizadora de um evento e colaboradora de outro.
 */
@Entity
@Table(name = "colaborador_evento")
public class ColaboradorDoEvento {

    /** Horário de Brasília: o servidor de implantação costuma rodar em UTC. */
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 60)
    private String funcao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(FUSO);

    protected ColaboradorDoEvento() {
        // Exigido pela JPA; não usar diretamente.
    }

    public ColaboradorDoEvento(Evento evento, Usuario usuario, String funcao) {
        this.evento = evento;
        this.usuario = usuario;
        this.funcao = funcao;
        this.criadoEm = LocalDateTime.now(FUSO);
    }

    public void alterarFuncao(String funcao) {
        this.funcao = funcao;
    }

    public Long getId() { return id; }
    public Evento getEvento() { return evento; }
    public Usuario getUsuario() { return usuario; }
    public String getFuncao() { return funcao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
