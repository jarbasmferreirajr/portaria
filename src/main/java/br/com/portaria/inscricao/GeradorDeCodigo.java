package br.com.portaria.inscricao;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

/**
 * Gera o código curto que o participante apresenta na portaria.
 *
 * O alfabeto exclui 0/O e 1/I/L de propósito. Sob chuva, no escuro e com a fila
 * andando, um caractere ambíguo custa mais tempo que um código um pouco maior.
 */
@Component
public class GeradorDeCodigo {

    private static final String ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int TAMANHO = 6;
    private static final int MAXIMO_DE_TENTATIVAS = 20;

    private final SecureRandom sorteador = new SecureRandom();
    private final RepositorioDeInscricoes inscricoes;

    public GeradorDeCodigo(RepositorioDeInscricoes inscricoes) {
        this.inscricoes = inscricoes;
    }

    public String gerar() {
        for (int tentativa = 0; tentativa < MAXIMO_DE_TENTATIVAS; tentativa++) {
            String codigo = sortear();
            if (!inscricoes.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException(
                "Não foi possível gerar um código único após " + MAXIMO_DE_TENTATIVAS + " tentativas");
    }

    private String sortear() {
        StringBuilder codigo = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            codigo.append(ALFABETO.charAt(sorteador.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }
}
