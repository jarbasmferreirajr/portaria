package br.com.portaria.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Escolhe como as mensagens saem do sistema, conforme portaria.email.modo:
 *
 *   log  (padrão) -- escreve a mensagem no log da aplicação. Zero configuração,
 *                    zero rede. É o modo do desenvolvimento e da apresentação.
 *   smtp          -- envia de verdade, por um servidor SMTP. Aponte-o para um
 *                    Mailpit local (docker run -p 1025:1025 -p 8025:8025
 *                    axllent/mailpit) para ver o e-mail chegando sem internet,
 *                    ou para o servidor real em produção.
 *
 * A propriedade é explícita de propósito: deixar o comportamento depender da
 * simples presença de uma configuração de SMTP tornaria difícil saber, olhando
 * o projeto, qual caminho está ativo.
 */
@Configuration
public class ConfiguracaoDeEmail {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracaoDeEmail.class);

    @Bean
    @ConditionalOnProperty(name = "portaria.email.modo", havingValue = "smtp")
    public EnviadorDeEmail enviadorPorSmtp(JavaMailSender remetente,
                                           @org.springframework.beans.factory.annotation.Value(
                                                   "${portaria.email.remetente:nao-responda@portaria.local}")
                                           String de) {
        return (destinatario, assunto, corpo) -> {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(de);
            mensagem.setTo(destinatario);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);
            remetente.send(mensagem);
            log.info("E-mail enviado para {} com o assunto \"{}\"", destinatario, assunto);
        };
    }

    @Bean
    @ConditionalOnProperty(name = "portaria.email.modo", havingValue = "log", matchIfMissing = true)
    public EnviadorDeEmail enviadorPorLog() {
        return (destinatario, assunto, corpo) ->
                log.info("""

                        ===================== E-MAIL (modo log) =====================
                        Para....: {}
                        Assunto.: {}

                        {}
                        =============================================================
                        """, destinatario, assunto, corpo);
    }
}
