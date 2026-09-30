package br.com.portaria.usuario;

/**
 * Os dados que o organizador pode alterar sobre si mesmo.
 *
 * Existe como agrupamento para que a assinatura do serviço não vire uma fila de
 * cinco Strings soltas -- onde trocar telefone por endereço na chamada compila
 * sem erro e falha em silêncio.
 */
public record DadosDoOrganizador(String nome, String email, String telefone,
                                 String endereco, String site) {
}
