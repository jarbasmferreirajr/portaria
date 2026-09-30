package br.com.portaria.inscricao;

import br.com.portaria.evento.Evento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regra de negócio das inscrições.
 */
@Service
public class ServicoDeInscricoes {

    private final RepositorioDeInscricoes inscricoes;
    private final GeradorDeCodigo geradorDeCodigo;

    public ServicoDeInscricoes(RepositorioDeInscricoes inscricoes, GeradorDeCodigo geradorDeCodigo) {
        this.inscricoes = inscricoes;
        this.geradorDeCodigo = geradorDeCodigo;
    }

    /** Inscrição recusada por uma regra de negócio, não por falha técnica. */
    public static class InscricaoRecusada extends RuntimeException {
        public InscricaoRecusada(String mensagem) {
            super(mensagem);
        }
    }

    /**
     * Cria a inscrição e devolve o código de acesso.
     *
     * LIMITAÇÃO CONHECIDA (está no backlog): a checagem de capacidade abaixo é um
     * check-then-act -- duas requisições simultâneas para a última vaga podem
     * passar juntas. A correção planejada é uma trava pessimista no evento ou uma
     * coluna de vagas com atualização condicional. Mantido simples nesta entrega,
     * e registrado aqui de propósito para não passar por descuido.
     */
    @Transactional
    public Inscricao inscrever(Evento evento, String nome, String email, String telefone,
                               String motoclube, String cidade) {
        if (evento.isEncerrado()) {
            throw new InscricaoRecusada("As inscrições para este evento foram encerradas.");
        }
        long jaInscritos = inscricoes.countByEventoId(evento.getId());
        if (jaInscritos >= evento.getCapacidade()) {
            throw new InscricaoRecusada("As vagas para este evento se esgotaram.");
        }
        if (inscricoes.existsByEventoIdAndEmailIgnoreCase(evento.getId(), email)) {
            throw new InscricaoRecusada("Este e-mail já possui inscrição neste evento.");
        }
        String codigo = geradorDeCodigo.gerar();
        return inscricoes.save(new Inscricao(evento, nome, email, telefone, motoclube, cidade, codigo));
    }

    @Transactional(readOnly = true)
    public List<Inscricao> listarDoEvento(Long eventoId) {
        return inscricoes.findByEventoIdOrderByNomeAsc(eventoId);
    }

    @Transactional(readOnly = true)
    public long contarDoEvento(Long eventoId) {
        return inscricoes.countByEventoId(eventoId);
    }

    @Transactional(readOnly = true)
    public Inscricao porCodigo(String codigo) {
        return inscricoes.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Código não encontrado"));
    }
}
