package br.com.portaria.inscricao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GeradorDeCodigoTest {

    @Test
    void geraCodigoComTamanhoEAlfabetoEsperados() {
        RepositorioDeInscricoes repositorio = mock(RepositorioDeInscricoes.class);
        when(repositorio.existsByCodigo(anyString())).thenReturn(false);

        String codigo = new GeradorDeCodigo(repositorio).gerar();

        assertThat(codigo).hasSize(6).matches("[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{6}");
        verify(repositorio).existsByCodigo(codigo);
    }
}
