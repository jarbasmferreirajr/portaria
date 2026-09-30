package br.com.portaria.usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Fluxo público de recuperação de senha: quem esqueceu a senha não consegue
 * fazer login, então estas páginas não podem exigir autenticação.
 */
@Controller
public class ControladorDeRecuperacaoDeSenha {

    private final ServicoDeRecuperacaoDeSenha recuperacao;

    public ControladorDeRecuperacaoDeSenha(ServicoDeRecuperacaoDeSenha recuperacao) {
        this.recuperacao = recuperacao;
    }

    public static class FormularioPedido {

        @NotBlank(message = "Informe seu e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class FormularioNovaSenha {

        @NotBlank
        private String token;

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres")
        private String novaSenha;

        @NotBlank(message = "Repita a nova senha")
        private String confirmacao;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNovaSenha() { return novaSenha; }
        public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
        public String getConfirmacao() { return confirmacao; }
        public void setConfirmacao(String confirmacao) { this.confirmacao = confirmacao; }
    }

    // ---------- pedir o link ----------

    @GetMapping("/recuperar-senha")
    public String pedir(Model modelo) {
        if (!modelo.containsAttribute("formulario")) {
            modelo.addAttribute("formulario", new FormularioPedido());
        }
        return "conta/esqueci-senha";
    }

    @PostMapping("/recuperar-senha")
    public String solicitar(@ModelAttribute("formulario") @Valid FormularioPedido formulario,
                            BindingResult erros) {
        if (erros.hasErrors()) {
            return "conta/esqueci-senha";
        }

        String urlBase = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        recuperacao.solicitar(formulario.getEmail(), urlBase);

        // Sempre o mesmo destino, exista ou não a conta: a resposta não pode
        // revelar quais e-mails estão cadastrados.
        return "redirect:/recuperar-senha?enviado";
    }

    // ---------- usar o link ----------

    @GetMapping("/redefinir-senha")
    public String abrirRedefinicao(@RequestParam("token") String token, Model modelo) {
        try {
            recuperacao.exigirTokenValido(token);
        } catch (ServicoDeRecuperacaoDeSenha.TokenInvalido invalido) {
            modelo.addAttribute("erroToken", invalido.getMessage());
            return "conta/redefinir-senha";
        }

        FormularioNovaSenha formulario = new FormularioNovaSenha();
        formulario.setToken(token);
        modelo.addAttribute("formulario", formulario);
        return "conta/redefinir-senha";
    }

    @PostMapping("/redefinir-senha")
    public String redefinir(@ModelAttribute("formulario") @Valid FormularioNovaSenha formulario,
                            BindingResult erros, Model modelo) {

        if (!erros.hasErrors() && !formulario.getNovaSenha().equals(formulario.getConfirmacao())) {
            erros.rejectValue("confirmacao", "confirmacao.divergente",
                    "A confirmação não confere com a nova senha");
        }
        if (erros.hasErrors()) {
            return "conta/redefinir-senha";
        }

        try {
            recuperacao.redefinir(formulario.getToken(), formulario.getNovaSenha());
        } catch (ServicoDeRecuperacaoDeSenha.TokenInvalido invalido) {
            modelo.addAttribute("erroToken", invalido.getMessage());
            return "conta/redefinir-senha";
        }

        return "redirect:/login?senhaRedefinida";
    }
}
