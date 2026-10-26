/* Tela de detalhe do evento: busca nos inscritos e cópia do link público.
 *
 * As duas coisas acontecem no navegador de propósito. A lista de inscritos já
 * está inteira na página -- pedir ao servidor que filtre de novo um dado que o
 * navegador já tem seria uma ida e volta sem ganho, e no dia do evento uma ida
 * e volta é algo que pode não voltar.
 */
(function () {
    'use strict';

    /* ---------- busca ---------- */

    /* Tira acento para comparar: quem procura "januario" tem que achar
       "Januário". A normalização NFD separa a letra do acento, e a faixa
       U+0300-U+036F remove só os acentos, preservando o resto. */
    function semAcento(texto) {
        return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    }

    var campo = document.getElementById('busca');
    var tabela = document.getElementById('tabela-inscritos');
    var semResultado = document.getElementById('sem-resultado');

    if (campo && tabela) {
        var linhas = Array.prototype.slice.call(tabela.tBodies[0].rows);

        // O texto de cada linha é preparado uma vez, não a cada tecla.
        linhas.forEach(function (linha) {
            linha.dataset.busca = semAcento(linha.textContent.replace(/\s+/g, ' '));
        });

        campo.addEventListener('input', function () {
            var termo = semAcento(campo.value.trim());
            var visiveis = 0;

            linhas.forEach(function (linha) {
                var combina = termo === '' || linha.dataset.busca.indexOf(termo) >= 0;
                linha.hidden = !combina;
                if (combina) { visiveis++; }
            });

            if (semResultado) { semResultado.hidden = visiveis > 0; }
            tabela.hidden = visiveis === 0;
        });
    }

    /* ---------- copiar o link público ---------- */

    var botao = document.getElementById('copiar-link');
    var link = document.getElementById('link-publico');

    if (link) {
        // Clicar no campo seleciona o endereço inteiro, para quem prefere
        // copiar à mão.
        link.addEventListener('click', function () { link.select(); });
    }

    if (botao && link) {
        botao.addEventListener('click', function () {
            var original = botao.textContent;

            function confirmar(texto) {
                botao.textContent = texto;
                botao.disabled = true;
                window.setTimeout(function () {
                    botao.textContent = original;
                    botao.disabled = false;
                }, 1600);
            }

            // A área de transferência exige contexto seguro (HTTPS ou
            // localhost). Em HTTP simples a chamada nem existe, então o
            // caminho alternativo seleciona o texto e diz o que fazer.
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(link.value).then(
                    function () { confirmar('Copiado'); },
                    function () { link.select(); confirmar('Use Ctrl+C'); }
                );
            } else {
                link.select();
                confirmar('Use Ctrl+C');
            }
        });
    }
})();
