// O "+ Novo" da tela de Tarefas.
//
// E a mesma lista da Cupula do sistema matriz: seis coisas diferentes saindo do
// mesmo botao. Por baixo todas viram a mesma tarefa, porque o caminho e o mesmo
// (alguem pede, alguem faz, quem pediu confere). O que muda e o vocabulario da
// janela, para a pessoa saber o que esta abrindo, e a demanda, que abre varios
// tickets de uma vez.

(function () {
  const botao = document.getElementById('botao-novo');
  const lista = document.getElementById('lista-novo');
  const janela = document.getElementById('janela-tarefa');
  if (!botao || !lista || !janela) return;

  const PALAVRAS = {
    TAREFA: {
      olho: 'Pedir para alguém',
      titulo: 'Nova tarefa',
      dica: 'Deixe "quem faz" em branco para abrir a tarefa ao setor inteiro: ela fica na fila de todos até alguém pegar.',
      rotulo: 'O que precisa ser feito',
      exemplo: 'Conferir os comprovantes de setembro',
      detalhe: 'Detalhe',
      criar: 'Criar tarefa'
    },
    DEMANDA: {
      olho: 'Várias tarefas de uma vez',
      titulo: 'Nova demanda',
      dica: 'A demanda é o guarda-chuva. Dê um nome para ela e abra abaixo um ticket para cada coisa que precisa ser feita.',
      rotulo: 'Nome da demanda',
      exemplo: 'Fechamento de setembro',
      detalhe: 'Detalhe da demanda',
      criar: 'Criar demanda'
    },
    DECISAO: {
      olho: 'Alguém precisa bater o martelo',
      titulo: 'Nova decisão',
      dica: 'A decisão fica registrada com quem decidiu e quando. Escreva a pergunta do jeito que ela precisa ser respondida.',
      rotulo: 'O que precisa ser decidido',
      exemplo: 'Manter ou cortar a cobrança automática da Loja da Maria',
      detalhe: 'O que pesa na decisão',
      criar: 'Criar decisão'
    },
    ALERTA: {
      olho: 'Só para saber',
      titulo: 'Alerta informativo',
      dica: 'Alerta não tem trabalho embutido: quem recebe só precisa ler e dar por vista.',
      rotulo: 'O aviso',
      exemplo: 'O banco fica fora do ar no sábado de manhã',
      detalhe: 'Detalhe do aviso',
      criar: 'Enviar alerta'
    },
    CHAMADO: {
      olho: 'Algo não está funcionando',
      titulo: 'Chamado técnico',
      dica: 'Escreva o que você fez, o que esperava que acontecesse e o que aconteceu. Sem isso o chamado volta pedindo informação.',
      rotulo: 'O que está acontecendo',
      exemplo: 'A baixa por comprovante não aceita o arquivo do Itaú',
      detalhe: 'O que você fez antes do erro',
      criar: 'Abrir chamado'
    },
    COMPRA: {
      olho: 'Precisa de autorização',
      titulo: 'Solicitação de compra',
      dica: 'Diga o que é, para que serve e quanto custa. Quem aprova gasto decide com isso na mão.',
      rotulo: 'O que precisa ser comprado',
      exemplo: 'Dois monitores para a mesa de cobrança',
      detalhe: 'Para que serve e quanto custa',
      criar: 'Pedir compra'
    }
  };

  const campoTipo = document.getElementById('tf-tipo');
  const olho = document.getElementById('tf-olho');
  const tituloJanela = document.getElementById('tf-titulo-janela');
  const dica = document.getElementById('tf-dica');
  const rotulo = document.getElementById('tf-rotulo-titulo');
  const campoTitulo = document.getElementById('tf-titulo');
  const rotuloDescricao = document.getElementById('tf-rotulo-descricao');
  const botaoCriar = document.getElementById('tf-criar');
  const tickets = document.getElementById('tf-tickets');
  const linhas = document.getElementById('tf-linhas-ticket');
  const soTicketUnico = document.querySelectorAll('[data-so-ticket-unico]');

  function abrirLista(abrir) {
    lista.hidden = !abrir;
    botao.setAttribute('aria-expanded', abrir ? 'true' : 'false');
  }

  function vestir(tipo) {
    const p = PALAVRAS[tipo] || PALAVRAS.TAREFA;
    campoTipo.value = tipo;
    olho.textContent = p.olho;
    tituloJanela.textContent = p.titulo;
    dica.textContent = p.dica;
    rotulo.textContent = p.rotulo;
    campoTitulo.placeholder = p.exemplo;
    rotuloDescricao.textContent = p.detalhe;
    botaoCriar.textContent = p.criar;

    const ehDemanda = tipo === 'DEMANDA';
    tickets.hidden = !ehDemanda;
    // campo escondido nao pode ir no envio, senao a demanda nasce com um dono
    // que ninguem escolheu
    soTicketUnico.forEach(function (bloco) {
      bloco.hidden = ehDemanda;
      bloco.querySelectorAll('input, select').forEach(function (campo) {
        campo.disabled = ehDemanda;
      });
    });
    tickets.querySelectorAll('input').forEach(function (campo) {
      campo.disabled = !ehDemanda;
    });
    if (ehDemanda) {
      const primeiro = linhas.querySelector('[data-ticket-titulo]');
      if (primeiro) primeiro.required = true;
    }
  }

  function renumerar() {
    const todas = linhas.querySelectorAll('.linha-ticket');
    todas.forEach(function (linha, posicao) {
      linha.querySelector('.conta-ticket').textContent = String(posicao + 1);
      const tirar = linha.querySelector('.tirar-ticket');
      // a primeira linha nao some: a demanda sem nenhum ticket nao existe
      if (tirar) tirar.hidden = todas.length === 1;
    });
  }

  botao.addEventListener('click', function (evento) {
    evento.stopPropagation();
    abrirLista(lista.hidden);
  });

  document.addEventListener('click', function (evento) {
    const escolha = evento.target.closest('[data-novo]');
    if (escolha) {
      vestir(escolha.dataset.novo);
      abrirLista(false);
      renumerar();
      janela.showModal();
      campoTitulo.focus();
      return;
    }
    if (!lista.hidden && !lista.contains(evento.target)) abrirLista(false);
  });

  document.addEventListener('keydown', function (evento) {
    if (evento.key === 'Escape' && !lista.hidden) abrirLista(false);
  });

  document.getElementById('tf-mais-ticket').addEventListener('click', function () {
    const modelo = linhas.querySelector('.linha-ticket');
    const nova = modelo.cloneNode(true);
    nova.querySelectorAll('input').forEach(function (campo) {
      campo.value = '';
      campo.disabled = false;
      campo.required = false;
    });
    linhas.appendChild(nova);
    renumerar();
    nova.querySelector('[data-ticket-titulo]').focus();
  });

  linhas.addEventListener('click', function (evento) {
    const tirar = evento.target.closest('.tirar-ticket');
    if (!tirar) return;
    if (linhas.querySelectorAll('.linha-ticket').length === 1) return;
    tirar.closest('.linha-ticket').remove();
    renumerar();
  });

  vestir('TAREFA');
  renumerar();
})();
