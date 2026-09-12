package br.com.portaria.inscricao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acesso às inscrições.
 *
 * Os prefixos findBy / existsBy / countBy pertencem ao vocabulário reservado do
 * Spring Data. Onde a consulta é escrita à mão com @Query, o nome do método é
 * livre e está em português.
 */
public interface RepositorioDeInscricoes extends JpaRepository<Inscricao, Long> {

    Optional<Inscricao> findByCodigo(String codigo);

    /**
     * Carrega a inscrição já com o evento associado.
     *
     * Como spring.jpa.open-in-view está desligado, a sessão da JPA se encerra
     * antes da renderização da página: acessar o evento depois disso lançaria
     * LazyInitializationException. O join fetch resolve isso na própria consulta.
     */
    @Query("select i from Inscricao i join fetch i.evento where i.codigo = :codigo")
    Optional<Inscricao> buscarPorCodigoComEvento(@Param("codigo") String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByEventoIdAndEmailIgnoreCase(Long eventoId, String email);

    List<Inscricao> findByEventoIdOrderByNomeAsc(Long eventoId);

    long countByEventoId(Long eventoId);
}
