package br.com.portaria.configuracao;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Rotas de entrada da aplicação.
 */
@Controller
public class ControladorPaginaInicial {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/eventos";
    }

    @GetMapping("/login")
    public String telaDeLogin() {
        return "login";
    }
}
