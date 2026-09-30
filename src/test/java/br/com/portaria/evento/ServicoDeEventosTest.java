package br.com.portaria.evento;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicoDeEventosTest {

    private RepositorioDeEventos repositorio;
    private ContagemDeInscritos inscritos;
    private ServicoDeEventos servico;

    @BeforeEach
    void preparar() {
        repositorio = mock(RepositorioDeEventos.class);
        inscritos = mock(ContagemDeInscritos.class);
        servico = new ServicoDeEventos(repositorio, inscritos);
    }

    @Test
    void criaIdentificadorNormalizadoEPreencheEvento() {
        when(repositorio.existsByIdentificadorPublico("encontro-de-motos")).thenReturn(false);
        when(repositorio.save(any(Evento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Evento resultado = servico.criar("Encontro de Motos!", "Descricao", "Praca",
                LocalDateTime.now(), LocalDateTime.now().plusHours(2), 50);

        assertThat(resultado.getIdentificadorPublico()).isEqualTo("encontro-de-motos");
        assertThat(resultado.getNome()).isEqualTo("Encontro de Motos!");
        verify(repositorio).save(any(Evento.class));
    }

    @Test
    void adicionaSufixoQuandoIdentificadorJaExiste() {
        when(repositorio.existsByIdentificadorPublico("evento")).thenReturn(true);
        when(repositorio.existsByIdentificadorPublico("evento-2")).thenReturn(false);
        when(repositorio.save(any(Evento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Evento resultado = servico.criar("!!!", null, "Local", LocalDateTime.now(), null, 10);

        assertThat(resultado.getIdentificadorPublico()).isEqualTo("evento-2");
    }

    @Test
    void listaEBuscaEventosDelegandoAoRepositorio() {
        Evento evento = mock(Evento.class);
        when(repositorio.findAll()).thenReturn(List.of(evento));
        when(repositorio.findById(7L)).thenReturn(Optional.of(evento));
        when(repositorio.findByIdentificadorPublico("evento")).thenReturn(Optional.of(evento));

        assertThat(servico.listar()).containsExactly(evento);
        assertThat(servico.porId(7L)).isSameAs(evento);
        assertThat(servico.porIdentificadorPublico("evento")).isSameAs(evento);
    }

    @Test
    void rejeitaEventoInexistente() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());
        when(repositorio.findByIdentificadorPublico("ausente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.porId(99L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servico.porIdentificadorPublico("ausente"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encerrarFechaInscricoesEReabrirDesfaz() {
        Evento evento = new Evento("Encontro", null, "Patio", LocalDateTime.now(),
                LocalDateTime.now().plusHours(4), 100, "encontro");
        when(repositorio.findById(7L)).thenReturn(Optional.of(evento));
        when(repositorio.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        servico.encerrar(7L);
        assertThat(evento.isEncerrado()).isTrue();
        assertThat(evento.getEncerradoEm()).isNotNull();

        servico.reabrir(7L);
        assertThat(evento.isEncerrado()).isFalse();
    }

    @Test
    void atualizarNaoMudaOIdentificadorPublico() {
        Evento evento = new Evento("Nome Antigo", null, "Patio", LocalDateTime.now(),
                LocalDateTime.now().plusHours(4), 100, "nome-antigo");
        when(repositorio.findById(7L)).thenReturn(Optional.of(evento));
        when(repositorio.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        servico.atualizar(7L, "Nome Novo", "Outra descricao", "Outro local",
                LocalDateTime.now(), LocalDateTime.now().plusHours(6), 250);

        assertThat(evento.getNome()).isEqualTo("Nome Novo");
        assertThat(evento.getCapacidade()).isEqualTo(250);
        // O link ja foi divulgado: nao pode mudar junto com o nome.
        assertThat(evento.getIdentificadorPublico()).isEqualTo("nome-antigo");
    }

    @Test
    void recusaCapacidadeMenorQueOsJaInscritos() {
        Evento evento = new Evento("Encontro", null, "Patio", LocalDateTime.now(),
                LocalDateTime.now().plusHours(4), 300, "encontro");
        when(repositorio.findById(7L)).thenReturn(Optional.of(evento));
        when(inscritos.contarDoEvento(7L)).thenReturn(50L);

        assertThatThrownBy(() -> servico.atualizar(7L, "Encontro", null, "Patio",
                LocalDateTime.now(), LocalDateTime.now().plusHours(4), 10))
                .isInstanceOf(ServicoDeEventos.AlteracaoRecusada.class)
                .hasMessageContaining("50 inscritos");

        // A capacidade original permanece intacta.
        assertThat(evento.getCapacidade()).isEqualTo(300);
    }
}
