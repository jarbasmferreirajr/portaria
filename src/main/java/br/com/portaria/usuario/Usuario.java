package br.com.portaria.usuario;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Uma pessoa que administra eventos no sistema.
 *
 * O e-mail é o identificador de login, e não um dado de contato qualquer: é por
 * ele que a recuperação de senha encontra a conta. Por isso é normalizado para
 * minúsculas na própria entidade -- quem digita "Fulano@Exemplo.com" no login
 * precisa chegar ao mesmo registro de quem digitou "fulano@exemplo.com".
 *
 * A senha nunca entra aqui em texto puro: o campo guarda o hash, e quem o
 * calcula é o serviço, que conhece o codificador.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    /** Horário de Brasília: o servidor de implantação costuma rodar em UTC. */
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public static final String PAPEL_ORGANIZADOR = "ORGANIZADOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(nullable = false, length = 30)
    private String papel;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(FUSO);

    protected Usuario() {
        // Exigido pela JPA; não usar diretamente.
    }

    public Usuario(String nome, String email, String senhaHash, String papel) {
        this.nome = nome;
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.ativo = true;
        this.criadoEm = LocalDateTime.now(FUSO);
    }

    /** Minúsculas e sem espaços nas pontas: a forma canônica guardada no banco. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    // Mudanças de estado passam por métodos com nome de intenção, e não por
    // setters genéricos: quem lê o código vê o que a operação significa.

    public void alterarDadosPessoais(String nome, String email) {
        this.nome = nome;
        this.email = normalizarEmail(email);
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
    }

    public void desativar() {
        this.ativo = false;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public String getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
