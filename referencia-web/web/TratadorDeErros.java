package br.com.itia.financeiro.web;

import br.com.itia.financeiro.servico.ContextoEmpresa;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Erro nao pode virar tela branca. Aqui cada problema conhecido vira um recado
 * em portugues e a pessoa volta para um lugar de onde da para continuar.
 */
@ControllerAdvice
public class TratadorDeErros {

    /** Ninguem entra em tela nenhuma sem ter escolhido a empresa. */
    @ExceptionHandler(ContextoEmpresa.EmpresaNaoEscolhida.class)
    public String semEmpresa() {
        return "redirect:/empresas";
    }

    /** Falta de alcada: a pessoa volta para onde estava, com o recado. */
    @ExceptionHandler(ContextoEmpresa.SemAlcada.class)
    public String semAlcada(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problemaConhecido(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/";
    }
}
