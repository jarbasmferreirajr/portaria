package br.com.portaria.evento;

import br.com.portaria.checkin.RepositorioDeCheckIns;
import br.com.portaria.inscricao.ServicoDeInscricoes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;

/**
 * Área do organizador: listar, criar e acompanhar eventos.
 */
@Controller
@RequestMapping("/eventos")
public class ControladorDeEventos {

    private final ServicoDeEventos eventos;
    private final ServicoDeInscricoes inscricoes;
    private final RepositorioDeCheckIns checkIns;

    public ControladorDeEventos(ServicoDeEventos eventos, ServicoDeInscricoes inscricoes,
                                RepositorioDeCheckIns checkIns) {
        this.eventos = eventos;
        this.inscricoes = inscricoes;
        this.checkIns = checkIns;
    }

    /** Dados do formulário de evento -- serve tanto para criar quanto para editar. */
    public static class FormularioDeEvento {

        @NotBlank(message = "Informe o nome do evento")
        @Size(max = 150)
        private String nome;

        @Size(max = 1000)
        private String descricao;

        @NotBlank(message = "Informe o local")
        @Size(max = 200)
        private String local;

        @NotNull(message = "Informe a data e hora de início")
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        private LocalDateTime inicio;

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        private LocalDateTime fim;

        @NotNull(message = "Informe a capacidade")
        @Min(value = 1, message = "A capacidade deve ser maior que zero")
        private Integer capacidade;

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public String getLocal() { return local; }
        public void setLocal(String local) { this.local = local; }
        public LocalDateTime getInicio() { return inicio; }
        public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
        public LocalDateTime getFim() { return fim; }
        public void setFim(LocalDateTime fim) { this.fim = fim; }
        public Integer getCapacidade() { return capacidade; }
        public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("eventos", eventos.listar());
        if (!modelo.containsAttribute("formulario")) {
            modelo.addAttribute("formulario", new FormularioDeEvento());
        }
        return "eventos/lista";
    }

    @PostMapping
    public String criar(@ModelAttribute("formulario") @Valid FormularioDeEvento formulario,
                        BindingResult erros, Model modelo) {
        if (erros.hasErrors()) {
            modelo.addAttribute("eventos", eventos.listar());
            return "eventos/lista";
        }
        Evento criado = eventos.criar(formulario.getNome(), formulario.getDescricao(),
                formulario.getLocal(), formulario.getInicio(), formulario.getFim(),
                formulario.getCapacidade());
        return "redirect:/eventos/" + criado.getId();
    }

    @GetMapping("/{id}")
    public String detalhar(@PathVariable Long id, Model modelo) {
        Evento evento = eventos.porId(id);
        modelo.addAttribute("evento", evento);
        modelo.addAttribute("inscricoes", inscricoes.listarDoEvento(id));
        modelo.addAttribute("totalInscritos", inscricoes.contarDoEvento(id));
        modelo.addAttribute("totalPresentes", checkIns.contarPorEvento(id));
        // O Thymeleaf 3.1 removeu o acesso à requisição dentro do template,
        // então a URL pública absoluta é montada aqui.
        modelo.addAttribute("linkPublico", ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/e/").path(evento.getIdentificadorPublico()).toUriString());
        return "eventos/detalhe";
    }

    // ---------- edição ----------

    @GetMapping("/{id}/editar")
    public String abrirEdicao(@PathVariable Long id, Model modelo) {
        Evento evento = eventos.porId(id);
        modelo.addAttribute("evento", evento);
        if (!modelo.containsAttribute("formulario")) {
            modelo.addAttribute("formulario", formularioDe(evento));
        }
        return "eventos/editar";
    }

    @PostMapping("/{id}/editar")
    public String salvarEdicao(@PathVariable Long id,
                               @ModelAttribute("formulario") @Valid FormularioDeEvento formulario,
                               BindingResult erros, Model modelo, RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            modelo.addAttribute("evento", eventos.porId(id));
            return "eventos/editar";
        }
        try {
            eventos.atualizar(id, formulario.getNome(), formulario.getDescricao(), formulario.getLocal(),
                    formulario.getInicio(), formulario.getFim(), formulario.getCapacidade());
        } catch (ServicoDeEventos.AlteracaoRecusada recusa) {
            modelo.addAttribute("evento", eventos.porId(id));
            modelo.addAttribute("erroEdicao", recusa.getMessage());
            return "eventos/editar";
        }
        atributos.addFlashAttribute("sucesso", "Evento atualizado.");
        return "redirect:/eventos/" + id;
    }

    // ---------- encerramento ----------

    /**
     * Encerra as inscrições.
     *
     * Sem mensagem de confirmação de propósito: o aviso permanente de estado,
     * que aparece na própria página, já diz que as inscrições estão encerradas
     * e o que isso implica. Somar um segundo aviso dizendo o mesmo faria a
     * pessoa ler duas vezes para descobrir que não havia nada de novo.
     */
    @PostMapping("/{id}/encerrar")
    public String encerrar(@PathVariable Long id) {
        eventos.encerrar(id);
        return "redirect:/eventos/" + id;
    }

    /** Reabrir mantém a confirmação: não há aviso permanente para o estado aberto. */
    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable Long id, RedirectAttributes atributos) {
        eventos.reabrir(id);
        atributos.addFlashAttribute("sucesso", "Inscrições reabertas.");
        return "redirect:/eventos/" + id;
    }

    private FormularioDeEvento formularioDe(Evento evento) {
        FormularioDeEvento formulario = new FormularioDeEvento();
        formulario.setNome(evento.getNome());
        formulario.setDescricao(evento.getDescricao());
        formulario.setLocal(evento.getLocal());
        formulario.setInicio(evento.getInicio());
        formulario.setFim(evento.getFim());
        formulario.setCapacidade(evento.getCapacidade());
        return formulario;
    }
}
