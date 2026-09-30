/*
 * Eco legível para campos de data e hora.
 *
 * O calendário que abre nos campos datetime-local é um widget do próprio
 * navegador: não dá para acrescentar botões dentro dele, nem um "OK" de
 * confirmação. O que dá para fazer é devolver a confirmação por fora --
 * escrevendo, logo abaixo do campo, o que ficou selecionado por extenso.
 *
 * Assim a pessoa vê "sábado, 12 de outubro de 2026 às 10:00" e sabe que
 * acertou, sem precisar reabrir o calendário para conferir.
 */
(function () {
    'use strict';

    const formatador = new Intl.DateTimeFormat('pt-BR', {
        weekday: 'long', day: '2-digit', month: 'long', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });

    function porExtenso(valor) {
        if (!valor) {
            return '';
        }
        const data = new Date(valor);
        if (isNaN(data.getTime())) {
            return '';
        }
        // O formatador pt-BR já devolve "terça-feira, 22 de setembro de 2026 às 09:00".
        // Acrescentar um "às" aqui produzia "às às" -- o idioma já resolveu isso.
        return formatador.format(data);
    }

    function ligar(campo) {
        const eco = document.createElement('small');
        eco.className = 'eco-data';
        campo.insertAdjacentElement('afterend', eco);

        const atualizar = () => {
            const texto = porExtenso(campo.value);
            eco.textContent = texto;
            eco.classList.toggle('preenchido', texto !== '');
        };

        campo.addEventListener('change', atualizar);
        campo.addEventListener('input', atualizar);
        atualizar();
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('input[type="datetime-local"]').forEach(ligar);
    });
})();
