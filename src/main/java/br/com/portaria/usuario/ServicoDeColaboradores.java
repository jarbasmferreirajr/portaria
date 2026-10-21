package br.com.portaria.usuario;

import br.com.portaria.evento.Evento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Quem trabalha no dia do evento.
 *
 * Cada colaborador tem CONTA PRÓPRIA. A alternativa -- um login de portaria
 * compartilhado pela equipe -- pareceria mais simples, mas destruiria a coluna
 * registrado_por da tabela check_in: ela continuaria preenchida, só que com um
 * nome que não corresponde a ninguém. Um dado errado é pior que um dado ausente,
 * porque ninguém desconfia dele.
 */
@Service
public class ServicoDeColaboradores {

    private final RepositorioDeColaboradores colaboradores;
    private final RepositorioDeUsuarios usuarios;
    private final ServicoDeUsuarios servicoDeUsuarios;
    private final ServicoDeRecuperacaoDeSenha convites;

    public ServicoDeColaboradores(RepositorioDeColaboradores colaboradores,
                                  RepositorioDeUsuarios usuarios,
                                  ServicoDeUsuarios servicoDeUsuarios,
                                  ServicoDeRecuperacaoDeSenha convites) {
        this.colaboradores = colaboradores;
        this.usuarios = usuarios;
        this.servicoDeUsuarios = servicoDeUsuarios;
        this.convites = convites;
    }

    /** Recusa por regra de negócio, não por falha técnica. */
    public static class InclusaoRecusada extends RuntimeException {
        public InclusaoRecusada(String mensagem) {
            super(mensagem);
        }
    }

    /** O que aconteceu, para a tela saber o que dizer ao organizador. */
    public enum Resultado {
        /** Pessoa nova: conta criada e convite enviado para ela definir a senha. */
        CONVIDADO,
        /** Já tinha conta no sistema: apenas vinculada ao evento. */
        VINCULADO
    }

    @Transactional(readOnly = true)
    public List<ColaboradorDoEvento> listar(Long eventoId) {
        return colaboradores.listarPorEvento(eventoId);
    }

    /**
     * Inclui alguém como colaborador do evento.
     *
     * Se a pessoa já tem conta -- porque organiza outro evento, ou já colaborou
     * antes -- o vínculo é só criado. Não se cria uma segunda conta para o mesmo
     * e-mail, nem se reenvia convite para quem já tem senha.
     */
    @Transactional
    public Resultado incluir(Evento evento, String nome, String email, String funcao, String urlBase) {
        String normalizado = Usuario.normalizarEmail(email);
        Optional<Usuario> existente = usuarios.findByEmail(normalizado);

        Usuario pessoa;
        Resultado resultado;

        if (existente.isPresent()) {
            pessoa = existente.get();
            if (colaboradores.existsByEventoIdAndUsuarioId(evento.getId(), pessoa.getId())) {
                throw new InclusaoRecusada(pessoa.getNome() + " já é colaborador deste evento.");
            }
            // A conta existir não significa que a pessoa consiga entrar: pode
            // ter sido convidada antes, nunca ter aberto o link e ter sido
            // removida. Nesse caso ela precisa de convite de novo.
            resultado = pessoa.isAcessoDefinido() ? Resultado.VINCULADO : Resultado.CONVIDADO;
        } else {
            pessoa = servicoDeUsuarios.criarAguardandoConvite(nome, normalizado,
                    Usuario.PAPEL_COLABORADOR);
            resultado = Resultado.CONVIDADO;
        }

        colaboradores.save(new ColaboradorDoEvento(evento, pessoa, funcao));

        if (resultado == Resultado.CONVIDADO) {
            convites.convidar(pessoa, evento.getNome(), urlBase);
        }
        return resultado;
    }

    /**
     * Reenvia o convite. Serve para quem perdeu o e-mail, deixou o link expirar
     * ou simplesmente nunca abriu -- sem precisar remover e incluir de novo.
     */
    @Transactional
    public String reenviarConvite(Long eventoId, Long colaboradorId, String urlBase) {
        ColaboradorDoEvento vinculo = colaboradores.findById(colaboradorId)
                .filter(c -> c.getEvento().getId().equals(eventoId))
                .orElseThrow(() -> new InclusaoRecusada("Colaborador não encontrado neste evento."));

        Usuario pessoa = vinculo.getUsuario();
        if (pessoa.isAcessoDefinido()) {
            throw new InclusaoRecusada(pessoa.getNome()
                    + " já definiu a senha e consegue entrar. Se esqueceu, use \"Esqueci minha senha\".");
        }

        convites.convidar(pessoa, vinculo.getEvento().getNome(), urlBase);
        return pessoa.getEmail();
    }

    @Transactional
    public void alterarFuncao(Long colaboradorId, String funcao) {
        ColaboradorDoEvento vinculo = colaboradores.findById(colaboradorId)
                .orElseThrow(() -> new InclusaoRecusada("Colaborador não encontrado."));
        vinculo.alterarFuncao(funcao);
        colaboradores.save(vinculo);
    }

    /**
     * Remove o vínculo com o evento. A CONTA permanece, e os check-ins que a
     * pessoa registrou continuam atribuídos a ela -- apagar o histórico de quem
     * saiu da equipe inutilizaria o próprio registro de presença.
     */
    @Transactional
    public void remover(Long eventoId, Long colaboradorId) {
        colaboradores.findById(colaboradorId)
                .filter(vinculo -> vinculo.getEvento().getId().equals(eventoId))
                .ifPresent(colaboradores::delete);
    }

    @Transactional(readOnly = true)
    public boolean podeOperar(Long eventoId, String email) {
        return colaboradores.temVinculo(eventoId, Usuario.normalizarEmail(email));
    }

}
