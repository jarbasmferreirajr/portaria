package br.com.portaria.configuracao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Configuração de autenticação e de acesso às rotas.
 */
@Configuration
public class ConfiguracaoSeguranca {

    // A partir da Sprint 3 os usuários vivem no banco: quem responde ao Spring
    // Security é ServicoDeDetalhesDoUsuario, descoberto automaticamente por ser
    // o único bean de UserDetailsService do contexto. O usuário fixo em memória
    // saiu daqui porque não se altera nem se recupera a senha de quem não é
    // persistido.

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