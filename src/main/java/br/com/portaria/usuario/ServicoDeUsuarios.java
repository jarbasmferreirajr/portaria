package br.com.portaria.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra de negócio das contas de usuário.
 *
 * É o único lugar que conhece o codificador de senha: a entidade guarda apenas
 * o hash já pronto, e nenhuma senha em texto puro atravessa a camada de dados.
 */
@Service
public class ServicoDeUsuarios {

    private final RepositorioDeUsuarios usuarios;
    private final PasswordEncoder codificador;

    public ServicoDeUsuarios(RepositorioDeUsuarios usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    /** Operação recusada por uma regra de negócio, não por falha técnica. */
    public static class OperacaoRecusada extends RuntimeException {
        public OperacaoRecusada(String mensagem) {
            super(mensagem);
        }
    }

    @Transactional
    public Usuario criar(String nome, String email, String senha, String papel) {
        String normalizado = Usuario.normalizarEmail(email);
        if (usuarios.existsByEmail(normalizado)) {
            throw new OperacaoRecusada("Já existe uma conta com este e-mail.");
        }
        return usuarios.save(new Usuario(nome, normalizado, codificador.encode(senha), papel));
    }

    @Transactional(readOnly = true)
    public Usuario porEmail(String email) {
        return usuarios.findByEmail(Usuario.normalizarEmail(email))
                .orElseThrow(() -> new OperacaoRecusada("Usuário não encontrado."));
    }

    @Transactional
    public Usuario alterarDados(String emailAtual, DadosDoOrganizador dados) {
        Usuario usuario = porEmail(emailAtual);
        String normalizado = Usuario.normalizarEmail(dados.email());

        // Só é conflito se o e-mail já pertence a OUTRA conta; manter o próprio
        // e-mail ao alterar apenas o nome precisa continuar sendo permitido.
        boolean mudouDeEmail = !normalizado.equals(usuario.getEmail());
        if (mudouDeEmail && usuarios.existsByEmail(normalizado)) {
            throw new OperacaoRecusada("Já existe uma conta com este e-mail.");
        }

        usuario.alterarDados(dados.nome(), normalizado, dados.telefone(),
                dados.endereco(), dados.site());
        return usuarios.save(usuario);
    }

    /**
     * Troca a senha exigindo a senha atual.
     *
     * A confirmação não é burocracia: sem ela, quem encontrasse uma sessão
     * aberta poderia assumir a conta trocando a senha sem nunca tê-la sabido.
     */
    @Transactional
    public void trocarSenha(String email, String senhaAtual, String novaSenha) {
        Usuario usuario = porEmail(email);

        if (!codificador.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new OperacaoRecusada("A senha atual está incorreta.");
        }
        if (codificador.matches(novaSenha, usuario.getSenhaHash())) {
            throw new OperacaoRecusada("A nova senha deve ser diferente da atual.");
        }

        usuario.trocarSenha(codificador.encode(novaSenha));
        usuarios.save(usuario);
    }

    /** Usada pela recuperação de senha, que já provou a identidade pelo token. */
    @Transactional
    public void definirSenhaSemConferirAtual(Usuario usuario, String novaSenha) {
        usuario.trocarSenha(codificador.encode(novaSenha));
        usuarios.save(usuario);
    }
}
