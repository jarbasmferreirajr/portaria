package br.com.portaria.inscricao;

import br.com.portaria.evento.Evento;
import br.com.portaria.evento.ServicoDeEventos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Área pública: o participante não faz login para se inscrever.
 */
@Controller
@RequestMapping("/e")
public class ControladorInscricaoPublica {

    private final ServicoDeEventos eventos;
    private final ServicoDeInscricoes inscricoes;

    public ControladorInscricaoPublica(ServicoDeEventos eventos, ServicoDeInscricoes inscricoes) {
        this.eventos = eventos;
        this.inscricoes = inscricoes;
    }

    /** Dados que o participante preenche, com as regras de validação. */
    public static class FormularioInscricao {

        @NotBlank(message = "Informe seu nome")
        @Size(max = 150)
        private String nome;

        @NotBlank(message = "Informe seu e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @Size(max = 30)
        private String telefone;

        @Size(max = 120)
        private String motoclube;

        @Size(max = 120)
        private String cidade;

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTelefone() { return telefone; }
        public void setTelefone(String telefone) { this.telefone = telefone; }
        public String getMotoclube() { return motoclube; }
        public void setMotoclube(String motoclube) { this.motoclube = motoclube; }
        public String getCidade() { return cidade; }
        public void setCidade(String cidade) { this.cidade = cidade; }
    }

    @GetMapping("/{identificador}")
    public String formulario(@PathVariable String identificador, Model modelo) {
        Evento evento = eventos.porIdentificadorPublico(identificador);
        prepararPagina(evento, modelo);
        if (!modelo.containsAttribute("formulario")) {
            modelo.addAttribute("formulario", new FormularioInscricao());
        }
        return "publico/inscricao";
    }

    @PostMapping("/{identificador}")
    public String inscrever(@PathVariable String identificador,
                            @ModelAttribute("formulario") @Valid FormularioInscricao formulario,
                            BindingResult erros, Model modelo) {
        Evento evento = eventos.porIdentificadorPublico(identificador);
        prepararPagina(evento, modelo);

        if (erros.hasErrors()) {
            return "publico/inscricao";
        }

        try {
            Inscricao inscricao = inscricoes.inscrever(evento, formulario.getNome(),
                    formulario.getEmail(), formulario.getTelefone(),
                    formulario.getMotoclube(), formulario.getCidade());
            return "redirect:/e/" + identificador + "/confirmado/" + inscricao.getCodigo();
        } catch (ServicoDeInscricoes.InscricaoRecusada recusa) {
            modelo.addAttribute("erroInscricao", recusa.getMessage());
            return "publico/inscricao";
        }
    }

    @GetMapping("/{identificador}/confirmado/{codigo}")
    public String confirmado(@PathVariable String identificador, @PathVariable String codigo,
                             Model modelo) {
        modelo.addAttribute("evento", eventos.porIdentificadorPublico(identificador));
        modelo.addAttribute("inscricao", inscricoes.porCodigo(codigo));
        return "publico/confirmado";
    }

    private void prepararPagina(Evento evento, Model modelo) {
        modelo.addAttribute("evento", evento);
        modelo.addAttribute("vagasRestantes",
                evento.getCapacidade() - inscricoes.contarDoEvento(evento.getId()));
    }
}
