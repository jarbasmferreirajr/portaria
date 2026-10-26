package br.com.portaria.email;

/**
 * Entrega de mensagens para fora do sistema.
 *
 * Vive em pacote próprio porque não pertence a nenhum domínio: a recuperação
 * de senha foi só a primeira a precisar dela, e hoje a inscrição também envia
 * o comprovante. Deixá-la em 'usuario' faria 'inscricao' depender de 'usuario'
 * e fecharia um ciclo com 'evento'.
 *
 * A interface existe porque a entrega é a ÚNICA etapa desses fluxos que
 * depende de infraestrutura externa. Isolá-la permite que o mesmo fluxo
 * rode sem rede nenhuma durante o desenvolvimento e a apresentação, e contra um
 * servidor real em produção -- trocando a implementação, não o código do
 * serviço que a usa.
 */
public interface EnviadorDeEmail {

    void enviar(String destinatario, String assunto, String corpo);
}
