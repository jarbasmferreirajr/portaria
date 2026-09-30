package br.com.portaria.usuario;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liga o Spring Security à tabela de usuários.
 *
 * Substitui o InMemoryUserDetailsManager que atendia a primeira entrega. O nome
 * loadUserByUsername vem da interface UserDetailsService -- vocabulário
 * reservado do framework, por isso permanece em inglês.
 *
 * O "username" do Spring Security aqui é o e-mail: é o dado que a pessoa
 * digita no login e o mesmo que a recuperação de senha usa para achar a conta.
 */
@Service
public class ServicoDeDetalhesDoUsuario implements UserDetailsService {

    private final RepositorioDeUsuarios usuarios;

    public ServicoDeDetalhesDoUsuario(RepositorioDeUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .roles(usuario.getPapel())
                .disabled(!usuario.isAtivo())
                .build();
    }
}
