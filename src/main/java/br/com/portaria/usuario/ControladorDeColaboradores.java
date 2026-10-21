package br.com.portaria.usuario;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Equipe de um evento: quem trabalha no dia.
 */
@Controller
@RequestMapping("/eventos/{eventoId}/colaboradores")
public class ControladorDeColaboradores {

    private static final String PAGINA = "eventos/colaboradores";

    private final ServicoDeEventos eventos;
    private final ServicoDeColaboradores colaboradores;

    public ControladorDeColaboradores(ServicoDeEventos eventos, ServicoDeColaboradores colaboradores) {
        this.eventos = eventos;
        this.colaboradores = colaboradores;
    }

    public static class FormularioColaborador {

        @NotBlank(message = "Informe o nome")
        @Size(max = 150)
        private String nome;

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @Size(max = 60)
        private String funcao;

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFuncao() { return funcao; }
        public void setFuncao(String funcao) { this.funcao = funcao; }
    }

    @GetMapping
    public String listar(@PathVariable Long eventoId, Model modelo) {
        preencher(eventoId, modelo);
        return PAGINA;
    }

    @PostMapping
    public String incluir(@PathVariable Long eventoId,
                          @ModelAttribute("formulario") @Valid FormularioColaborador formulario,
                          BindingResult erros, Model modelo, RedirectAttributes atributos) {

        if (erros.hasErrors()) {
            preencher(eventoId, modelo);
            return PAGINA;
        }

        Evento evento = eventos.porId(eventoId);
        String urlBase = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();

        try {
            ServicoDeColaboradores.Resultado resultado = colaboradores.incluir(
                    evento, formulario.getNome(), formulario.getEmail(),
                    formulario.getFuncao(), urlBase);

            atributos.addFlashAttribute("sucesso",
                    resultado == ServicoDeColaboradores.Resultado.CONVIDADO
                            ? "Convite enviado para " + formulario.getEmail()
                              + ". A pessoa define a própria senha pelo link."
                            : formulario.getEmail() + " já tinha conta e foi vinculado ao evento.");

        } catch (ServicoDeColaboradores.InclusaoRecusada recusa) {
            modelo.addAttribute("erroInclusao", recusa.getMessage());
            preencher(eventoId, modelo);
            return PAGINA;
        }

        return "redirect:/eventos/" + eventoId + "/colaboradores";
    }

    @PostMapping("/{colaboradorId}/reenviar-convite")
    public String reenviarConvite(@PathVariable Long eventoId, @PathVariable Long colaboradorId,
                                  RedirectAttributes atributos) {
        String urlBase = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        try {
            String email = colaboradores.reenviarConvite(eventoId, colaboradorId, urlBase);
            atributos.addFlashAttribute("sucesso", "Convite reenviado para " + email + ".");
        } catch (ServicoDeColaboradores.InclusaoRecusada recusa) {
            atributos.addFlashAttribute("erroFlash", recusa.getMessage());
        }
        return "redirect:/eventos/" + eventoId + "/colaboradores";
    }

    @PostMapping("/{colaboradorId}/remover")
    public String remover(@PathVariable Long eventoId, @PathVariable Long colaboradorId,
                          RedirectAttributes atributos) {
        colaboradores.remover(eventoId, colaboradorId);
        atributos.addFlashAttribute("sucesso",
                "Colaborador removido do evento. A conta e os check-ins registrados por ele permanecem.");
        return "redirect:/eventos/" + eventoId + "/colaboradores";
    }

    private void preencher(Long eventoId, Model modelo) {
        modelo.addAttribute("evento", eventos.porId(eventoId));
        modelo.addAttribute("colaboradores", colaboradores.listar(eventoId));
        if (!modelo.containsAttribute("formulario")) {
            modelo.addAttribute("formulario", new FormularioColaborador());
        }
    }
}
