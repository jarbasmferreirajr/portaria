package br.com.portaria.checkin;

import br.com.portaria.inscricao.Inscricao;
import br.com.portaria.inscricao.RepositorioDeInscricoes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Regra de negócio do check-in presencial.
 */
@Service
public class ServicoDeCheckIn {

    /**
     * Caracteres que o gerador de códigos nunca produz (ver GeradorDeCodigo, que
     * exclui 0/O e 1/I/L de propósito). Se um deles aparece no que foi digitado,
     * é leitura errada com certeza -- e dizer isso é mais útil ao operador que
     * um "código não encontrado" genérico, porque aponta onde procurar o erro.
     */
    private static final String IMPOSSIVEIS = "01OIL";

    private final RepositorioDeInscricoes inscricoes;
    private final RepositorioDeCheckIns checkIns;
    private final RegistroDePresenca registro;

    public ServicoDeCheckIn(RepositorioDeInscricoes inscricoes, RepositorioDeCheckIns checkIns,
                            RegistroDePresenca registro) {
        this.inscricoes = inscricoes;
        this.checkIns = checkIns;
        this.registro = registro;
    }

    /**
     * Registra a presença a partir do código apresentado na portaria.
     *
     * A operação é IDEMPOTENTE: chamar duas vezes com o mesmo código não cria
     * duas presenças. A garantia vem em duas camadas -- a consulta prévia cobre
     * o caso comum, e a restrição UNIQUE do banco cobre a corrida entre dois
     * operadores escaneando ao mesmo tempo em portões diferentes. Confiar apenas
     * na consulta prévia seria um check-then-act, vulnerável à concorrência.
     *
     * O identificador do evento é exigido de propósito: um código válido de
     * OUTRO evento, para o operador deste portão, é o mesmo que código inexistente.
     */
    public ResultadoCheckIn registrarPorCodigo(String codigo, Long eventoId, String operador) {
        String normalizado = normalizar(codigo);
        Optional<Inscricao> encontrada = inscricoes.buscarPorCodigoComEvento(normalizado);

        if (encontrada.isEmpty() || !encontrada.get().getEvento().getId().equals(eventoId)) {
            return ResultadoCheckIn.naoEncontrado(dicaDeLeitura(normalizado));
        }

        Inscricao inscricao = encontrada.get();

        Optional<CheckIn> jaRegistrado = checkIns.findByInscricaoId(inscricao.getId());
        if (jaRegistrado.isPresent()) {
            return ResultadoCheckIn.jaPresente(inscricao, jaRegistrado.get().getRegistradoEm());
        }

        try {
            CheckIn novo = registro.gravar(inscricao, operador);
            return ResultadoCheckIn.liberado(inscricao, novo.getRegistradoEm());
        } catch (DataIntegrityViolationException corrida) {
            // Outro operador registrou a mesma inscrição entre a consulta e a gravação.
            LocalDateTime momento = checkIns.findByInscricaoId(inscricao.getId())
                    .map(CheckIn::getRegistradoEm)
                    .orElse(LocalDateTime.now());
            return ResultadoCheckIn.jaPresente(inscricao, momento);
        }
    }

    /** Aceita o código como o operador digitar: minúsculo, com espaços ou hifens. */
    private String normalizar(String codigo) {
        if (codigo == null) {
            return "";
        }
        return codigo.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Explica o provável erro de leitura, quando dá para saber qual é.
     * Devolve null quando não há nada de útil a dizer -- e aí a tela mostra
     * apenas que o código não foi encontrado.
     */
    private String dicaDeLeitura(String codigo) {
        StringBuilder achados = new StringBuilder();
        for (char caractere : IMPOSSIVEIS.toCharArray()) {
            if (codigo.indexOf(caractere) >= 0) {
                if (!achados.isEmpty()) {
                    achados.append(", ");
                }
                achados.append(caractere);
            }
        }
        if (achados.isEmpty()) {
            return null;
        }
        return "Os códigos gerados nunca usam 0, 1, O, I nem L, e este tem "
                + achados + ". Confira se não houve troca na leitura.";
    }
}
