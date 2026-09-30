package br.com.portaria.inscricao;

import br.com.portaria.evento.ContagemDeInscritos;
import org.springframework.stereotype.Component;

/**
 * Responde à contagem pedida pelo pacote de eventos, consultando o repositório
 * de inscrições. É o lado adaptador da inversão de dependência.
 */
@Component
public class ContagemDeInscritosPorRepositorio implements ContagemDeInscritos {

    private final RepositorioDeInscricoes inscricoes;

    public ContagemDeInscritosPorRepositorio(RepositorioDeInscricoes inscricoes) {
        this.inscricoes = inscricoes;
    }

    @Override
    public long contarDoEvento(Long eventoId) {
        return inscricoes.countByEventoId(eventoId);
    }
}
