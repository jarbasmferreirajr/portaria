package br.com.portaria.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import br.com.portaria.evento.Evento;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeColaboradores extends JpaRepository<ColaboradorDoEvento, Long> {

    /**
     * Lista os colaboradores de um evento já com os dados da pessoa.
     * O join fetch é obrigatório: open-in-view está desligado.
     */
    @Query("""
           select c from ColaboradorDoEvento c
             join fetch c.usuario u
            where c.evento.id = :eventoId
            order by u.nome asc
           """)
    List<ColaboradorDoEvento> listarPorEvento(@Param("eventoId") Long eventoId);

    Optional<ColaboradorDoEvento> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    boolean existsByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    /** Os eventos em que esta pessoa trabalha -- é a lista que ela vê ao entrar. */
    @Query("""
           select c.evento from ColaboradorDoEvento c
            where lower(c.usuario.email) = lower(:email)
            order by c.evento.inicio desc
           """)
    List<Evento> eventosDoColaborador(@Param("email") String email);

    /** Usada no controle de acesso: esta pessoa pode operar este evento? */
    @Query("""
           select count(c) > 0 from ColaboradorDoEvento c
            where c.evento.id = :eventoId and lower(c.usuario.email) = lower(:email)
           """)
    boolean temVinculo(@Param("eventoId") Long eventoId, @Param("email") String email);
}
