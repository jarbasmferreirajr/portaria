package br.com.portaria.usuario;

/**
 * Entrega de mensagens para fora do sistema.
 *
 * A interface existe porque a entrega é a ÚNICA etapa da recuperação de senha
 * que depende de infraestrutura externa. Isolá-la permite que o mesmo fluxo
 * rode sem rede nenhuma durante o desenvolvimento e a apresentação, e contra um
 * servidor real em produção -- trocando a implementação, não o código do
 * serviço que a usa.
 */
public interface EnviadorDeEmail {

    void enviar(String destinatario, String assunto, String corpo);
}
