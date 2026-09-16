/* Composicao do pacote.
   Duas coisas so: a lista de itens mostra apenas os itens do servico escolhido,
   e a quantidade some quando a utilizacao e ilimitada. */
(function () {
  function ajustarItens(form) {
    var servico = form.querySelector('[data-servico]');
    var abrangencia = form.querySelector('[data-abrangencia]');
    var item = form.querySelector('[data-item]');
    if (!servico || !item) {
      return;
    }
    var soUmItem = abrangencia && abrangencia.value === 'ITEM_ESCOLHIDO';
    var campo = item.closest('div');
    if (campo) {
      campo.style.opacity = soUmItem ? '1' : '.4';
    }
    item.disabled = !soUmItem;

    Array.prototype.forEach.call(item.options, function (opcao) {
      if (!opcao.value) {
        return;
      }
      var desteServico = opcao.getAttribute('data-de') === servico.value;
      opcao.hidden = !desteServico;
      if (!desteServico && opcao.selected) {
        item.value = '';
      }
    });
  }

  function ajustarQuantidade(form) {
    var ilimitado = form.querySelector('[data-ilimitado]');
    var quantidade = form.querySelector('[data-quantidade]');
    if (!ilimitado || !quantidade) {
      return;
    }
    var semLimite = ilimitado.value === 'true';
    quantidade.disabled = semLimite;
    if (semLimite) {
      quantidade.value = '';
    }
    var campo = quantidade.closest('div');
    if (campo) {
      campo.style.opacity = semLimite ? '.4' : '1';
    }
  }

  function ligar(form) {
    ajustarItens(form);
    ajustarQuantidade(form);
    form.addEventListener('change', function (evento) {
      if (evento.target.matches('[data-servico], [data-abrangencia]')) {
        ajustarItens(form);
      }
      if (evento.target.matches('[data-ilimitado]')) {
        ajustarQuantidade(form);
      }
    });
  }

  document.querySelectorAll('form').forEach(function (form) {
    if (form.querySelector('[data-servico]') || form.querySelector('[data-ilimitado]')) {
      ligar(form);
    }
  });
})();
