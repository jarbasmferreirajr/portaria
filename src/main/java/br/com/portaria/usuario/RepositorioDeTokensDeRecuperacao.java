package br.com.portaria.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepositorioDeTokensDeRecuperacao extends JpaRepository<TokenDeRecuperacao, Long> {

    /**
     * Carrega o usuário junto: o token só é útil acompanhado de quem ele
     * autoriza, e spring.jpa.open-in-view está desligado.
     */
    @Query("select t from TokenDeRecuperacao t join fetch t.usuario where t.tokenHash = :hash")
    Optional<TokenDeRecuperacao> buscarPorHashComUsuario(@Param("hash") String hash);

    List<TokenDeRecuperacao> findByUsuarioIdAndUsadoEmIsNull(Long usuarioId);
}
