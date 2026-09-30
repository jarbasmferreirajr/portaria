package br.com.portaria.configuracao;

import br.com.portaria.usuario.RepositorioDeUsuarios;
import br.com.portaria.usuario.Usuario;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Coloca o usuário autenticado no modelo de toda página.
 *
 * Os templates antes liam ${#authentication}, um objeto que vem do módulo
 * thymeleaf-extras-springsecurity. Esse módulo saiu do projeto na migração para
 * o Spring Boot 4, e a expressão passou a resolver para nulo silenciosamente --
 * o nome do operador sumiu da barra superior e do painel de portaria sem que
 * nada quebrasse de forma visível.
 *
 * Resolver aqui, com o próprio Spring MVC, elimina a dependência de um módulo
 * externo e ainda melhora a tela: mostramos o NOME da pessoa, e não o e-mail
 * com que ela faz login.
 *
 * O custo é uma consulta por requisição renderizada. É uma busca por chave
 * única e indexada; se um dia pesar, o caminho é guardar o nome na sessão.
 */
@ControllerAdvice
public class AtributosDoUsuarioAutenticado {

    private final RepositorioDeUsuarios usuarios;

    public AtributosDoUsuarioAutenticado(RepositorioDeUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @ModelAttribute("usuarioAutenticado")
    public Usuario usuarioAutenticado(Principal autenticado) {
        if (autenticado == null) {
            return null;  // páginas públicas: inscrição e login
        }
        return usuarios.findByEmail(Usuario.normalizarEmail(autenticado.getName()))
                .orElse(null);
    }
}
