package br.com.portaria.usuario;

import br.com.portaria.evento.Evento;
import br.com.portaria.evento.EventosDoColaborador;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lado adaptador da inversão de dependência declarada em EventosDoColaborador. */
@Component
public class EventosDoColaboradorPorRepositorio implements EventosDoColaborador {

    private final RepositorioDeColaboradores colaboradores;

    public EventosDoColaboradorPorRepositorio(RepositorioDeColaboradores colaboradores) {
        this.colaboradores = colaboradores;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evento> listarPara(String email) {
        return colaboradores.eventosDoColaborador(Usuario.normalizarEmail(email));
    }
}
