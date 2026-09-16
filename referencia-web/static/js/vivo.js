// Duas coisas que deixam o sistema "redondo":
//
// 1. AVISO AO VIVO: um canal aberto com o servidor. Quando alguem lanca,
//    recebe ou cadastra alguma coisa na mesma empresa, esta tela fica sabendo
//    na hora. Se a pessoa estiver digitando ou com uma janela aberta, o sistema
//    nao atrapalha: mostra uma barra e deixa ela decidir quando atualizar.
//
// 2. ONDE PAROU: a tela guarda a posicao da rolagem e a janela que estava
//    aberta. Sair e voltar, ou recarregar, devolve a pessoa no mesmo lugar.

(function () {
  const caminho = location.pathname + location.search;
  const ehTelaDeEntrada = location.pathname === '/entrar';

  // ------------------------------------------------------------ onde parou
  function guardar(chave, valor) {
    try { localStorage.setItem(chave, valor); } catch (e) { /* navegador privado */ }
  }
  function ler(chave) {
    try { return localStorage.getItem(chave); } catch (e) { return null; }
  }

  if (!ehTelaDeEntrada) {
    guardar('itfc:ultimaTela', caminho);
    window.addEventListener('beforeunload', function () {
      guardar('itfc:rolagem:' + caminho, String(window.scrollY));
    });
    window.addEventListener('load', function () {
      const posicao = parseInt(ler('itfc:rolagem:' + caminho) || '0', 10);
      if (posicao > 0) window.scrollTo(0, posicao);

      // Reabre a janela de perfil que estava aberta nesta tela.
      const perfilAberto = ler('itfc:janela:' + caminho);
      if (perfilAberto) {
        const alvo = document.querySelector('[data-perfil="' + perfilAberto + '"]');
        if (alvo) alvo.click();
      }
    });
    document.addEventListener('click', function (evento) {
      const perfil = evento.target.closest('[data-perfil]');
      if (perfil) guardar('itfc:janela:' + caminho, perfil.dataset.perfil);
      const fecha = evento.target.closest('[data-fecha]');
      if (fecha) guardar('itfc:janela:' + caminho, '');
    });
  } else {
    // Na tela de entrada, leva junto o endereco de onde a pessoa parou.
    const campo = document.getElementById('voltarPara');
    const ultima = ler('itfc:ultimaTela');
    if (campo && ultima) campo.value = ultima;
  }

  // --------------------------------------------------------- aviso ao vivo
  if (ehTelaDeEntrada) return;

  let canal = null;
  let tentativa = 0;

  function ocupada() {
    if (document.querySelector('dialog[open]')) return true;
    const foco = document.activeElement;
    return !!(foco && foco.matches('input, textarea, select'));
  }

  function barraDeAtualizar() {
    if (document.getElementById('barra-atualizar')) return;
    const barra = document.createElement('div');
    barra.id = 'barra-atualizar';
    barra.className = 'barra-atualizar';
    barra.innerHTML = '<span>Novos lançamentos nesta empresa.</span>'
      + '<button type="button">Atualizar agora</button>';
    barra.querySelector('button').addEventListener('click', function () {
      location.reload();
    });
    document.body.appendChild(barra);
  }

  function conectar() {
    const protocolo = location.protocol === 'https:' ? 'wss://' : 'ws://';
    try {
      canal = new WebSocket(protocolo + location.host + '/ws/avisos');
    } catch (e) {
      return;
    }

    canal.addEventListener('open', function () { tentativa = 0; });

    canal.addEventListener('message', function () {
      if (ocupada()) {
        barraDeAtualizar();
      } else {
        guardar('itfc:rolagem:' + caminho, String(window.scrollY));
        location.reload();
      }
    });

    canal.addEventListener('close', function () {
      // Volta a tentar, com espera crescente, ate 30 segundos.
      tentativa = Math.min(tentativa + 1, 6);
      setTimeout(conectar, tentativa * 5000);
    });
  }

  conectar();
})();
