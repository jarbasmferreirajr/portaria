package br.com.portaria.inscricao;

import br.com.portaria.evento.Evento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicoDeInscricoesTest {

    private RepositorioDeInscricoes repositorio;
    private GeradorDeCodigo gerador;
    private ServicoDeInscricoes servico;
    private Evento evento;

    @BeforeEach
    void preparar() {
        repositorio = mock(RepositorioDeInscricoes.class);
        gerador = mock(GeradorDeCodigo.class);
        servico = new ServicoDeInscricoes(repositorio, gerador);
        evento = mock(Evento.class);
        when(evento.getId()).thenReturn(4L);
        when(evento.getCapacidade()).thenReturn(2);
    }

    @Test
    void inscreveQuandoHaVaga() {
        when(repositorio.countByEventoId(4L)).thenReturn(1L);
        when(repositorio.existsByEventoIdAndEmailIgnoreCase(4L, "ana@example.com")).thenReturn(false);
        when(gerador.gerar()).thenReturn("ABC234");
        when(repositorio.save(any(Inscricao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Inscricao resultado = servico.inscrever(evento, "Ana", "ana@example.com", null, null, "Gaspar");

        assertThat(resultado.getEvento()).isSameAs(evento);
        assertThat(resultado.getNome()).isEqualTo("Ana");
        assertThat(resultado.getCodigo()).isEqualTo("ABC234");
        verify(repositorio).save(any(Inscricao.class));
    }

    @Test
    void recusaQuandoCapacidadeEsgotada() {
        when(repositorio.countByEventoId(4L)).thenReturn(2L);

        assertThatThrownBy(() -> servico.inscrever(evento, "Ana", "ana@example.com", null, null, null))
                .isInstanceOf(ServicoDeInscricoes.InscricaoRecusada.class)
                .hasMessage("As vagas para este evento se esgotaram.");
        verifyNoInteractions(gerador);
        verify(repositorio, never()).save(any());
    }

    @Test
    void recusaEmailDuplicado() {
        when(repositorio.countByEventoId(4L)).thenReturn(0L);
        when(repositorio.existsByEventoIdAndEmailIgnoreCase(4L, "ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> servico.inscrever(evento, "Ana", "ana@example.com", null, null, null))
                .isInstanceOf(ServicoDeInscricoes.InscricaoRecusada.class)
                .hasMessage("Este e-mail já possui inscrição neste evento.");
        verifyNoInteractions(gerador);
    }

    @Test
    void consultaListagemContagemEPorCodigo() {
        Inscricao inscricao = mock(Inscricao.class);
        when(repositorio.findByEventoIdOrderByNomeAsc(4L)).thenReturn(List.of(inscricao));
        when(repositorio.countByEventoId(4L)).thenReturn(1L);
        when(repositorio.findByCodigo("ABC234")).thenReturn(Optional.of(inscricao));

        assertThat(servico.listarDoEvento(4L)).containsExactly(inscricao);
        assertThat(servico.contarDoEvento(4L)).isEqualTo(1L);
        assertThat(servico.porCodigo("ABC234")).isSameAs(inscricao);
    }
}

