package br.com.portaria.usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regras das contas de usuário.
 *
 * Usa o BCrypt de verdade em vez de um codificador simulado: o valor destes
 * testes está justamente em provar que a senha nunca é guardada em texto puro
 * e que a conferência da senha atual realmente funciona.
 */
class ServicoDeUsuariosTest {

    private RepositorioDeUsuarios repositorio;
    private PasswordEncoder codificador;
    private ServicoDeUsuarios servico;

    @BeforeEach
    void preparar() {
        repositorio = mock(RepositorioDeUsuarios.class);
        codificador = new BCryptPasswordEncoder();
        servico = new ServicoDeUsuarios(repositorio, codificador);
        when(repositorio.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private Usuario usuarioSalvo(String email, String senha) {
        return new Usuario("Fulano", email, codificador.encode(senha), Usuario.PAPEL_ORGANIZADOR);
    }

    @Test
    @DisplayName("Ao criar, o e-mail é normalizado e a senha nunca é guardada em texto puro")
    void criaNormalizandoEmailEHashandoSenha() {
        when(repositorio.existsByEmail("jarbas@exemplo.com")).thenReturn(false);

        Usuario criado = servico.criar("Jarbas", "  Jarbas@Exemplo.COM ", "segredo123",
                Usuario.PAPEL_ORGANIZADOR);

        assertThat(criado.getEmail()).isEqualTo("jarbas@exemplo.com");
        assertThat(criado.getSenhaHash()).isNotEqualTo("segredo123");
        assertThat(codificador.matches("segredo123", criado.getSenhaHash())).isTrue();
        assertThat(criado.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Não é possível criar duas contas com o mesmo e-mail")
    void recusaEmailJaCadastrado() {
        when(repositorio.existsByEmail("jarbas@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> servico.criar("Jarbas", "JARBAS@exemplo.com", "segredo123",
                Usuario.PAPEL_ORGANIZADOR))
                .isInstanceOf(ServicoDeUsuarios.OperacaoRecusada.class)
                .hasMessageContaining("Já existe uma conta");

        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("A troca de senha exige a senha atual correta")
    void recusaTrocaComSenhaAtualErrada() {
        when(repositorio.findByEmail("jarbas@exemplo.com"))
                .thenReturn(Optional.of(usuarioSalvo("jarbas@exemplo.com", "senhaCerta")));

        assertThatThrownBy(() -> servico.trocarSenha("jarbas@exemplo.com", "senhaErrada", "novaSenha1"))
                .isInstanceOf(ServicoDeUsuarios.OperacaoRecusada.class)
                .hasMessageContaining("senha atual");

        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("A nova senha não pode ser igual à atual")
    void recusaNovaSenhaIgualAAtual() {
        when(repositorio.findByEmail("jarbas@exemplo.com"))
                .thenReturn(Optional.of(usuarioSalvo("jarbas@exemplo.com", "senhaCerta")));

        assertThatThrownBy(() -> servico.trocarSenha("jarbas@exemplo.com", "senhaCerta", "senhaCerta"))
                .isInstanceOf(ServicoDeUsuarios.OperacaoRecusada.class)
                .hasMessageContaining("diferente");
    }

    @Test
    @DisplayName("Com a senha atual correta, o hash é substituído")
    void trocaSenhaQuandoAAtualConfere() {
        Usuario usuario = usuarioSalvo("jarbas@exemplo.com", "senhaCerta");
        String hashAntigo = usuario.getSenhaHash();
        when(repositorio.findByEmail("jarbas@exemplo.com")).thenReturn(Optional.of(usuario));

        servico.trocarSenha("jarbas@exemplo.com", "senhaCerta", "novaSenha1");

        assertThat(usuario.getSenhaHash()).isNotEqualTo(hashAntigo);
        assertThat(codificador.matches("novaSenha1", usuario.getSenhaHash())).isTrue();
        verify(repositorio).save(usuario);
    }

    @Test
    @DisplayName("Alterar só o nome, mantendo o próprio e-mail, não é conflito")
    void permiteManterOProprioEmail() {
        Usuario usuario = usuarioSalvo("jarbas@exemplo.com", "segredo123");
        when(repositorio.findByEmail("jarbas@exemplo.com")).thenReturn(Optional.of(usuario));

        servico.alterarDadosPessoais("jarbas@exemplo.com", "Jarbas Jr.", "Jarbas@Exemplo.com");

        assertThat(usuario.getNome()).isEqualTo("Jarbas Jr.");
        assertThat(usuario.getEmail()).isEqualTo("jarbas@exemplo.com");
        verify(repositorio, never()).existsByEmail(anyString());
    }
}
