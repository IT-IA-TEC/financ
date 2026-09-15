/* Registro de atendimento.
   Duas coisas: mostra so os itens do servico escolhido, e pergunta ao sistema
   se o atendimento entra no pacote do cliente, antes de gravar. */
(function () {
  var form = document.querySelector('[data-confere-pacote]');
  if (!form) {
    return;
  }

  var cliente = form.querySelector('[data-cliente]');
  var servico = form.querySelector('[data-servico]');
  var dia = form.querySelector('[data-dia]');
  var quantidade = form.querySelector('[data-quantidade]');
  var tratamento = form.querySelector('[data-tratamento]');
  var aviso = form.querySelector('[data-aviso-pacote]');
  var semItens = form.querySelector('[data-sem-itens]');

  function ajustarItens() {
    var escolhas = form.querySelectorAll('.escolha-item');
    var visiveis = 0;
    escolhas.forEach(function (escolha) {
      var desteServico = escolha.getAttribute('data-de') === servico.value;
      escolha.style.display = desteServico ? '' : 'none';
      if (!desteServico) {
        escolha.querySelector('input').checked = false;
      } else {
        visiveis = visiveis + 1;
      }
    });
    if (semItens) {
      semItens.style.display = visiveis === 0 ? '' : 'none';
    }
  }

  function conferirPacote() {
    if (!cliente.value || !servico.value) {
      return;
    }
    var endereco = '/realizados/cobertura?pagadorId=' + encodeURIComponent(cliente.value)
      + '&servicoId=' + encodeURIComponent(servico.value)
      + '&dia=' + encodeURIComponent(dia.value || '')
      + '&quantidade=' + encodeURIComponent(quantidade.value || '1');

    fetch(endereco, { headers: { 'Accept': 'application/json' } })
      .then(function (resposta) { return resposta.json(); })
      .then(function (dados) {
        aviso.textContent = dados.explicacao;
        aviso.classList.toggle('atencao', !dados.temPacote || dados.precisaDeAvaliacao);
        if (tratamento.value === 'PELO_PACOTE') {
          aviso.textContent = dados.explicacao
            + ' O tratamento vai ser: ' + rotuloDe(dados.tratamento) + '.';
        }
      })
      .catch(function () {
        aviso.textContent = 'Não consegui conferir o pacote agora. O registro continua possível.';
      });
  }

  function rotuloDe(nome) {
    var opcao = tratamento.querySelector('option[value="' + nome + '"]');
    return opcao ? opcao.textContent.toLowerCase() : nome;
  }

  [cliente, servico, dia, quantidade, tratamento].forEach(function (campo) {
    campo.addEventListener('change', function () {
      if (campo === servico) {
        ajustarItens();
      }
      conferirPacote();
    });
  });

  ajustarItens();
  conferirPacote();
})();
