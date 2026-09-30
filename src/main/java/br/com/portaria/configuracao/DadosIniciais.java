package br.com.portaria.configuracao;

import br.com.portaria.evento.Evento;
import br.com.portaria.evento.RepositorioDeEventos;
import br.com.portaria.evento.ServicoDeEventos;
import br.com.portaria.inscricao.ServicoDeInscricoes;
import br.com.portaria.usuario.RepositorioDeUsuarios;
import br.com.portaria.usuario.ServicoDeUsuarios;
import br.com.portaria.usuario.Usuario;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Semeia um evento de exemplo na primeira execução, para que a aplicação abra
 * já com algo demonstrável. Não roda no perfil de teste.
 *
 * O método run() vem da interface CommandLineRunner do Spring -- nome reservado,
 * não traduzível.
 */
@Component
@Profile("!test")
public class DadosIniciais implements CommandLineRunner {

    /** Conta inicial, para que exista alguém capaz de entrar na primeira execução. */
    private static final String EMAIL_ADMINISTRADOR = "admin@portaria.local";
    private static final String SENHA_ADMINISTRADOR = "portaria123";

    private final RepositorioDeEventos repositorioDeEventos;
    private final ServicoDeEventos eventos;
    private final ServicoDeInscricoes inscricoes;
    private final RepositorioDeUsuarios repositorioDeUsuarios;
    private final ServicoDeUsuarios usuarios;

    public DadosIniciais(RepositorioDeEventos repositorioDeEventos, ServicoDeEventos eventos,
                         ServicoDeInscricoes inscricoes, RepositorioDeUsuarios repositorioDeUsuarios,
                         ServicoDeUsuarios usuarios) {
        this.repositorioDeEventos = repositorioDeEventos;
        this.eventos = eventos;
        this.inscricoes = inscricoes;
        this.repositorioDeUsuarios = repositorioDeUsuarios;
        this.usuarios = usuarios;
    }

    /**
     * Cria o administrador inicial se ainda não houver nenhuma conta.
     *
     * Roda fora da checagem de eventos de propósito: um banco que já tem
     * eventos, mas nenhum usuário, continua precisando de alguém para entrar.
     */
    private void semearAdministrador() {
        if (repositorioDeUsuarios.count() > 0) {
            return;
        }
        usuarios.criar("Administrador", EMAIL_ADMINISTRADOR, SENHA_ADMINISTRADOR,
                Usuario.PAPEL_ORGANIZADOR);
    }

    @Override
    public void run(String... argumentos) {
        semearAdministrador();

        if (repositorioDeEventos.count() > 0) {
            return;
        }

        LocalDateTime sabado = LocalDateTime.now().plusDays(12).with(LocalTime.of(10, 0));

        Evento encontro = eventos.criar(
                "3º Encontro de Motociclistas de Gaspar",
                "Encontro regional com exposição de motos, praça de alimentação e shows de rock ao vivo a partir das 18h.",
                "Parque de Eventos — Gaspar/SC",
                sabado,
                sabado.plusHours(14),
                300
        );

        inscricoes.inscrever(encontro, "Ana Paula Vieira", "ana.vieira@exemplo.com",
                "(47) 99999-1010", "Lobos de Aço MC", "Blumenau");
        inscricoes.inscrever(encontro, "Carlos Eduardo Reis", "carlos.reis@exemplo.com",
                "(47) 99999-2020", "Lobos de Aço MC", "Blumenau");
        inscricoes.inscrever(encontro, "Marina Duarte", "marina.duarte@exemplo.com",
                "(48) 98888-3030", "Estrada Livre MC", "Florianópolis");
        inscricoes.inscrever(encontro, "Roberto Nunes", "roberto.nunes@exemplo.com",
                "(47) 97777-4040", null, "Gaspar");
    }
}
