# -*- coding: utf-8 -*-
"""Tira da pagina web cada pedaco visivel e procura ele na tela da janela."""
import io, os, re, unicodedata, json

WEB = r"C:\Projetos\ERP-BLANCO-&-LISBOA\FINANCEIRO-JAVA-ANTIGO\src\main\resources\templates"
JAVA = r"C:\Projetos\ERP-BLANCO-&-LISBOA\FINANCEIRO-JAVA\src\main\java\br\com\itia\financeiro\tela"

MAPA = {
 "painel.html": ["TelaPainel"],
 "titulos.html": ["TelaTitulos"], "titulo.html": ["TelaTitulo"], "titulo-novo.html": ["TelaTituloNovo"],
 "clientes.html": ["TelaClientes","TelaClienteNovo"], "cobranca.html": ["TelaCobranca"],
 "cobrancas.html": ["TelaCobrancas"], "comprovantes.html": ["TelaComprovantes"],
 "conciliacao.html": ["TelaConciliacao"], "conversas.html": ["TelaConversas"],
 "disparos.html": ["TelaDisparos"], "disparo.html": ["TelaDisparo"],
 "empresas.html": ["TelaEmpresas"], "empresas-configurar.html": ["TelaEmpresasConfigurar"],
 "empresas-pessoas.html": ["TelaPessoasDaEmpresa"], "entrar.html": ["TelaEntrar"],
 "esteira.html": ["TelaEsteira"], "etiquetas.html": ["TelaEtiquetas"],
 "fechamento.html": ["TelaFechamento"], "fluxos.html": ["TelaFluxos"],
 "fontes-de-tarefas.html": ["TelaFontesDeTarefas"], "integracao.html": ["TelaIntegracao"],
 "integracoes.html": ["TelaIntegracoes"], "integracoes-ajuda.html": ["TelaIntegracoesAjuda"],
 "inteligencia.html": ["TelaInteligencia"], "modelos.html": ["TelaModelos"],
 "obrigacao.html": ["TelaObrigacao","TelaObrigacaoNova"], "pacote.html": ["TelaPacote"],
 "pacotes.html": ["TelaPacotes"], "pagar.html": ["TelaPagar","JanelasDoPagar","TelaObrigacao"],
 "perfil.html": ["TelaPerfil"], "realizado.html": ["TelaRealizado"],
 "realizados.html": ["TelaRealizados"], "regras.html": ["TelaRegras"],
 "regua.html": ["TelaRegua"], "servico.html": ["TelaServico"], "servicos.html": ["TelaServicos"],
 "tarefas.html": ["TelaTarefas","TelaTarefaNova"], "acordos.html": ["TelaAcordos"],
 "acordo.html": ["TelaAcordo"], "agente.html": ["TelaAgente"], "analises.html": ["TelaAnalises"],
}

def limpar(t):
    t = unicodedata.normalize("NFKD", t)
    t = "".join(c for c in t if not unicodedata.combining(c))
    t = re.sub(r"[^a-z0-9 ]", " ", t.lower())
    return re.sub(r"\s+", " ", t).strip()

def pedacos(html):
    """O que a pagina mostra: titulo, secao, botao, coluna de tabela, rotulo e janela."""
    achados = []
    for tag, tipo in [("h1","titulo"),("h2","janela"),("h3","secao"),("th","coluna"),
                      ("label","campo"),("button","botao"),("legend","secao"),
                      ("summary","secao")]:
        for m in re.finditer(r"<%s\b[^>]*>(.*?)</%s>" % (tag, tag), html, re.S | re.I):
            texto = re.sub(r"<[^>]+>", " ", m.group(1))
            texto = re.sub(r"&[a-z#0-9]+;", " ", texto)
            texto = re.sub(r"\s+", " ", texto).strip()
            if texto and len(texto) > 2 and not texto.startswith("$"):
                achados.append((tipo, texto))
    for m in re.finditer(r'class="nome-bloco"[^>]*>([^<]{3,60})<', html):
        achados.append(("secao", m.group(1).strip()))
    for m in re.finditer(r'class="[^"]*\bolho\b[^"]*"[^>]*>(?:<i></i>)?([^<]{3,60})<', html):
        achados.append(("olho", m.group(1).strip()))
    return achados

relatorio = {}
for arquivo, telas in sorted(MAPA.items()):
    caminho = os.path.join(WEB, arquivo)
    if not os.path.exists(caminho):
        continue
    html = io.open(caminho, encoding="utf-8").read()
    java = ""
    for t in telas:
        p = os.path.join(JAVA, t + ".java")
        if os.path.exists(p):
            java += io.open(p, encoding="utf-8").read()
    alvo = limpar(java)
    faltando = []
    vistos = set()
    for tipo, texto in pedacos(html):
        chave = limpar(texto)
        if not chave or chave in vistos:
            continue
        vistos.add(chave)
        # procura a frase inteira; se for longa, procura os primeiros termos
        if chave in alvo:
            continue
        curto = " ".join(chave.split()[:4])
        if len(curto) > 8 and curto in alvo:
            continue
        faltando.append((tipo, texto))
    if faltando:
        relatorio[arquivo] = faltando

io.open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "faltando.json"),
        "w", encoding="utf-8").write(json.dumps(relatorio, ensure_ascii=False, indent=1))
for arquivo, itens in relatorio.items():
    print("== %s (%d)" % (arquivo, len(itens)))
