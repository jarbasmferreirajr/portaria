package br.com.portaria.usuario;

import br.com.portaria.email.EnviadorDeEmail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * As garantias do fluxo de recuperação de senha.
 *
 * São regras de segurança, não de conveniência: cada teste aqui corresponde a
 * uma forma conhecida de abusar desse fluxo.
 */
class ServicoDeRecuperacaoDeSenhaTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private RepositorioDeUsuarios usuarios;
    private RepositorioDeTokensDeRecuperacao tokens;
    private ServicoDeUsuarios servicoDeUsuarios;
    private EnviadorDeEmail enviador;
    private ServicoDeRecuperacaoDeSenha servico;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarios = mock(RepositorioDeUsuarios.class);
        tokens = mock(RepositorioDeTokensDeRecuperacao.class);
        servicoDeUsuarios = mock(ServicoDeUsuarios.class);
        enviador = mock(EnviadorDeEmail.class);
        servico = new ServicoDeRecuperacaoDeSenha(usuarios, tokens, servicoDeUsuarios, enviador);

        usuario = new Usuario("Jarbas", "jarbas@exemplo.com", "hash-antigo", Usuario.PAPEL_ORGANIZADOR);
        when(tokens.save(any(TokenDeRecuperacao.class))).thenAnswer(c -> c.getArgument(0));
    }

    /** Mesmo cálculo do serviço, escrito aqui de propósito para conferir o que foi gravado. */
    private String sha256(String valor) throws Exception {
        MessageDigest digestor = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digestor.digest(valor.getBytes(StandardCharsets.UTF_8)));
    }

    private String tokenDoLink(String corpo) {
        int inicio = corpo.indexOf("token=") + "token=".length();
        int fim = corpo.indexOf('\n', inicio);
        return corpo.substring(inicio, fim < 0 ? corpo.length() : fim).trim();
    }

    @Test
    @DisplayName("E-mail sem conta não gera token nem mensagem, e não acusa o erro")
    void emailInexistenteNaoRevelaNada() {
        when(usuarios.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        servico.solicitar("ninguem@exemplo.com", "http://localhost:8080");

        verify(tokens, never()).save(any(TokenDeRecuperacao.class));
        verify(enviador, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("O banco guarda o hash do token, nunca o token que viaja no link")
    void guardaApenasOHashDoToken() throws Exception {
        when(usuarios.findByEmail("jarbas@exemplo.com")).thenReturn(Optional.of(usuario));
        when(tokens.findByUsuarioIdAndUsadoEmIsNull(any())).thenReturn(List.of());

        servico.solicitar("jarbas@exemplo.com", "http://localhost:8080");

        ArgumentCaptor<TokenDeRecuperacao> salvo = ArgumentCaptor.forClass(TokenDeRecuperacao.class);
        verify(tokens).save(salvo.capture());

        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(eq("jarbas@exemplo.com"), anyString(), corpo.capture());

        String tokenEnviado = tokenDoLink(corpo.getValue());

        assertThat(tokenEnviado).isNotBlank();
        assertThat(salvo.getValue().getTokenHash()).isNotEqualTo(tokenEnviado);
        assertThat(salvo.getValue().getTokenHash()).isEqualTo(sha256(tokenEnviado));
    }

    @Test
    @DisplayName("Pedir um link novo invalida os anteriores")
    void pedidoNovoInvalidaOsAnteriores() {
        TokenDeRecuperacao anterior =
                new TokenDeRecuperacao(usuario, "hash-antigo", LocalDateTime.now(FUSO).plusMinutes(20));
        when(usuarios.findByEmail("jarbas@exemplo.com")).thenReturn(Optional.of(usuario));
        when(tokens.findByUsuarioIdAndUsadoEmIsNull(any())).thenReturn(List.of(anterior));

        servico.solicitar("jarbas@exemplo.com", "http://localhost:8080");

        assertThat(anterior.estaValido()).isFalse();
    }

    @Test
    @DisplayName("Token válido redefine a senha e é consumido")
    void tokenValidoRedefineEEhConsumido() throws Exception {
        String token = "token-de-teste";
        TokenDeRecuperacao registro =
                new TokenDeRecuperacao(usuario, sha256(token), LocalDateTime.now(FUSO).plusMinutes(20));
        when(tokens.buscarPorHashComUsuario(sha256(token))).thenReturn(Optional.of(registro));

        servico.redefinir(token, "senhaNova123");

        verify(servicoDeUsuarios).definirSenhaSemConferirAtual(usuario, "senhaNova123");
        assertThat(registro.getUsadoEm()).isNotNull();
        assertThat(registro.estaValido()).isFalse();
    }

    @Test
    @DisplayName("O mesmo token não serve duas vezes")
    void tokenNaoServeDuasVezes() throws Exception {
        String token = "token-de-teste";
        TokenDeRecuperacao registro =
                new TokenDeRecuperacao(usuario, sha256(token), LocalDateTime.now(FUSO).plusMinutes(20));
        registro.marcarComoUsado();
        when(tokens.buscarPorHashComUsuario(sha256(token))).thenReturn(Optional.of(registro));

        assertThatThrownBy(() -> servico.redefinir(token, "outraSenha123"))
                .isInstanceOf(ServicoDeRecuperacaoDeSenha.TokenInvalido.class);

        verify(servicoDeUsuarios, never()).definirSenhaSemConferirAtual(any(), anyString());
    }

    @Test
    @DisplayName("Token expirado é recusado")
    void tokenExpiradoEhRecusado() throws Exception {
        String token = "token-de-teste";
        TokenDeRecuperacao registro =
                new TokenDeRecuperacao(usuario, sha256(token), LocalDateTime.now(FUSO).minusMinutes(1));
        when(tokens.buscarPorHashComUsuario(sha256(token))).thenReturn(Optional.of(registro));

        assertThatThrownBy(() -> servico.redefinir(token, "outraSenha123"))
                .isInstanceOf(ServicoDeRecuperacaoDeSenha.TokenInvalido.class);
    }
}
