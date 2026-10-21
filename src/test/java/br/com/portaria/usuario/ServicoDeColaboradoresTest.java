package br.com.portaria.usuario;

import br.com.portaria.evento.Evento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Regras da equipe do evento.
 *
 * O que estes testes protegem é a confiabilidade da coluna registrado_por do
 * check-in: sem conta própria por colaborador, ela registraria sempre a mesma
 * pessoa, e a pergunta "quem liberou essa entrada?" deixaria de ter resposta.
 */
class ServicoDeColaboradoresTest {

    private RepositorioDeColaboradores colaboradores;
    private RepositorioDeUsuarios usuarios;
    private ServicoDeUsuarios servicoDeUsuarios;
    private ServicoDeRecuperacaoDeSenha convites;
    private ServicoDeColaboradores servico;

    private Evento evento;

    @BeforeEach
    void preparar() {
        colaboradores = mock(RepositorioDeColaboradores.class);
        usuarios = mock(RepositorioDeUsuarios.class);
        servicoDeUsuarios = mock(ServicoDeUsuarios.class);
        convites = mock(ServicoDeRecuperacaoDeSenha.class);
        servico = new ServicoDeColaboradores(colaboradores, usuarios, servicoDeUsuarios, convites);

        evento = new Evento("Encontro", null, "Patio", LocalDateTime.now(),
                LocalDateTime.now().plusHours(8), 300, "encontro");
    }

    @Test
    @DisplayName("Pessoa nova ganha conta própria e recebe convite para definir a senha")
    void pessoaNovaRecebeContaEConvite() {
        when(usuarios.findByEmail("porteiro@exemplo.com")).thenReturn(Optional.empty());
        Usuario criado = new Usuario("Porteiro", "porteiro@exemplo.com", "hash",
                Usuario.PAPEL_COLABORADOR);
        criado.aguardarDefinicaoDeAcesso();
        when(servicoDeUsuarios.criarAguardandoConvite(eq("Porteiro"), eq("porteiro@exemplo.com"),
                eq(Usuario.PAPEL_COLABORADOR))).thenReturn(criado);

        var resultado = servico.incluir(evento, "Porteiro", "Porteiro@Exemplo.com",
                "Portaria", "http://localhost:8080");

        assertThat(resultado).isEqualTo(ServicoDeColaboradores.Resultado.CONVIDADO);
        verify(colaboradores).save(any(ColaboradorDoEvento.class));
        verify(convites).convidar(eq(criado), eq("Encontro"), eq("http://localhost:8080"));
    }

    @Test
    @DisplayName("Quem já tem conta é apenas vinculado, sem conta nova nem convite")
    void pessoaExistenteApenasVinculada() {
        // Conta nasce com acesso definido: entrou no sistema por outro caminho.
        Usuario jaExiste = new Usuario("Ana", "ana@exemplo.com", "hash", Usuario.PAPEL_ORGANIZADOR);
        when(usuarios.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(jaExiste));
        when(colaboradores.existsByEventoIdAndUsuarioId(any(), any())).thenReturn(false);

        var resultado = servico.incluir(evento, "Ana", "ana@exemplo.com", "Apoio",
                "http://localhost:8080");

        assertThat(resultado).isEqualTo(ServicoDeColaboradores.Resultado.VINCULADO);
        verify(servicoDeUsuarios, never()).criarAguardandoConvite(anyString(), anyString(), anyString());
        verify(convites, never()).convidar(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("A mesma pessoa não entra duas vezes no mesmo evento")
    void recusaColaboradorRepetido() {
        Usuario jaExiste = new Usuario("Ana", "ana@exemplo.com", "hash", Usuario.PAPEL_COLABORADOR);
        when(usuarios.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(jaExiste));
        when(colaboradores.existsByEventoIdAndUsuarioId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servico.incluir(evento, "Ana", "ana@exemplo.com", "Apoio",
                "http://localhost:8080"))
                .isInstanceOf(ServicoDeColaboradores.InclusaoRecusada.class)
                .hasMessageContaining("já é colaborador");

        verify(colaboradores, never()).save(any(ColaboradorDoEvento.class));
    }

    @Test
    @DisplayName("Quem foi removido sem nunca ter definido a senha recebe convite ao voltar")
    void reinclusaoSemAcessoDefinidoConvidaDeNovo() {
        // A conta ficou da inclusão anterior, mas a pessoa nunca abriu o link.
        Usuario semAcesso = new Usuario("Porteiro", "porteiro@exemplo.com", "hash-aleatorio",
                Usuario.PAPEL_COLABORADOR);
        semAcesso.aguardarDefinicaoDeAcesso();
        when(usuarios.findByEmail("porteiro@exemplo.com")).thenReturn(Optional.of(semAcesso));
        when(colaboradores.existsByEventoIdAndUsuarioId(any(), any())).thenReturn(false);

        var resultado = servico.incluir(evento, "Porteiro", "porteiro@exemplo.com",
                "Portaria", "http://localhost:8080");

        // Existir conta não basta: sem acesso definido, a pessoa precisa do link.
        assertThat(resultado).isEqualTo(ServicoDeColaboradores.Resultado.CONVIDADO);
        verify(convites).convidar(eq(semAcesso), eq("Encontro"), eq("http://localhost:8080"));
    }

    @Test
    @DisplayName("Reenviar convite para quem já tem senha é recusado, com orientação")
    void naoReenviaConviteParaQuemJaEntra() {
        Usuario comAcesso = new Usuario("Ana", "ana@exemplo.com", "hash", Usuario.PAPEL_COLABORADOR);

        // Um evento recém-construído tem id nulo; só quem veio do banco tem id.
        // Como o serviço confere o vínculo comparando ids, o cenário precisa
        // representar um evento JÁ PERSISTIDO -- caso contrário o teste falharia
        // por um motivo que não existe em produção.
        Evento eventoPersistido = mock(Evento.class);
        when(eventoPersistido.getId()).thenReturn(1L);
        when(eventoPersistido.getNome()).thenReturn("Encontro");

        ColaboradorDoEvento vinculo = new ColaboradorDoEvento(eventoPersistido, comAcesso, "Portaria");
        when(colaboradores.findById(5L)).thenReturn(Optional.of(vinculo));

        assertThatThrownBy(() -> servico.reenviarConvite(1L, 5L, "http://localhost:8080"))
                .isInstanceOf(ServicoDeColaboradores.InclusaoRecusada.class)
                .hasMessageContaining("Esqueci minha senha");

        verify(convites, never()).convidar(any(), anyString(), anyString());
    }
}
