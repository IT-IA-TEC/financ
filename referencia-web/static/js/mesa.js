// A mesa de cobranca: comandos com barra, teclas de atalho e a conversa
// abrindo no fim.
//
// Todo texto que entra na caixa vem montado do servidor, com os numeros reais
// do cliente. Aqui so colocamos na caixa: ninguem digita valor na pressa.
(function () {
  'use strict';

  var caixa = document.getElementById('mesa-texto');
  var menu = document.getElementById('menu-comandos');
  var comandos = Array.prototype.slice.call(
    document.querySelectorAll('.linha-comando'));

  function escrever(texto, trocando) {
    if (!caixa) {
      return;
    }
    if (trocando || caixa.value.trim() === '') {
      caixa.value = texto;
    } else {
      caixa.value = caixa.value.trim() + '\n\n' + texto;
    }
    caixa.focus();
    caixa.selectionStart = caixa.value.length;
    caixa.selectionEnd = caixa.value.length;
    esconderMenu();
  }

  function esconderMenu() {
    if (menu) {
      menu.hidden = true;
    }
  }

  function mostrarMenu(filtro) {
    if (!menu) {
      return;
    }
    var algum = false;
    comandos.forEach(function (linha) {
      var nome = linha.getAttribute('data-nome') || '';
      var cabe = filtro === '' || nome.indexOf(filtro) === 0;
      linha.hidden = !cabe;
      algum = algum || cabe;
    });
    menu.hidden = !algum;
  }

  // ------------------------------------------------------- comandos com barra
  if (caixa && menu) {
    caixa.addEventListener('input', function () {
      var ate = caixa.value.slice(0, caixa.selectionStart);
      var ultima = ate.split('\n').pop();
      if (ultima.charAt(0) === '/') {
        mostrarMenu(ultima.slice(1).toLowerCase());
      } else {
        esconderMenu();
      }
    });

    caixa.addEventListener('keydown', function (evento) {
      if (evento.key === 'Escape') {
        esconderMenu();
      }
    });

    comandos.forEach(function (linha) {
      linha.addEventListener('click', function () {
        // tira a linha que comecou com barra e poe o texto do comando
        var partes = caixa.value.split('\n');
        if (partes.length && partes[partes.length - 1].charAt(0) === '/') {
          partes.pop();
          caixa.value = partes.join('\n');
        }
        escrever(linha.getAttribute('data-texto'), caixa.value.trim() === '');
      });
    });
  }

  // ------------------------------------------------- botoes de texto pronto
  document.querySelectorAll('[data-comando]').forEach(function (botao) {
    var nome = botao.getAttribute('data-comando');
    var linha = document.querySelector('.linha-comando[data-nome="' + nome + '"]');
    if (!linha) {
      botao.disabled = true;
      return;
    }
    botao.addEventListener('click', function () {
      escrever(linha.getAttribute('data-texto'), false);
    });
  });

  // ------------------------------------------------------ teclas de atalho
  var teclas = {
    F2: '2via', F3: 'pix', F4: 'tudo'
  };
  var janelas = {
    F5: 'janela-promessa', F6: 'janela-simular', F9: 'janela-ligacao',
    F10: 'janela-retorno', F11: 'janela-transferir', F12: 'janela-pausa'
  };

  document.addEventListener('keydown', function (evento) {
    if (evento.ctrlKey || evento.altKey || evento.metaKey) {
      return;
    }
    var comando = teclas[evento.key];
    if (comando) {
      var linha = document.querySelector('.linha-comando[data-nome="' + comando + '"]');
      if (linha) {
        evento.preventDefault();
        escrever(linha.getAttribute('data-texto'), false);
      }
      return;
    }
    var janela = janelas[evento.key];
    if (janela) {
      var caixaJanela = document.getElementById(janela);
      if (caixaJanela && typeof caixaJanela.showModal === 'function') {
        evento.preventDefault();
        caixaJanela.showModal();
      }
    }
  });

  // -------------------------------------------- a conversa abre no fim dela
  var conversa = document.querySelector('.conversa-caixa');
  if (conversa) {
    conversa.scrollTop = conversa.scrollHeight;
  }
})();
