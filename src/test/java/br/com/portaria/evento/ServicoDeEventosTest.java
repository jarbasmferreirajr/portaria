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
    private ServicoDeEventos servico;

    @BeforeEach
    void preparar() {
        repositorio = mock(RepositorioDeEventos.class);
        servico = new ServicoDeEventos(repositorio);
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
}
