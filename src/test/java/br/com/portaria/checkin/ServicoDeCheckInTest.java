package br.com.portaria.checkin;

import br.com.portaria.evento.Evento;
import br.com.portaria.evento.ServicoDeEventos;
import br.com.portaria.inscricao.Inscricao;
import br.com.portaria.inscricao.ServicoDeInscricoes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O invariante mais importante do sistema: a presença de uma pessoa é única.
 *
 * Num portão de evento, o mesmo código é escaneado duas vezes o tempo todo --
 * a pessoa volta ao carro, o leitor bipa duas vezes, o operador clica de novo
 * achando que falhou. Nada disso pode inflar a contagem de público, que é o
 * número usado para lotação e para prestação de contas.
 *
 * O sufixo "Test" na classe e a anotação @Test são vocabulário reservado do
 * JUnit e do plugin Surefire: sem eles, os testes não são encontrados.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServicoDeCheckInTest {

    @Autowired private ServicoDeEventos eventos;
    @Autowired private ServicoDeInscricoes inscricoes;
    @Autowired private ServicoDeCheckIn servicoDeCheckIn;
    @Autowired private RepositorioDeCheckIns checkIns;

    private Inscricao inscricao;
    private Long eventoId;

    @BeforeEach
    void prepararCenario() {
        checkIns.deleteAll();
        Evento evento = eventos.criar("Evento de Teste " + System.nanoTime(), null, "Pátio",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(6), 100);
        eventoId = evento.getId();
        inscricao = inscricoes.inscrever(evento, "Fulano de Tal",
                "fulano+" + System.nanoTime() + "@exemplo.com", null, null, "Gaspar");
    }

    @Test
    @DisplayName("Primeiro check-in libera a entrada")
    void primeiroCheckInLiberaEntrada() {
        ResultadoCheckIn resultado = servicoDeCheckIn.registrarPorCodigo(
                inscricao.getCodigo(), eventoId, "admin");

        assertThat(resultado.isLiberado()).isTrue();
        assertThat(resultado.inscricao().getId()).isEqualTo(inscricao.getId());
        assertThat(checkIns.existsByInscricaoId(inscricao.getId())).isTrue();
    }

    @Test
    @DisplayName("Check-in repetido não cria uma segunda presença")
    void checkInRepetidoEhIdempotente() {
        servicoDeCheckIn.registrarPorCodigo(inscricao.getCodigo(), eventoId, "admin");
        ResultadoCheckIn segundaTentativa = servicoDeCheckIn.registrarPorCodigo(
                inscricao.getCodigo(), eventoId, "admin");

        assertThat(segundaTentativa.isJaPresente()).isTrue();
        assertThat(checkIns.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("O código é aceito do jeito que o operador digitar")
    void codigoEhNormalizado() {
        String comoDigitado = "  " + inscricao.getCodigo().toLowerCase() + " ";

        ResultadoCheckIn resultado = servicoDeCheckIn.registrarPorCodigo(
                comoDigitado, eventoId, "admin");

        assertThat(resultado.isLiberado()).isTrue();
    }

    @Test
    @DisplayName("Código inexistente não derruba a fila, apenas informa o operador")
    void codigoInexistenteApenasInforma() {
        ResultadoCheckIn resultado = servicoDeCheckIn.registrarPorCodigo("ZZZZZZ", eventoId, "admin");

        assertThat(resultado.isNaoEncontrado()).isTrue();
        assertThat(checkIns.count()).isZero();
    }
}
