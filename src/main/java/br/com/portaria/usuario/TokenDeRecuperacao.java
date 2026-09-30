package br.com.portaria.usuario;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Autorização temporária para redefinir a senha sem conhecer a atual.
 *
 * A entidade guarda apenas o hash do token. O valor original vai no link do
 * e-mail e não é persistido em lugar nenhum.
 */
@Entity
@Table(name = "token_recuperacao_senha")
public class TokenDeRecuperacao {

    /** Horário de Brasília: o servidor de implantação costuma rodar em UTC. */
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(FUSO);

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    protected TokenDeRecuperacao() {
        // Exigido pela JPA; não usar diretamente.
    }

    public TokenDeRecuperacao(Usuario usuario, String tokenHash, LocalDateTime expiraEm) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.criadoEm = LocalDateTime.now(FUSO);
        this.expiraEm = expiraEm;
    }

    /** Um token só vale se ainda não foi usado e ainda não expirou. */
    public boolean estaValido() {
        return usadoEm == null && LocalDateTime.now(FUSO).isBefore(expiraEm);
    }

    public void marcarComoUsado() {
        this.usadoEm = LocalDateTime.now(FUSO);
    }

    /** Invalida sem consumir: usado quando um token novo é pedido. */
    public void invalidar() {
        if (this.usadoEm == null) {
            this.usadoEm = LocalDateTime.now(FUSO);
        }
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public LocalDateTime getUsadoEm() { return usadoEm; }
}
