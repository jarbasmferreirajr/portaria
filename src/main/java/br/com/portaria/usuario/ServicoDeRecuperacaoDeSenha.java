package br.com.portaria.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recuperação de acesso para quem esqueceu a senha.
 *
 * As quatro decisões de segurança deste fluxo, e o porquê de cada uma:
 *
 * 1. O banco guarda o HASH do token, nunca o token. Um vazamento do banco não
 *    entrega o acesso de ninguém.
 * 2. O token tem validade curta e é de USO ÚNICO. Um link antigo, esquecido na
 *    caixa de entrada, não vira uma porta permanente.
 * 3. Pedir a recuperação devolve SEMPRE a mesma resposta, exista ou não a conta.
 *    Responder "e-mail não encontrado" entregaria a lista de usuários a quem
 *    quisesse descobri-la, um endereço por vez.
 * 4. Pedir um token novo invalida os anteriores. Só o último link vale.
 */
@Service
public class ServicoDeRecuperacaoDeSenha {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeRecuperacaoDeSenha.class);

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Duration VALIDADE = Duration.ofMinutes(30);
    private static final int BYTES_DO_TOKEN = 32;

    private final RepositorioDeUsuarios usuarios;
    private final RepositorioDeTokensDeRecuperacao tokens;
    private final ServicoDeUsuarios servicoDeUsuarios;
    private final EnviadorDeEmail enviador;

    private final SecureRandom sorteador = new SecureRandom();

    public ServicoDeRecuperacaoDeSenha(RepositorioDeUsuarios usuarios,
                                       RepositorioDeTokensDeRecuperacao tokens,
                                       ServicoDeUsuarios servicoDeUsuarios,
                                       EnviadorDeEmail enviador) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.servicoDeUsuarios = servicoDeUsuarios;
        this.enviador = enviador;
    }

    /** Recusa por regra de negócio, não por falha técnica. */
    public static class TokenInvalido extends RuntimeException {
        public TokenInvalido(String mensagem) {
            super(mensagem);
        }
    }

    /**
     * Gera o link de recuperação e o entrega ao e-mail informado.
     *
     * Não devolve nada e não lança exceção quando o e-mail não existe: quem
     * chama não deve conseguir distinguir os dois casos, nem por retorno, nem
     * por tempo de resposta perceptível.
     */
    @Transactional
    public void solicitar(String email, String urlBase) {
        Optional<Usuario> encontrado = usuarios.findByEmail(Usuario.normalizarEmail(email));

        if (encontrado.isEmpty()) {
            log.info("Recuperação pedida para e-mail sem conta; nada foi enviado.");
            return;
        }

        Usuario usuario = encontrado.get();

        // Só o link mais recente vale.
        tokens.findByUsuarioIdAndUsadoEmIsNull(usuario.getId())
                .forEach(TokenDeRecuperacao::invalidar);

        String token = sortearToken();
        LocalDateTime expiraEm = LocalDateTime.now(FUSO).plus(VALIDADE);
        tokens.save(new TokenDeRecuperacao(usuario, hashDe(token), expiraEm));

        String link = urlBase + "/redefinir-senha?token=" + token;
        enviador.enviar(usuario.getEmail(), "Portaria - redefinicao de senha", corpoDoEmail(usuario, link));
    }

    @Transactional(readOnly = true)
    public TokenDeRecuperacao exigirTokenValido(String token) {
        return tokens.buscarPorHashComUsuario(hashDe(token))
                .filter(TokenDeRecuperacao::estaValido)
                .orElseThrow(() -> new TokenInvalido(
                        "Este link de recuperação é inválido ou já expirou. Peça um novo."));
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenDeRecuperacao registro = tokens.buscarPorHashComUsuario(hashDe(token))
                .filter(TokenDeRecuperacao::estaValido)
                .orElseThrow(() -> new TokenInvalido(
                        "Este link de recuperação é inválido ou já expirou. Peça um novo."));

        servicoDeUsuarios.definirSenhaSemConferirAtual(registro.getUsuario(), novaSenha);

        registro.marcarComoUsado();
        tokens.save(registro);
    }

    /** 32 bytes aleatórios em Base64 sem preenchimento: seguro e cabe numa URL. */
    private String sortearToken() {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        sorteador.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashDe(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossivel) {
            // SHA-256 é exigido de toda implementação da plataforma Java.
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", impossivel);
        }
    }

    private String corpoDoEmail(Usuario usuario, String link) {
        return """
               Olá, %s.

               Recebemos um pedido para redefinir a senha da sua conta no Portaria.
               Para escolher uma senha nova, acesse o endereço abaixo:

               %s

               O link vale por %d minutos e só pode ser usado uma vez.

               Se não foi você quem pediu, ignore esta mensagem: nada muda na
               sua conta enquanto o link não for aberto.
               """.formatted(usuario.getNome(), link, VALIDADE.toMinutes());
    }
}
