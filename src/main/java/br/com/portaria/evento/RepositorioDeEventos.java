package br.com.portaria.evento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Acesso aos eventos.
 *
 * Os prefixos findBy / existsBy fazem parte do vocabulário reservado do
 * Spring Data: é a partir deles que o framework deriva a consulta SQL
 * automaticamente. Traduzi-los quebraria a derivação.
 */
public interface RepositorioDeEventos extends JpaRepository<Evento, Long> {

    Optional<Evento> findByIdentificadorPublico(String identificadorPublico);

    boolean existsByIdentificadorPublico(String identificadorPublico);
}
