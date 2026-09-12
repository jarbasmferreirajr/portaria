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

    public ServicoDeEventos(RepositorioDeEventos eventos) {
        this.eventos = eventos;
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
