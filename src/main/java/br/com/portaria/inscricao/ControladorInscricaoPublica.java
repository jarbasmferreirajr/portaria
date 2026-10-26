package br.com.portaria.inscricao;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.portaria.evento.Evento;
import br.com.portaria.evento.ServicoDeEventos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Área pública: o participante não faz login para se inscrever.
 */
@Controller
@RequestMapping("/e/{identificador}")
public class ControladorInscricaoPublica {

    /** Todas as saídas do fluxo de inscrição voltam para esta mesma página. */
    private static final String PAGINA_INSCRICAO = "publico/inscricao";

    private final ServicoDeEventos eventos;
    private final ServicoDeInscricoes inscricoes;
    private final ComprovanteDeInscricao comprovante;

    public ControladorInscricaoPublica(ServicoDeEventos eventos, ServicoDeInscricoes inscricoes,
                                       ComprovanteDeInscricao comprovante) {
        this.eventos = eventos;
        this.inscricoes = inscricoes;
        this.comprovante = comprovante;
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

    @GetMapping
    public String formulario(@PathVariable String identificador, Model modelo) {
        Evento evento = eventos.porIdentificadorPublico(identificador);
        prepararPagina(evento, modelo);
        if (!modelo.containsAttribute("formulario")) {
            modelo.addAttribute("formulario", new FormularioInscricao());
        }
        return PAGINA_INSCRICAO;
    }

    @PostMapping
    public String inscrever(@PathVariable String identificador,
                            @ModelAttribute("formulario") @Valid FormularioInscricao formulario,
                            BindingResult erros, Model modelo) {
        Evento evento = eventos.porIdentificadorPublico(identificador);
        prepararPagina(evento, modelo);

        if (erros.hasErrors()) {
            return PAGINA_INSCRICAO;
        }

        try {
            Inscricao inscricao = inscricoes.inscrever(evento, formulario.getNome(),
                    formulario.getEmail(), formulario.getTelefone(),
                    formulario.getMotoclube(), formulario.getCidade());

            String caminho = "/e/" + identificador + "/confirmado/" + inscricao.getCodigo();

            // Depois de gravada, nunca dentro da transação: ver ComprovanteDeInscricao.
            comprovante.enviar(inscricao, evento, ServletUriComponentsBuilder
                    .fromCurrentContextPath().path(caminho).toUriString());

            return "redirect:" + caminho;
        } catch (ServicoDeInscricoes.InscricaoRecusada recusa) {
            modelo.addAttribute("erroInscricao", recusa.getMessage());
            return PAGINA_INSCRICAO;
        }
    }

    @GetMapping("/confirmado/{codigo}")
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