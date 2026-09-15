/* Os dois paineis analiticos do contas a receber.
   Tudo desenhado em SVG aqui mesmo: sem biblioteca de fora, com as cores e o
   canto reto da marca. O servidor manda os numeros e a leitura de cada modo;
   esta tela so desenha e troca de modo. */
(function () {
  var area = document.getElementById('paineis');
  if (!area) {
    return;
  }

  var COR = {
    tinta: '#050506',
    acento: '#FF0000',
    nevoa: '#B9B9BE',
    grafite: '#2E2E31',
    claro: '#EDEDEF'
  };

  var estado = { periodo: { esquerda: 'mes', direita: 'mes' }, modos: {} };

  function dinheiro(valor) {
    var n = Number(valor || 0);
    if (Math.abs(n) >= 1000000) {
      return 'R$ ' + (n / 1000000).toFixed(1).replace('.', ',') + ' mi';
    }
    if (Math.abs(n) >= 1000) {
      return 'R$ ' + (n / 1000).toFixed(1).replace('.', ',') + ' mil';
    }
    return 'R$ ' + n.toFixed(0);
  }

  function rotuloValor(modo, valor) {
    var n = Number(valor || 0);
    if (modo.tipo === 'linhas' && modo.chave === 'prazo') {
      return n.toFixed(0) + 'd';
    }
    if (modo.chave === 'inadimplencia' || modo.chave === 'pareto') {
      return n.toFixed(0);
    }
    return dinheiro(n);
  }

  function elemento(nome, atributos, texto) {
    var alvo = document.createElementNS('http://www.w3.org/2000/svg', nome);
    Object.keys(atributos || {}).forEach(function (chave) {
      alvo.setAttribute(chave, atributos[chave]);
    });
    if (texto !== undefined) {
      alvo.textContent = texto;
    }
    return alvo;
  }

  function maiorValor(series, apenas) {
    var maior = 0;
    series.forEach(function (serie) {
      if (apenas && serie.desenho !== apenas) {
        return;
      }
      serie.pontos.forEach(function (ponto) {
        maior = Math.max(maior, Math.abs(Number(ponto.valor || 0)));
      });
    });
    return maior === 0 ? 1 : maior;
  }

  /* ------------------------------------------------------------ desenhos */

  function desenhar(modo) {
    var largura = 720;
    var altura = 300;
    var margem = { cima: 18, direita: 46, baixo: 42, esquerda: 62 };
    var svg = elemento('svg', {
      viewBox: '0 0 ' + largura + ' ' + altura,
      preserveAspectRatio: 'xMidYMid meet',
      role: 'img',
      'aria-label': modo.nome
    });

    if (modo.tipo === 'rosca') {
      desenharRosca(svg, modo, largura, altura);
      return svg;
    }

    var largoUtil = largura - margem.esquerda - margem.direita;
    var altoUtil = altura - margem.cima - margem.baixo;
    var rotulos = modo.series[0].pontos.map(function (p) { return p.rotulo; });
    var passo = largoUtil / Math.max(rotulos.length, 1);

    var barras = modo.series.filter(function (s) { return s.desenho === 'barra'; });
    var linhas = modo.series.filter(function (s) { return s.desenho === 'linha'; });
    var escalaBarras = maiorValor(barras.length ? barras : modo.series, null);
    var escalaLinhas = maiorValor(linhas, null);

    // linhas de apoio e eixo
    for (var i = 0; i <= 4; i++) {
      var y = margem.cima + (altoUtil / 4) * i;
      svg.appendChild(elemento('line', {
        x1: margem.esquerda, x2: largura - margem.direita, y1: y, y2: y,
        stroke: i === 4 ? COR.tinta : COR.claro, 'stroke-width': i === 4 ? 1.5 : 1
      }));
      var valorEixo = escalaBarras * (1 - i / 4);
      svg.appendChild(elemento('text', {
        x: margem.esquerda - 8, y: y + 4, 'text-anchor': 'end',
        'font-size': 10, fill: COR.grafite, 'font-family': 'IBM Plex Mono, monospace'
      }, rotuloValor(modo, valorEixo)));
    }

    // barras
    if (barras.length) {
      var larguraBarra = Math.min(38, (passo * 0.62) / barras.length);
      barras.forEach(function (serie, indiceSerie) {
        serie.pontos.forEach(function (ponto, indice) {
          var valor = Number(ponto.valor || 0);
          var alturaBarra = (Math.abs(valor) / escalaBarras) * altoUtil;
          var x = margem.esquerda + passo * indice + passo / 2
            - (larguraBarra * barras.length) / 2 + larguraBarra * indiceSerie;
          var y = margem.cima + altoUtil - alturaBarra;
          var barra = elemento('rect', {
            x: x, y: y, width: Math.max(larguraBarra - 2, 2),
            height: Math.max(alturaBarra, valor === 0 ? 0 : 1),
            fill: COR[serie.cor] || COR.tinta
          });
          barra.appendChild(elemento('title', {},
            serie.nome + ' · ' + ponto.rotulo + ': ' + rotuloValor(modo, valor)));
          svg.appendChild(barra);
        });
      });
    }

    // linhas
    linhas.forEach(function (serie) {
      var escala = barras.length ? escalaLinhas : escalaBarras;
      var caminho = '';
      serie.pontos.forEach(function (ponto, indice) {
        var valor = Number(ponto.valor || 0);
        var x = margem.esquerda + passo * indice + passo / 2;
        var y = margem.cima + altoUtil - (Math.abs(valor) / escala) * altoUtil;
        caminho += (indice === 0 ? 'M' : 'L') + x + ' ' + y + ' ';
        var bolinha = elemento('circle', {
          cx: x, cy: y, r: 3.5, fill: COR[serie.cor] || COR.acento
        });
        bolinha.appendChild(elemento('title', {},
          serie.nome + ' · ' + ponto.rotulo + ': ' + rotuloValor(modo, valor)));
        svg.appendChild(bolinha);
      });
      svg.insertBefore(elemento('path', {
        d: caminho.trim(), fill: 'none', stroke: COR[serie.cor] || COR.acento,
        'stroke-width': 2
      }), svg.firstChild.nextSibling);
    });

    // rotulos do eixo de baixo
    rotulos.forEach(function (rotulo, indice) {
      if (rotulos.length > 14 && indice % 2 !== 0) {
        return;
      }
      svg.appendChild(elemento('text', {
        x: margem.esquerda + passo * indice + passo / 2,
        y: altura - margem.baixo + 18,
        'text-anchor': 'middle', 'font-size': 10, fill: COR.grafite,
        'font-family': 'IBM Plex Mono, monospace'
      }, rotulo));
    });

    return svg;
  }

  function desenharRosca(svg, modo, largura, altura) {
    var pontos = modo.series[0].pontos;
    var total = pontos.reduce(function (soma, p) { return soma + Number(p.valor || 0); }, 0);
    var centroX = largura / 2 - 60;
    var centroY = altura / 2;
    var raio = 96;
    var espessura = 34;
    var cores = [COR.tinta, COR.nevoa, COR.acento];
    var angulo = -Math.PI / 2;

    if (total <= 0) {
      svg.appendChild(elemento('text', {
        x: largura / 2, y: altura / 2, 'text-anchor': 'middle',
        'font-size': 13, fill: COR.grafite
      }, 'sem dados no período'));
      return;
    }

    pontos.forEach(function (ponto, indice) {
      var valor = Number(ponto.valor || 0);
      if (valor <= 0) {
        return;
      }
      var fatia = (valor / total) * Math.PI * 2;
      var fim = angulo + fatia;
      var grande = fatia > Math.PI ? 1 : 0;
      var x1 = centroX + Math.cos(angulo) * raio;
      var y1 = centroY + Math.sin(angulo) * raio;
      var x2 = centroX + Math.cos(fim) * raio;
      var y2 = centroY + Math.sin(fim) * raio;
      var xi2 = centroX + Math.cos(fim) * (raio - espessura);
      var yi2 = centroY + Math.sin(fim) * (raio - espessura);
      var xi1 = centroX + Math.cos(angulo) * (raio - espessura);
      var yi1 = centroY + Math.sin(angulo) * (raio - espessura);

      var caminho = elemento('path', {
        d: 'M' + x1 + ' ' + y1 + ' A' + raio + ' ' + raio + ' 0 ' + grande + ' 1 '
          + x2 + ' ' + y2 + ' L' + xi2 + ' ' + yi2 + ' A' + (raio - espessura) + ' '
          + (raio - espessura) + ' 0 ' + grande + ' 0 ' + xi1 + ' ' + yi1 + ' Z',
        fill: cores[indice % cores.length],
        stroke: '#FFFFFF', 'stroke-width': 1
      });
      caminho.appendChild(elemento('title', {},
        ponto.rotulo + ': ' + dinheiro(valor) + ' ('
        + ((valor / total) * 100).toFixed(1) + '%)'));
      svg.appendChild(caminho);
      angulo = fim;
    });

    pontos.forEach(function (ponto, indice) {
      var valor = Number(ponto.valor || 0);
      var y = centroY - 34 + indice * 26;
      svg.appendChild(elemento('rect', {
        x: centroX + raio + 30, y: y - 9, width: 12, height: 12,
        fill: [COR.tinta, COR.nevoa, COR.acento][indice % 3]
      }));
      svg.appendChild(elemento('text', {
        x: centroX + raio + 50, y: y + 1, 'font-size': 12, fill: COR.tinta
      }, ponto.rotulo + ': ' + dinheiro(valor)));
      svg.appendChild(elemento('text', {
        x: centroX + raio + 50, y: y + 15, 'font-size': 10.5, fill: COR.grafite,
        'font-family': 'IBM Plex Mono, monospace'
      }, total > 0 ? ((valor / total) * 100).toFixed(1) + '%' : ''));
    });
  }

  /* Texto sempre por textContent: nada do banco vira marcacao na tela. */
  function texto(tag, conteudo, classe) {
    var alvo = document.createElement(tag);
    alvo.textContent = conteudo || '';
    if (classe) {
      alvo.className = classe;
    }
    return alvo;
  }

  function olho(conteudo) {
    var alvo = document.createElement('span');
    alvo.className = 'olho';
    alvo.appendChild(document.createElement('i'));
    alvo.appendChild(document.createTextNode(conteudo || ''));
    return alvo;
  }

  /* -------------------------------------------------------------- montagem */

  function seletorDePeriodo(lado, periodos) {
    var caixa = document.createElement('label');
    caixa.className = 'periodo-do-quadro';
    caixa.appendChild(document.createTextNode('Período'));
    var lista = document.createElement('select');
    periodos.forEach(function (periodo) {
      var opcao = document.createElement('option');
      opcao.value = periodo.chave;
      opcao.textContent = periodo.rotulo;
      opcao.selected = periodo.chave === estado.periodo[lado];
      lista.appendChild(opcao);
    });
    lista.addEventListener('change', function () {
      estado.periodo[lado] = lista.value;
      buscar(lado);
    });
    caixa.appendChild(lista);
    return caixa;
  }

  function montarPainel(painel, lado, dados) {
    var chaveEscolhida = estado.modos[lado]
      || (painel.modos[0] && painel.modos[0].chave);
    var modo = painel.modos.filter(function (m) { return m.chave === chaveEscolhida; })[0]
      || painel.modos[0];

    var quadro = document.createElement('section');
    quadro.className = 'quadro-grafico' + (lado === 'direita' ? ' espelhado' : '');

    var topo = document.createElement('div');
    topo.className = 'grafico-topo';
    topo.appendChild(seletorDePeriodo(lado, dados.periodos));
    var tituloDoTopo = document.createElement('div');
    tituloDoTopo.appendChild(olho(painel.titulo));

    // Titulo e subtitulo na mesma linha, separados pelo traco vermelho.
    var linhaDoTitulo = document.createElement('h2');
    linhaDoTitulo.appendChild(document.createTextNode(modo.nome));
    linhaDoTitulo.appendChild(texto('span', '-', 'traco'));
    linhaDoTitulo.appendChild(texto('span', modo.descricao, 'sub'));
    tituloDoTopo.appendChild(linhaDoTitulo);
    topo.appendChild(tituloDoTopo);

    var abas = document.createElement('div');
    abas.className = 'modos';
    painel.modos.forEach(function (opcao) {
      var botao = document.createElement('button');
      botao.type = 'button';
      botao.textContent = opcao.nome;
      botao.className = opcao.chave === modo.chave ? 'ativo' : '';
      botao.addEventListener('click', function () {
        estado.modos[lado] = opcao.chave;
        desenharTudo(dados);
      });
      abas.appendChild(botao);
    });

    var desenho = document.createElement('div');
    desenho.className = 'grafico-area';
    desenho.appendChild(desenhar(modo));

    var leitura = document.createElement('aside');
    leitura.className = 'grafico-leitura ' + modo.leitura.situacao;
    leitura.appendChild(olho('Leitura'));
    leitura.appendChild(texto('h3', modo.leitura.titulo));
    leitura.appendChild(texto('p', modo.leitura.indicador, 'indicador'));
    leitura.appendChild(texto('p', modo.leitura.texto));
    leitura.appendChild(texto('p', painel.pergunta, 'pergunta'));

    var corpo = document.createElement('div');
    corpo.className = 'grafico-corpo';
    corpo.appendChild(desenho);
    corpo.appendChild(leitura);

    quadro.appendChild(topo);
    quadro.appendChild(abas);
    quadro.appendChild(corpo);
    return quadro;
  }

  var lugares = {};

  function trocar(lado, quadro) {
    if (lugares[lado] && lugares[lado].parentNode === area) {
      area.replaceChild(quadro, lugares[lado]);
    } else if (lado === 'esquerda') {
      area.insertBefore(quadro, area.firstChild);
    } else {
      area.appendChild(quadro);
    }
    lugares[lado] = quadro;
  }

  /* Cada quadro tem o proprio periodo: da para comparar o mes de um lado com
     o ano do outro, sem sair da tela. */
  function buscar(lado) {
    var aguardando = document.createElement('section');
    aguardando.className = 'quadro-grafico';
    aguardando.appendChild(texto('p', 'montando o painel', 'carregando'));
    trocar(lado, aguardando);

    fetch('/titulos/graficos?periodo=' + encodeURIComponent(estado.periodo[lado]),
      { headers: { Accept: 'application/json' } })
      .then(function (resposta) { return resposta.json(); })
      .then(function (dados) {
        var painel = lado === 'esquerda' ? dados.esquerda : dados.direita;
        trocar(lado, montarPainel(painel, lado, dados));
      })
      .catch(function () {
        var erro = document.createElement('section');
        erro.className = 'quadro-grafico';
        erro.appendChild(texto('p', 'não consegui montar este painel agora', 'carregando'));
        trocar(lado, erro);
      });
  }

  area.replaceChildren();
  buscar('esquerda');
  buscar('direita');
})();
