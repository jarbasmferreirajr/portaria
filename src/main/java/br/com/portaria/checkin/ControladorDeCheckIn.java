package br.com.portaria.checkin;

import br.com.portaria.evento.Evento;
import br.com.portaria.evento.ServicoDeEventos;
import br.com.portaria.inscricao.ServicoDeInscricoes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Painel de portaria: a tela que o operador usa com a fila andando.
 */
@Controller
@RequestMapping("/eventos/{id}/checkin")
public class ControladorDeCheckIn {

    private final ServicoDeEventos eventos;
    private final ServicoDeInscricoes inscricoes;
    private final ServicoDeCheckIn servicoDeCheckIn;
    private final RepositorioDeCheckIns checkIns;

    public ControladorDeCheckIn(ServicoDeEventos eventos, ServicoDeInscricoes inscricoes,
                                ServicoDeCheckIn servicoDeCheckIn, RepositorioDeCheckIns checkIns) {
        this.eventos = eventos;
        this.inscricoes = inscricoes;
        this.servicoDeCheckIn = servicoDeCheckIn;
        this.checkIns = checkIns;
    }

    @GetMapping
    public String painel(@PathVariable Long id, Model modelo) {
        preencherPainel(id, modelo);
        return "checkin/painel";
    }

    /**
     * Recebe o código digitado (ou tecladeado por um leitor USB, que se comporta
     * como teclado) e devolve APENAS o fragmento do painel -- o htmx troca esse
     * pedaço da página sem recarregar, mantendo o operador no mesmo lugar.
     *
     * Se o JavaScript estiver desligado, o mesmo endereço responde a um POST
     * comum e o navegador simplesmente renderiza o fragmento. A portaria nunca
     * para de funcionar por causa do front-end.
     */
    @PostMapping
    public String registrar(@PathVariable Long id,
                            @RequestParam("codigo") String codigo,
                            Principal operadorAutenticado,
                            Model modelo) {
        String operador = operadorAutenticado != null ? operadorAutenticado.getName() : "desconhecido";
        ResultadoCheckIn resultado = servicoDeCheckIn.registrarPorCodigo(codigo, id, operador);

        modelo.addAttribute("resultado", resultado);
        modelo.addAttribute("codigoDigitado", codigo);
        preencherPainel(id, modelo);
        return "checkin/painel :: painel";
    }

    private void preencherPainel(Long id, Model modelo) {
        Evento evento = eventos.porId(id);
        modelo.addAttribute("evento", evento);
        modelo.addAttribute("presentes", checkIns.listarPorEvento(id));
        modelo.addAttribute("totalPresentes", checkIns.contarPorEvento(id));
        modelo.addAttribute("totalInscritos", inscricoes.contarDoEvento(id));
    }
}
