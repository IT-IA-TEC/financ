// Janelas flutuantes: cadastro e perfil.
// O perfil busca o conteudo pronto no servidor e troca so o miolo da janela,
// entao entrar numa unidade e voltar acontece sem recarregar a tela de tras.

(function () {
  const janelaPerfil = document.getElementById('janela-perfil');
  const conteudo = document.getElementById('perfil-conteudo');

  function abrir(janela) {
    if (janela && typeof janela.showModal === 'function') {
      janela.showModal();
    }
  }

  async function carregarPerfil(endereco) {
    if (!janelaPerfil || !conteudo) return;
    conteudo.innerHTML = '<div class="carregando">carregando</div>';
    if (!janelaPerfil.open) abrir(janelaPerfil);
    try {
      const resposta = await fetch(endereco, { headers: { 'X-Janela': '1' } });
      conteudo.innerHTML = await resposta.text();
    } catch (erro) {
      conteudo.innerHTML = '<div class="carregando">não consegui abrir o perfil agora</div>';
    }
  }

  document.addEventListener('click', function (evento) {
    const abre = evento.target.closest('[data-abre]');
    if (abre) {
      abrir(document.getElementById(abre.dataset.abre));
      return;
    }

    const perfil = evento.target.closest('[data-perfil]');
    if (perfil) {
      carregarPerfil(perfil.dataset.perfil);
      return;
    }

    const fecha = evento.target.closest('[data-fecha]');
    if (fecha) {
      const janela = fecha.closest('dialog');
      if (janela) janela.close();
    }
  });

  // Clicar fora da janela fecha.
  document.querySelectorAll('dialog.janela').forEach(function (janela) {
    janela.addEventListener('click', function (evento) {
      if (evento.target === janela) janela.close();
    });
  });
})();
