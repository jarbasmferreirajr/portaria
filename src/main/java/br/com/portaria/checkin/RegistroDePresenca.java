package br.com.portaria.checkin;

import br.com.portaria.inscricao.Inscricao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isola a gravação do check-in em uma transação própria.
 *
 * Existe como componente separado por um motivo técnico concreto: quando a
 * restrição UNIQUE dispara, a transação corrente fica marcada como
 * rollback-only e não pode mais ser usada. Se o try/catch estivesse dentro da
 * mesma transação do serviço, a consulta seguinte também falharia. Com a
 * gravação isolada aqui, o serviço captura a violação já fora da transação que
 * morreu e continua atendendo a fila.
 */
@Component
public class RegistroDePresenca {

    private final RepositorioDeCheckIns checkIns;

    public RegistroDePresenca(RepositorioDeCheckIns checkIns) {
        this.checkIns = checkIns;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CheckIn gravar(Inscricao inscricao, String operador) {
        return checkIns.saveAndFlush(new CheckIn(inscricao, operador));
    }
}
