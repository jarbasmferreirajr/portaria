package br.com.portaria.evento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Regra de negócio dos eventos.
 */
@Service
public class ServicoDeEventos {

    private final RepositorioDeEventos eventos;
    private final ContagemDeInscritos inscritos;

    public ServicoDeEventos(RepositorioDeEventos eventos, ContagemDeInscritos inscritos) {
        this.eventos = eventos;
        this.inscritos = inscritos;
    }

    /** Alteração recusada por uma regra de negócio, não por falha técnica. */
    public static class AlteracaoRecusada extends RuntimeException {
        public AlteracaoRecusada(String mensagem) {
            super(mensagem);
        }
    }

    @Transactional(readOnly = true)
    public List<Evento> listar() {
        return eventos.findAll();
    }

    @Transactional(readOnly = true)
    public Evento porId(Long id) {
        return eventos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Evento porIdentificadorPublico(String identificador) {
        return eventos.findByIdentificadorPublico(identificador)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado: " + identificador));
    }

    @Transactional
    public Evento criar(String nome, String descricao, String local, LocalDateTime inicio,
                        LocalDateTime fim, Integer capacidade) {
        String identificador = gerarIdentificadorUnico(nome);
        return eventos.save(new Evento(nome, descricao, local, inicio, fim, capacidade, identificador));
    }

    /**
     * Atualiza os dados do evento.
     *
     * Editar um evento ENCERRADO é permitido de propósito: encerrar fecha as
     * inscrições, e o evento ainda vai acontecer -- trocar o local por causa da
     * chuva ou corrigir o horário continuam sendo operações legítimas.
     *
     * O que não se permite é reduzir a capacidade abaixo de quem já está
     * inscrito: o sistema passaria a informar uma lotação que não corresponde à
     * realidade, e é essa informação que o painel de portaria usa.
     */
    @Transactional
    public Evento atualizar(Long id, String nome, String descricao, String local,
                            LocalDateTime inicio, LocalDateTime fim, Integer capacidade) {
        Evento evento = porId(id);

        long jaInscritos = inscritos.contarDoEvento(id);
        if (capacidade != null && capacidade < jaInscritos) {
            throw new AlteracaoRecusada(
                    "Este evento já tem " + jaInscritos + " inscritos. "
                    + "A capacidade não pode ser menor que isso.");
        }

        evento.alterarDados(nome, descricao, local, inicio, fim, capacidade);
        return eventos.save(evento);
    }

    @Transactional
    public void encerrar(Long id) {
        Evento evento = porId(id);
        evento.encerrar();
        eventos.save(evento);
    }

    @Transactional
    public void reabrir(Long id) {
        Evento evento = porId(id);
        evento.reabrir();
        eventos.save(evento);
    }

    /**
     * Monta o identificador usado na URL pública a partir do nome do evento:
     * "3º Encontro de Motociclistas" vira "3o-encontro-de-motociclistas".
     *
     * Acentos são removidos porque a URL precisa sobreviver a ser digitada à mão,
     * colada em aplicativo de mensagem e impressa em cartaz.
     */
    private String gerarIdentificadorUnico(String nome) {
        String base = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "evento";
        }
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        String candidato = base;
        int sufixo = 2;
        while (eventos.existsByIdentificadorPublico(candidato)) {
            candidato = base + "-" + sufixo++;
        }
        return candidato;
    }
}
