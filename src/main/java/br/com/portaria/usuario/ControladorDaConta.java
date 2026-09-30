package br.com.portaria.usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Tela "Minha conta": alterar dados pessoais e trocar a senha.
 *
 * Só existe porque o usuário passou a ser persistido -- não se altera a senha
 * de quem vivia fixo em memória.
 */
@Controller
@RequestMapping("/conta")
public class ControladorDaConta {

    private static final String PAGINA = "conta/minha-conta";

    private final ServicoDeUsuarios usuarios;

    public ControladorDaConta(ServicoDeUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    /** Dados de contato do organizador. Só nome e e-mail são obrigatórios. */
    public static class FormularioDadosDoOrganizador {

        @NotBlank(message = "Informe seu nome")
        @Size(max = 150)
        private String nome;

        @NotBlank(message = "Informe seu e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @Size(max = 30, message = "Telefone longo demais")
        private String telefone;

        @Size(max = 200, message = "Endereço longo demais")
        private String endereco;

        @Size(max = 150, message = "Endereço do site longo demais")
        private String site;

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTelefone() { return telefone; }
        public void setTelefone(String telefone) { this.telefone = telefone; }
        public String getEndereco() { return endereco; }
        public void setEndereco(String endereco) { this.endereco = endereco; }
        public String getSite() { return site; }
        public void setSite(String site) { this.site = site; }
    }

    public static class FormularioSenha {

        @NotBlank(message = "Informe a senha atual")
        private String senhaAtual;

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, message = "A nova senha deve ter ao menos 8 caracteres")
        private String novaSenha;

        @NotBlank(message = "Repita a nova senha")
        private String confirmacao;

        public String getSenhaAtual() { return senhaAtual; }
        public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
        public String getNovaSenha() { return novaSenha; }
        public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
        public String getConfirmacao() { return confirmacao; }
        public void setConfirmacao(String confirmacao) { this.confirmacao = confirmacao; }
    }

    @GetMapping
    public String abrir(Principal autenticado, Model modelo) {
        Usuario usuario = usuarios.porEmail(autenticado.getName());
        preencher(modelo, usuario);
        return PAGINA;
    }

    @PostMapping("/dados")
    public String alterarDados(@ModelAttribute("formularioDados") @Valid FormularioDadosDoOrganizador formulario,
                               BindingResult erros, Principal autenticado, Model modelo,
                               HttpServletRequest requisicao, HttpServletResponse resposta,
                               RedirectAttributes atributos) {

        Usuario usuario = usuarios.porEmail(autenticado.getName());

        if (erros.hasErrors()) {
            preencher(modelo, usuario);
            return PAGINA;
        }

        boolean mudouDeEmail = !Usuario.normalizarEmail(formulario.getEmail()).equals(usuario.getEmail());

        try {
            usuarios.alterarDados(autenticado.getName(), new DadosDoOrganizador(
                    formulario.getNome(), formulario.getEmail(), formulario.getTelefone(),
                    formulario.getEndereco(), formulario.getSite()));
        } catch (ServicoDeUsuarios.OperacaoRecusada recusa) {
            modelo.addAttribute("erroDados", recusa.getMessage());
            preencher(modelo, usuario);
            return PAGINA;
        }

        // O e-mail é o identificador de login: se mudou, a sessão atual passa a
        // apontar para um usuário que não existe mais com aquele nome. Encerrar
        // a sessão é mais honesto que remendar o contexto de segurança.
        if (mudouDeEmail) {
            encerrarSessao(requisicao, resposta);
            return "redirect:/login?emailAlterado";
        }

        atributos.addFlashAttribute("sucesso", "Dados do organizador atualizados.");
        return "redirect:/conta";
    }

    @PostMapping("/senha")
    public String trocarSenha(@ModelAttribute("formularioSenha") @Valid FormularioSenha formulario,
                              BindingResult erros, Principal autenticado, Model modelo,
                              HttpServletRequest requisicao, HttpServletResponse resposta) {

        Usuario usuario = usuarios.porEmail(autenticado.getName());

        if (!erros.hasErrors() && !formulario.getNovaSenha().equals(formulario.getConfirmacao())) {
            erros.rejectValue("confirmacao", "confirmacao.divergente",
                    "A confirmação não confere com a nova senha");
        }

        if (erros.hasErrors()) {
            preencher(modelo, usuario);
            return PAGINA;
        }

        try {
            usuarios.trocarSenha(autenticado.getName(), formulario.getSenhaAtual(),
                    formulario.getNovaSenha());
        } catch (ServicoDeUsuarios.OperacaoRecusada recusa) {
            modelo.addAttribute("erroSenha", recusa.getMessage());
            preencher(modelo, usuario);
            return PAGINA;
        }

        // Senha trocada encerra a sessão de propósito: se a troca aconteceu
        // porque a senha antiga vazou, qualquer sessão aberta com ela precisa
        // morrer junto.
        encerrarSessao(requisicao, resposta);
        return "redirect:/login?senhaAlterada";
    }

    private void encerrarSessao(HttpServletRequest requisicao, HttpServletResponse resposta) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(requisicao, resposta, autenticacao);
    }

    /** Repõe no modelo o que a página precisa, preservando o que o usuário digitou. */
    private void preencher(Model modelo, Usuario usuario) {
        modelo.addAttribute("usuario", usuario);

        if (!modelo.containsAttribute("formularioDados")) {
            FormularioDadosDoOrganizador dados = new FormularioDadosDoOrganizador();
            dados.setNome(usuario.getNome());
            dados.setEmail(usuario.getEmail());
            dados.setTelefone(usuario.getTelefone());
            dados.setEndereco(usuario.getEndereco());
            dados.setSite(usuario.getSite());
            modelo.addAttribute("formularioDados", dados);
        }
        if (!modelo.containsAttribute("formularioSenha")) {
            modelo.addAttribute("formularioSenha", new FormularioSenha());
        }
    }
}
