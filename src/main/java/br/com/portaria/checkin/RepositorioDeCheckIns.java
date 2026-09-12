package br.com.portaria.checkin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acesso aos registros de entrada.
 */
public interface RepositorioDeCheckIns extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findByInscricaoId(Long inscricaoId);

    boolean existsByInscricaoId(Long inscricaoId);

    @Query("""
           select c from CheckIn c
             join fetch c.inscricao i
            where i.evento.id = :eventoId
            order by c.registradoEm desc
           """)
    List<CheckIn> listarPorEvento(@Param("eventoId") Long eventoId);

    @Query("select count(c) from CheckIn c where c.inscricao.evento.id = :eventoId")
    long contarPorEvento(@Param("eventoId") Long eventoId);
}
