package br.com.portaria.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Acesso aos usuários.
 *
 * Os prefixos findBy / existsBy pertencem ao vocabulário reservado do Spring
 * Data: é deles que o framework deriva a consulta.
 */
public interface RepositorioDeUsuarios extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
