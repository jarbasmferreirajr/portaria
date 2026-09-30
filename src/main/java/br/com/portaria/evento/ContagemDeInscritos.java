package br.com.portaria.evento;

/**
 * Quantas pessoas já se inscreveram em um evento.
 *
 * A interface é declarada AQUI, no pacote que precisa da informação, e
 * implementada no pacote de inscrições. Sem isso, "evento" passaria a depender
 * de "inscricao" -- que já depende de "evento" -- e os dois pacotes ficariam
 * presos num ciclo.
 *
 * É inversão de dependência: quem precisa do serviço define o contrato, e quem
 * sabe respondê-lo se adapta a ele.
 */
public interface ContagemDeInscritos {

    long contarDoEvento(Long eventoId);
}
