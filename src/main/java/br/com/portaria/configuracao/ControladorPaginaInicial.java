package br.com.portaria.configuracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Rotas de entrada da aplicação.
 */
@Controller
public class ControladorPaginaInicial {

    /**
     * Se a tela de login deve exibir as credenciais de demonstração.
     *
     * Amarrado a uma PROPRIEDADE, e não ao nome do perfil, pelo mesmo motivo
     * que o console do H2 -- ver ConfiguracaoSeguranca. Um ambiente que por
     * descuido subisse com o perfil de desenvolvimento ainda assim pode
     * desligar o aviso; e quem quiser demonstrar o sistema em outro perfil
     * liga a propriedade, sem precisar de uma exceção no código.
     *
     * O padrão é false: um ambiente que não disse nada não anuncia senha
     * nenhuma na página de login.
     */
    private final boolean exibirCredenciaisDeDemonstracao;

    public ControladorPaginaInicial(
            @Value("${portaria.demonstracao:false}") boolean exibirCredenciaisDeDemonstracao) {
        this.exibirCredenciaisDeDemonstracao = exibirCredenciaisDeDemonstracao;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/eventos";
    }

    @GetMapping("/login")
    public String telaDeLogin(Model modelo) {
        modelo.addAttribute("demonstracao", exibirCredenciaisDeDemonstracao);
        return "login";
    }
}
