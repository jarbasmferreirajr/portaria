package br.com.portaria.evento;

import java.util.List;

/**
 * Os eventos em que uma pessoa trabalha como colaboradora.
 *
 * Mesma inversão de dependência de ContagemDeInscritos, pelo mesmo motivo: o
 * pacote "usuario" já depende de "evento" (um colaborador é vínculo com um
 * evento). Se "evento" fosse buscar o vínculo diretamente, os dois pacotes
 * ficariam num ciclo. Quem precisa da informação declara o contrato; quem sabe
 * respondê-lo se adapta.
 */
public interface EventosDoColaborador {

    List<Evento> listarPara(String email);
}
