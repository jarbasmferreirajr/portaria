package br.com.portaria.configuracao;

import org.springframework.beans.factory.annotation.Value;
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

    /**
     * As exceções do console do H2 são amarradas à propriedade que liga o
     * console, e não a um nome de perfil.
     *
     * A diferença importa: amarrar ao perfil deixaria as regras valendo em
     * qualquer ambiente que por descuido subisse com o perfil de
     * desenvolvimento, mesmo sem console nenhum. Amarrando à propriedade, as
     * exceções existem exatamente enquanto existe aquilo que as justifica --
     * e no perfil do PostgreSQL, onde o console não sobe, o sistema roda com
     * CSRF íntegro e X-Frame-Options em DENY.
     */
    @Bean
    public SecurityFilterChain cadeiaDeFiltros(
            HttpSecurity http,
            @Value("${spring.h2.console.enabled:false}") boolean consoleH2Ativo) throws Exception {

        // Substitui AntPathRequestMatcher, removido no Spring Security 7.
        // O builder resolve os padrões contra o servlet padrão da aplicação.
        PathPatternRequestMatcher.Builder caminho = PathPatternRequestMatcher.withDefaults();

        http
            .authorizeHttpRequests(rotas -> {
                // Área pública: inscrição do participante e recursos estáticos.
                // /webjars/** precisa entrar aqui porque a própria tela de login
                // carrega o htmx dali; um 401 nesse script faz o navegador abrir
                // a caixa nativa de autenticação por cima da página.
                // Quem esqueceu a senha não consegue autenticar-se: as duas
                // rotas da recuperação precisam ser públicas por definição.
                rotas.requestMatchers("/e/**", "/login", "/recuperar-senha", "/redefinir-senha",
                                      "/css/**", "/js/**", "/fontes/**", "/webjars/**",
                                      "/favicon.ico", "/favicon.svg").permitAll();

                if (consoleH2Ativo) {
                    rotas.requestMatchers(caminho.matcher("/h2-console/**")).permitAll();
                }

                // A lista de eventos atende os dois papéis, com conteúdos
                // diferentes: o organizador vê os seus, o colaborador vê
                // aqueles em que trabalha.
                rotas.requestMatchers(caminho.matcher("/eventos")).authenticated();

                // O painel de portaria é o que o colaborador vem fazer aqui.
                // A regra de ROTA garante o papel; a de EVENTO -- se esta pessoa
                // trabalha NESTE evento -- é verificada no controlador, porque
                // depende de consultar o vínculo no banco.
                rotas.requestMatchers(caminho.matcher("/eventos/*/checkin/**"))
                        .hasAnyRole("ORGANIZADOR", "COLABORADOR");

                // Todo o resto da área de eventos é administração.
                rotas.requestMatchers(caminho.matcher("/eventos/**")).hasRole("ORGANIZADOR");

                rotas.anyRequest().authenticated();
            })
            .formLogin(formulario -> formulario
                .loginPage("/login")
                .defaultSuccessUrl("/eventos", true)
                .permitAll()
            )
            .logout(saida -> saida
                .logoutRequestMatcher(caminho.matcher("/logout"))
                .logoutSuccessUrl("/login?saiu")
                .permitAll()
            );

        if (consoleH2Ativo) {
            // O console do H2 roda dentro de um frame e não envia token CSRF.
            // As duas exceções existem só por causa dele, e caem junto com ele.
            http.csrf(csrf -> csrf.ignoringRequestMatchers(caminho.matcher("/h2-console/**")))
                .headers(cabecalhos -> cabecalhos.frameOptions(frame -> frame.sameOrigin()));
        }

        // Sem httpBasic: com ele configurado, qualquer 401 vira um desafio
        // WWW-Authenticate e o navegador abre a caixa nativa de login, em vez
        // de levar a pessoa ao formulário da aplicação.
        return http.build();
    }
}
