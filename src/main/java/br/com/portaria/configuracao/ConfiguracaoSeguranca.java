package br.com.portaria.configuracao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Configuração de autenticação e de acesso às rotas.
 */
@Configuration
public class ConfiguracaoSeguranca {

    /**
     * Um único usuário mantido em memória: suficiente para a primeira entrega.
     * Cadastro de usuários, papéis e recuperação de senha estão no backlog --
     * não agregariam nada à demonstração do fluxo de inscrição e check-in.
     */
    @Bean
    public UserDetailsService servicoDeUsuarios(PasswordEncoder codificador) {
        UserDetails organizador = User.withUsername("admin")
                .password(codificador.encode("portaria123"))
                .roles("ORGANIZADOR")
                .build();
        return new InMemoryUserDetailsManager(organizador);
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadeiaDeFiltros(HttpSecurity http) throws Exception {
        // Substitui AntPathRequestMatcher, removido no Spring Security 7.
        // O builder resolve os padrões contra o servlet padrão da aplicação.
        PathPatternRequestMatcher.Builder caminho = PathPatternRequestMatcher.withDefaults();

        http
            .authorizeHttpRequests(rotas -> rotas
                // Área pública: inscrição do participante e recursos estáticos.
                // /webjars/** precisa entrar aqui porque a própria tela de login
                // carrega o htmx dali; um 401 nesse script faz o navegador abrir
                // a caixa nativa de autenticação por cima da página.
                .requestMatchers("/e/**", "/login", "/css/**", "/js/**",
                                 "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers(caminho.matcher("/h2-console/**")).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(formulario -> formulario
                .loginPage("/login")
                .defaultSuccessUrl("/eventos", true)
                .permitAll()
            )
            .logout(saida -> saida
                .logoutRequestMatcher(caminho.matcher("/logout"))
                .logoutSuccessUrl("/login?saiu")
                .permitAll()
            )
            // O console do H2 roda dentro de um frame e não envia token CSRF.
            // Liberado apenas em desenvolvimento; sai antes de qualquer implantação real.
            .csrf(csrf -> csrf.ignoringRequestMatchers(caminho.matcher("/h2-console/**")))
            .headers(cabecalhos -> cabecalhos.frameOptions(frame -> frame.sameOrigin()));
        // Sem httpBasic: com ele configurado, qualquer 401 vira um desafio
        // WWW-Authenticate e o navegador abre a caixa nativa de login, em vez
        // de levar a pessoa ao formulário da aplicação.
        return http.build();
    }
}