/* Aviso sonoro da portaria.
 *
 * No portão o operador olha para a pessoa, não para a tela: o som diz
 * se pode passar antes de ele voltar o olho para o celular. São três
 * sinais claramente diferentes entre si -- dois tons subindo para
 * liberado, dois tons iguais para já registrado, um grave longo para
 * código desconhecido -- porque "um bipe qualquer" não informa nada com
 * a fila andando.
 *
 * Os tons são gerados pelo próprio navegador (Web Audio): nenhum
 * arquivo de áudio no repositório e nada a baixar, o que importa porque
 * o sistema precisa funcionar sem internet no dia do evento.
 *
 * O estado do interruptor fica no navegador do próprio aparelho, então
 * cada operador escolhe o seu sem afetar os outros.
 */
(function () {
    'use strict';

    var CHAVE = 'portaria.som';
    var contexto = null;

    function ligado() {
        try {
            return window.localStorage.getItem(CHAVE) !== 'nao';
        } catch (erro) {
            // Navegador em janela anônima ou com dados de site bloqueados.
            return true;
        }
    }

    function guardar(valor) {
        try {
            window.localStorage.setItem(CHAVE, valor ? 'sim' : 'nao');
        } catch (erro) {
            // Sem armazenamento a escolha vale só para esta sessão. Não é
            // motivo para quebrar a tela.
        }
    }

    function tocar(notas) {
        if (!ligado()) { return; }
        try {
            var Fabrica = window.AudioContext || window.webkitAudioContext;
            if (!Fabrica) { return; }
            if (!contexto) { contexto = new Fabrica(); }
            if (contexto.state === 'suspended') { contexto.resume(); }

            var inicio = contexto.currentTime;
            notas.forEach(function (nota) {
                var oscilador = contexto.createOscillator();
                var ganho = contexto.createGain();
                oscilador.type = nota.forma || 'sine';
                oscilador.frequency.value = nota.hz;

                // A subida e a descida evitam o estalo que um volume
                // ligado de uma vez produz no alto-falante.
                var em = inicio + nota.em;
                ganho.gain.setValueAtTime(0.0001, em);
                ganho.gain.exponentialRampToValueAtTime(0.22, em + 0.012);
                ganho.gain.exponentialRampToValueAtTime(0.0001, em + nota.dura);

                oscilador.connect(ganho);
                ganho.connect(contexto.destination);
                oscilador.start(em);
                oscilador.stop(em + nota.dura + 0.03);
            });
        } catch (erro) {
            // Áudio bloqueado pelo navegador. A tela já mostra o
            // resultado; o som é reforço, nunca a única informação.
        }
    }

    var SONS = {
        liberado:     [{ hz: 880, em: 0, dura: 0.09 }, { hz: 1320, em: 0.10, dura: 0.13 }],
        repetido:     [{ hz: 460, em: 0, dura: 0.11 }, { hz: 460, em: 0.19, dura: 0.11 }],
        desconhecido: [{ hz: 190, em: 0, dura: 0.34, forma: 'square' }]
    };

    function reagir() {
        var painel = document.getElementById('painel');
        if (!painel) { return; }

        var caixa = document.getElementById('som');
        if (caixa) { caixa.checked = ligado(); }

        var estado = painel.getAttribute('data-resultado');
        if (SONS[estado]) { tocar(SONS[estado]); }
    }

    document.addEventListener('change', function (evento) {
        if (evento.target && evento.target.id === 'som') {
            guardar(evento.target.checked);
        }
    });

    // O htmx troca o painel a cada leitura; é aí que o retorno acontece.
    document.body.addEventListener('htmx:afterSwap', reagir);

    // Caminho sem JavaScript do htmx (envio normal do formulário): a
    // página inteira recarrega e o evento acima nunca dispara.
    reagir();
})();
