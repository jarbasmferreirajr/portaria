package br.com.portaria.checkin;

import br.com.portaria.inscricao.Inscricao;
import java.time.LocalDateTime;

/**
 * Resultado de uma tentativa de check-in, na linguagem do operador da portaria.
 *
 * Os três estados são deliberadamente distintos: JA_PRESENTE não é erro nem
 * sucesso silencioso. Num evento lotado, o segundo escaneamento do mesmo código
 * pode significar comprovante compartilhado -- o operador precisa ver isso.
 *
 * A dica acompanha apenas o NAO_ENCONTRADO e é opcional. Serve para quando o
 * sistema consegue dizer algo mais útil que "não encontrado" -- por exemplo,
 * que o código digitado tem um caractere que o gerador nunca produz.
 *
 * Os métodos getXxx() existem porque o Thymeleaf lê propriedades pela convenção
 * JavaBean; os acessadores curtos de record não são reconhecidos por ela.
 */
public record ResultadoCheckIn(Situacao situacao, Inscricao inscricao, LocalDateTime momento,
                               String dica) {

    public enum Situacao {
        LIBERADO,
        JA_PRESENTE,
        NAO_ENCONTRADO
    }

    public static ResultadoCheckIn liberado(Inscricao inscricao, LocalDateTime momento) {
        return new ResultadoCheckIn(Situacao.LIBERADO, inscricao, momento, null);
    }

    public static ResultadoCheckIn jaPresente(Inscricao inscricao, LocalDateTime momento) {
        return new ResultadoCheckIn(Situacao.JA_PRESENTE, inscricao, momento, null);
    }

    public static ResultadoCheckIn naoEncontrado() {
        return naoEncontrado(null);
    }

    public static ResultadoCheckIn naoEncontrado(String dica) {
        return new ResultadoCheckIn(Situacao.NAO_ENCONTRADO, null, null, dica);
    }

    public Situacao getSituacao()     { return situacao; }
    public Inscricao getInscricao()   { return inscricao; }
    public LocalDateTime getMomento() { return momento; }
    public String getDica()           { return dica; }

    public boolean isLiberado()      { return situacao == Situacao.LIBERADO; }
    public boolean isJaPresente()    { return situacao == Situacao.JA_PRESENTE; }
    public boolean isNaoEncontrado() { return situacao == Situacao.NAO_ENCONTRADO; }
}
