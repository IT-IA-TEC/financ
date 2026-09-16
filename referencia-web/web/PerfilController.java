package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Consentimento;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.Fonte;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.servico.CadastroDeUnidades;
import br.com.itia.financeiro.servico.CarteiraServico;
import br.com.itia.financeiro.servico.CatalogoServico;
import br.com.itia.financeiro.servico.ConsultaDeCnpj;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.DocumentoServico;
import br.com.itia.financeiro.servico.EtiquetaServico;
import br.com.itia.financeiro.servico.PerfilServico;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O perfil do cliente, em janela.
 *
 * Cada aba vem como um pedaço de tela, buscado sem recarregar o fundo. A pessoa
 * abre o perfil, entra numa unidade, volta, entra em outra, e a lista de trás
 * continua onde estava.
 */
@Controller
@RequestMapping("/clientes")
public class PerfilController {

    private final PerfilServico perfil;
    private final CarteiraServico carteira;
    private final EtiquetaServico etiquetas;
    private final DocumentoServico documentos;
    private final ConsultaDeCnpj consultaDeCnpj;
    private final CatalogoServico catalogo;
    private final ContextoEmpresa contexto;
    private final CadastroDeUnidades cadastro;

    public PerfilController(PerfilServico perfil, CarteiraServico carteira,
                            EtiquetaServico etiquetas, DocumentoServico documentos,
                            ConsultaDeCnpj consultaDeCnpj, CatalogoServico catalogo,
                            ContextoEmpresa contexto, CadastroDeUnidades cadastro) {
        this.perfil = perfil;
        this.carteira = carteira;
        this.etiquetas = etiquetas;
        this.documentos = documentos;
        this.consultaDeCnpj = consultaDeCnpj;
        this.catalogo = catalogo;
        this.contexto = contexto;
        this.cadastro = cadastro;
    }

    // ------------------------------------------------------ perfil da pessoa

    @GetMapping("/{id}/perfil")
    public String perfilDaPessoa(@PathVariable UUID id,
                                 @RequestParam(required = false, defaultValue = "resumo") String aba,
                                 Model model) {
        Pagador pessoa = perfil.pessoa(id);
        model.addAttribute("pessoa", pessoa);
        model.addAttribute("aba", aba);
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("posicao", perfil.posicaoDa(id));
        model.addAttribute("unidades", perfil.unidadesDe(id));

        switch (aba) {
            case "cadastro" -> {
                model.addAttribute("contatos", perfil.contatosDe(id));
                model.addAttribute("papeis", etiquetas.ativasDoEscopo("CONTATO"));
            }
            case "contratos" -> {
                model.addAttribute("contratos", perfil.contratosDe(id));
                model.addAttribute("servicos", catalogo.catalogo());
            }
            case "financeiro" -> {
                model.addAttribute("credito", perfil.creditoDe(id));
                model.addAttribute("disponivel", perfil.limiteDisponivel(id));
                model.addAttribute("restricoes", perfil.restricoesDe(id));
            }
            case "historico" -> {
                model.addAttribute("historico", perfil.historicoDe(id));
                model.addAttribute("tipos", Interacao.TIPOS);
            }
            case "documentos" -> {
                model.addAttribute("documentos", perfil.documentosDe(id));
                model.addAttribute("tiposDocumento", Documento.TIPOS);
            }
            case "conformidade" -> {
                model.addAttribute("consentimentos", perfil.consentimentosDe(id));
                model.addAttribute("canais", Consentimento.CANAIS);
                model.addAttribute("bases", Consentimento.BASES);
                model.addAttribute("contatos", perfil.contatosDe(id));
            }
            default -> {
                model.addAttribute("contratos", perfil.contratosDe(id));
                model.addAttribute("credito", perfil.creditoDe(id));
                model.addAttribute("contatos", perfil.contatosDe(id));
            }
        }
        return "fragmentos/perfil-pessoa";
    }

