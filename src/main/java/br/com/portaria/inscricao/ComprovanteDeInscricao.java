package br.com.portaria.inscricao;

import br.com.portaria.email.EnviadorDeEmail;
import br.com.portaria.evento.Evento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Envia ao participante, por e-mail, o código que ele vai apresentar na portaria.
 *
 * Duas decisões que valem explicação:
 *
 * 1. NÃO participa da transação da inscrição. É chamado depois que a inscrição
 *    já está gravada, nunca dentro dela. Se o envio fosse parte da transação,
 *    um servidor de e-mail lento seguraria a resposta ao participante, e uma
 *    falha no envio desfaria uma inscrição perfeitamente válida.
 *
 * 2. ENGOLE a falha, registrando no log. A tela de confirmação já mostra o
 *    código, e o participante foi orientado a tirar um print -- o e-mail é
 *    conveniência, não a fonte da verdade. Derrubar a confirmação porque o
 *    SMTP caiu trocaria um problema pequeno por um grande.
 */
@Component
public class ComprovanteDeInscricao {

    private static final Logger log = LoggerFactory.getLogger(ComprovanteDeInscricao.class);

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final EnviadorDeEmail enviador;

    public ComprovanteDeInscricao(EnviadorDeEmail enviador) {
        this.enviador = enviador;
    }

    public void enviar(Inscricao inscricao, Evento evento, String linkDaConfirmacao) {
        try {
            enviador.enviar(inscricao.getEmail(),
                    "Portaria - seu codigo para " + evento.getNome(),
                    corpo(inscricao, evento, linkDaConfirmacao));
        } catch (RuntimeException falha) {
            log.warn("Não foi possível enviar o comprovante da inscrição {} para {}: {}",
                    inscricao.getCodigo(), inscricao.getEmail(), falha.getMessage());
        }
    }

    private String corpo(Inscricao inscricao, Evento evento, String link) {
        return """
               Olá, %s.

               Sua inscrição em "%s" está confirmada.

               SEU CÓDIGO DE ENTRADA: %s

               Apresente este código na portaria do evento. Guarde esta mensagem
               ou tire um print da tela de confirmação -- o código funciona mesmo
               sem sinal de celular.

               Onde...: %s
               Quando.: %s

               Se precisar ver o comprovante de novo:
               %s

               --
               Portaria
               """.formatted(inscricao.getNome(), evento.getNome(), inscricao.getCodigo(),
                             evento.getLocal(), DATA.format(evento.getInicio()), link);
    }
}