    @PostMapping("/{id}/identificacao")
    public String salvarIdentificacao(@PathVariable UUID id,
                                      @RequestParam String nome,
                                      @RequestParam(required = false) String nomeSocial,
                                      @RequestParam(required = false) String cpf,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimento,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate clienteDesde,
                                      @RequestParam(required = false) String situacao,
                                      @RequestParam(required = false) String whatsapp,
                                      @RequestParam(required = false) String telefone,
                                      @RequestParam(required = false) String email,
                                      @RequestParam(required = false) String observacao,
                                      RedirectAttributes redirect) {
        perfil.salvarIdentificacao(id, nome.trim(), nomeSocial, cpf, dataNascimento, clienteDesde,
                situacao, whatsapp, telefone, email, observacao);
        redirect.addFlashAttribute("aviso", "Dados da pessoa salvos.");
        return "redirect:/clientes";
    }

    // ---------------------------------------------------------------- contatos

    @PostMapping("/{id}/contatos")
    public String salvarContato(@PathVariable UUID id,
                                @RequestParam(required = false) UUID contatoId,
                                @RequestParam(required = false) UUID unidadeId,
                                @RequestParam String nome,
                                @RequestParam(required = false) String telefone,
                                @RequestParam(required = false) String whatsapp,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false, defaultValue = "1") int prioridade,
                                @RequestParam(required = false, defaultValue = "false") boolean aceitaCobranca,
                                @RequestParam(required = false) String melhorHorario,
                                @RequestParam(required = false) String observacao,
                                @RequestParam(required = false, defaultValue = "true") boolean ativo,
                                @RequestParam(required = false) List<UUID> papel,
                                RedirectAttributes redirect) {
        perfil.salvarContato(contatoId, id, unidadeId, nome.trim(), telefone, whatsapp, email,
                prioridade, aceitaCobranca, melhorHorario, observacao, ativo, papel);
        redirect.addFlashAttribute("aviso", "Contato salvo.");
        return "redirect:/clientes";
    }

    // --------------------------------------------------------------- contratos

    @PostMapping("/{id}/contratos")
    public String salvarContrato(@PathVariable UUID id,
                                 @RequestParam(required = false) UUID contratoId,
                                 @RequestParam(required = false) UUID unidadeId,
                                 @RequestParam(required = false) String numero,
                                 @RequestParam(required = false) String descricao,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                 @RequestParam(required = false) BigDecimal valor,
                                 @RequestParam(required = false) String indiceReajuste,
                                 @RequestParam(required = false) Integer mesReajuste,
                                 @RequestParam(required = false) Integer diaVencimento,
                                 @RequestParam(required = false) String periodicidade,
                                 @RequestParam(required = false) String responsavel,
                                 @RequestParam(required = false, defaultValue = "ATIVO") String situacao,
                                 @RequestParam(required = false) String observacao,
                                 RedirectAttributes redirect) {
        perfil.salvarContrato(contratoId, id, unidadeId, numero, descricao, inicio, fim, valor,
                indiceReajuste, mesReajuste, diaVencimento, periodicidade, responsavel,
                situacao, observacao);
        redirect.addFlashAttribute("aviso", "Contrato salvo.");
        return "redirect:/clientes";
    }

    @PostMapping("/contratos/{contratoId}/servicos")
    public String adicionarServico(@PathVariable UUID contratoId,
                                   @RequestParam(required = false) UUID servicoId,
                                   @RequestParam(required = false) String descricao,
                                   @RequestParam(required = false) BigDecimal quantidade,
                                   @RequestParam(required = false) BigDecimal valor,
                                   RedirectAttributes redirect) {
        perfil.adicionarServicoAoContrato(contratoId, servicoId, descricao, quantidade, valor);
        redirect.addFlashAttribute("aviso", "Serviço incluído no contrato.");
        return "redirect:/clientes";
    }

    // ----------------------------------------------------------------- credito

    @PostMapping("/{id}/credito")
    public String salvarCredito(@PathVariable UUID id,
                                @RequestParam(required = false) String classificacao,
                                @RequestParam(required = false) BigDecimal limite,
                                @RequestParam(required = false, defaultValue = "false") boolean bloqueado,
                                @RequestParam(required = false) String motivo,
                                @RequestParam(required = false) String observacao,
                                RedirectAttributes redirect) {
        perfil.salvarCredito(id, classificacao, limite, bloqueado, motivo, observacao);
        redirect.addFlashAttribute("aviso", "Análise de crédito salva.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/restricoes")
    public String registrarRestricao(@PathVariable UUID id,
                                     @RequestParam String tipo,
                                     @RequestParam(required = false) String origem,
                                     @RequestParam(required = false) BigDecimal valor,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                     @RequestParam(required = false) String observacao,
                                     RedirectAttributes redirect) {
        perfil.registrarRestricao(id, tipo, origem, valor, data, observacao);
        redirect.addFlashAttribute("aviso", "Restrição registrada.");
        return "redirect:/clientes";
    }

    // --------------------------------------------------------------- historico

    @PostMapping("/{id}/interacoes")
    public String registrarInteracao(@PathVariable UUID id,
                                     @RequestParam(required = false) UUID unidadeId,
                                     @RequestParam String tipo,
                                     @RequestParam(required = false) String descricao,
                                     @RequestParam(required = false) BigDecimal valor,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPrometida,
                                     @RequestParam(required = false) String canal,
                                     RedirectAttributes redirect) {
        perfil.registrarInteracao(id, unidadeId, tipo, descricao, valor, dataPrometida, canal);
        redirect.addFlashAttribute("aviso", "Registro guardado no histórico.");
        return "redirect:/clientes";
    }

    @PostMapping("/interacoes/{interacaoId}/baixar")
    public String baixarPromessa(@PathVariable UUID interacaoId,
                                 @RequestParam(defaultValue = "true") boolean cumprida,
                                 RedirectAttributes redirect) {
        perfil.baixarPromessa(interacaoId, cumprida);
        redirect.addFlashAttribute("aviso",
                cumprida ? "Promessa marcada como cumprida." : "Promessa marcada como quebrada.");
        return "redirect:/clientes";
    }

    // -------------------------------------------------------------- documentos

    @PostMapping("/{id}/documentos")
    public String anexar(@PathVariable UUID id,
                         @RequestParam MultipartFile arquivo,
                         @RequestParam(required = false) UUID unidadeId,
                         @RequestParam(required = false) UUID contratoId,
                         @RequestParam String tipo,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validade,
                         @RequestParam(required = false) String observacao,
                         RedirectAttributes redirect) {
        documentos.anexar(arquivo, id, unidadeId, contratoId, tipo, validade, observacao);
        redirect.addFlashAttribute("aviso", "Documento anexado.");
        return "redirect:/clientes";
    }

    @GetMapping("/documentos/{documentoId}")
    public ResponseEntity<byte[]> baixar(@PathVariable UUID documentoId) {
        Documento documento = documentos.buscar(documentoId);
        byte[] conteudo = documentos.ler(documentoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + documento.getNomeArquivo() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(conteudo);
    }

    // ------------------------------------------------------------ conformidade

    @PostMapping("/{id}/consentimentos")
    public String registrarConsentimento(@PathVariable UUID id,
                                         @RequestParam(required = false) UUID contatoId,
                                         @RequestParam String canal,
                                         @RequestParam String baseLegal,
                                         @RequestParam String situacao,
                                         @RequestParam(required = false) String origem,
                                         @RequestParam(required = false) String observacao,
                                         RedirectAttributes redirect) {
        perfil.registrarConsentimento(id, contatoId, canal, baseLegal, situacao, origem, observacao);
        redirect.addFlashAttribute("aviso", "Registro de consentimento guardado.");
        return "redirect:/clientes";
    }

    // ----------------------------------------------------- a janela da unidade

    @GetMapping("/unidades/{unidadeId}")
    public String unidade(@PathVariable UUID unidadeId,
                          @RequestParam(required = false, defaultValue = "dados") String aba,
                          Model model) {
        ClienteEspelho unidade = perfil.unidade(unidadeId);
        CarteiraServico.LinhaUnidade linha = carteira.unidade(unidadeId);
        model.addAttribute("unidade", unidade);
        model.addAttribute("linha", linha);
        model.addAttribute("pagador", unidade.getPagador());
        model.addAttribute("aba", aba);
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("consultaDisponivel", consultaDeCnpj.disponivel());

        switch (aba) {
            case "enderecos" -> model.addAttribute("enderecos", perfil.enderecosDe(unidadeId));
            case "cobranca" -> model.addAttribute("cobranca", perfil.cobrancaDe(unidadeId));
            case "contatos" -> {
                model.addAttribute("contatos", perfil.contatosDaUnidade(unidadeId));
                model.addAttribute("papeis", etiquetas.ativasDoEscopo("CONTATO"));
            }
            case "documentos" -> {
                model.addAttribute("documentos", perfil.documentosDaUnidade(unidadeId));
                model.addAttribute("tiposDocumento", Documento.TIPOS);
            }
            case "historico" -> {
                model.addAttribute("historico", perfil.historicoDaUnidade(unidadeId));
                model.addAttribute("trocas", cadastro.trocasDe(unidadeId));
            }
            default -> model.addAttribute("pessoas", cadastro.pessoas());
        }
        return "fragmentos/perfil-unidade";
    }

    @PostMapping("/unidades/{unidadeId}/dados")
    public String salvarDadosDaUnidade(@PathVariable UUID unidadeId,
                                       @RequestParam String razaoSocial,
                                       @RequestParam(required = false) String cnpjCpf,
                                       @RequestParam(required = false) String nomeFantasia,
                                       @RequestParam(required = false) String codigoExterno,
                                       @RequestParam(required = false) String inscricaoEstadual,
                                       @RequestParam(required = false) String inscricaoMunicipal,
                                       @RequestParam(required = false) String regimeTributario,
                                       @RequestParam(required = false) String porte,
                                       @RequestParam(required = false) String cnaePrincipal,
                                       @RequestParam(required = false) String cnaeDescricao,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAbertura,
                                       @RequestParam(required = false) String situacaoCadastral,
                                       @RequestParam(required = false) Boolean optanteSimples,
                                       @RequestParam(required = false) List<String> retencao,
                                       @RequestParam(required = false) String telefone,
                                       @RequestParam(required = false) String email,
                                       @RequestParam(required = false) String responsavel,
                                       RedirectAttributes redirect) {
        ClienteEspelho unidade = perfil.unidade(unidadeId);
        unidade.atualizarCom(razaoSocial.trim(), cnpjCpf, responsavel, telefone, email,
                unidade.isAtivo());
        unidade.setCodigoExterno(codigoExterno);
        unidade.atualizarDadosFiscais(nomeFantasia, inscricaoEstadual, inscricaoMunicipal,
                regimeTributario, porte, cnaePrincipal, cnaeDescricao, dataAbertura,
                situacaoCadastral, optanteSimples,
                retencao == null ? null : String.join(", ", retencao), Fonte.MANUAL);
        redirect.addFlashAttribute("aviso", "Dados da unidade salvos.");
        return "redirect:/clientes";
    }

    @PostMapping("/unidades/{unidadeId}/comercial")
    public String salvarComercial(@PathVariable UUID unidadeId,
                                  @RequestParam(required = false) java.math.BigDecimal percentual,
                                  @RequestParam(required = false) String plataforma,
                                  @RequestParam(required = false, defaultValue = "PADRAO") String tomDeCobranca,
                                  @RequestParam(required = false, defaultValue = "false") boolean aceitaParcelamento,
                                  @RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicioNaCasa,
                                  @RequestParam(required = false) String motivo,
                                  RedirectAttributes redirect) {
        cadastro.salvarComercial(unidadeId, percentual, plataforma, tomDeCobranca,
                aceitaParcelamento, inicioNaCasa, motivo);
        redirect.addFlashAttribute("aviso", "Dados comerciais da unidade salvos.");
        return "redirect:/clientes";
    }

    @PostMapping("/unidades/{unidadeId}/dono")
    public String trocarDono(@PathVariable UUID unidadeId,
                             @RequestParam UUID pagadorId,
                             @RequestParam String motivo,
                             RedirectAttributes redirect) {
        cadastro.trocarDono(unidadeId, pagadorId, motivo);
        redirect.addFlashAttribute("aviso",
                "Unidade passada para a outra pessoa. A troca ficou registrada no histórico.");
        return "redirect:/clientes";
    }

    @PostMapping("/importar")
    public String importarBase(@RequestParam org.springframework.web.multipart.MultipartFile arquivo,
                               RedirectAttributes redirect) {
        CadastroDeUnidades.Importacao resultado = cadastro.importarBase(arquivo);
        redirect.addFlashAttribute("aviso", resultado.pessoasNovas() + " pessoa(s) nova(s), "
                + resultado.unidadesNovas() + " unidade(s) nova(s) e "
                + resultado.jaExistiam() + " que já existiam."
                + (resultado.recusadas().isEmpty() ? ""
                : " Recusadas: " + String.join("; ", resultado.recusadas())));
        return "redirect:/clientes";
    }

    @PostMapping("/unidades/{unidadeId}/consultar-cnpj")
    public String consultarCnpj(@PathVariable UUID unidadeId, RedirectAttributes redirect) {
        consultaDeCnpj.preencher(perfil.unidade(unidadeId));
        redirect.addFlashAttribute("aviso",
                "Dados trazidos da Receita. Confira antes de usar em nota.");
        return "redirect:/clientes";
    }

    @PostMapping("/unidades/{unidadeId}/enderecos")
    public String salvarEndereco(@PathVariable UUID unidadeId,
                                 @RequestParam(required = false) UUID enderecoId,
                                 @RequestParam String tipo,
                                 @RequestParam(required = false) String cep,
                                 @RequestParam(required = false) String logradouro,
                                 @RequestParam(required = false) String numero,
                                 @RequestParam(required = false) String complemento,
                                 @RequestParam(required = false) String bairro,
                                 @RequestParam(required = false) String cidade,
                                 @RequestParam(required = false) String uf,
                                 @RequestParam(required = false) String pais,
                                 RedirectAttributes redirect) {
        perfil.salvarEndereco(enderecoId, unidadeId, tipo, cep, logradouro, numero, complemento,
                bairro, cidade, uf, pais, Fonte.MANUAL);
        redirect.addFlashAttribute("aviso", "Endereço salvo.");
        return "redirect:/clientes";
    }

    @PostMapping("/unidades/{unidadeId}/cobranca")
    public String salvarCobranca(@PathVariable UUID unidadeId,
                                 @RequestParam(required = false) String forma,
                                 @RequestParam(required = false) String chavePix,
                                 @RequestParam(required = false) String banco,
                                 @RequestParam(required = false) String agencia,
                                 @RequestParam(required = false) String conta,
                                 @RequestParam(required = false) String titular,
                                 @RequestParam(required = false) Integer diaVencimento,
                                 @RequestParam(required = false) String periodicidade,
                                 @RequestParam(required = false) String emailCobranca,
                                 @RequestParam(required = false, defaultValue = "false") boolean debitoRecorrente,
                                 @RequestParam(required = false) BigDecimal juros,
                                 @RequestParam(required = false) BigDecimal multa,
                                 @RequestParam(required = false) BigDecimal desconto,
                                 @RequestParam(required = false) Integer carencia,
                                 @RequestParam(required = false) Integer protestarApos,
                                 @RequestParam(required = false) String instrucoes,
                                 RedirectAttributes redirect) {
        perfil.salvarCobranca(unidadeId, forma, chavePix, banco, agencia, conta, titular,
                diaVencimento, periodicidade, emailCobranca, debitoRecorrente, juros, multa,
                desconto, carencia, protestarApos, instrucoes, Fonte.MANUAL);
        redirect.addFlashAttribute("aviso", "Preferência de cobrança salva.");
        return "redirect:/clientes";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/clientes";
    }
}
