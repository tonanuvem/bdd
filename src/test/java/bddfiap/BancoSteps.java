package bddfiap;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VOCABULARIO DE NEGOCIO DO BANCO  (Fase 2 do laboratorio)
 *
 * Estes passos JA ESTAO IMPLEMENTADOS. O seu trabalho na Fase 2 nao e'
 * escrever Java: e' decidir QUAIS CENARIOS devem existir.
 *
 * Passos disponiveis:
 *
 *   Dado que a conta "A" tem saldo 1000
 *   Quando a conta "A" transfere 100 para a conta "B"
 *   Quando a conta "A" transfere 100 para ela mesma
 *   Quando a conta "A" solicita um emprestimo de 5000 com juros de 2
 *   Entao a operacao deve ser "aprovada"        (ou "recusada")
 *   Entao o saldo da conta "A" deve ser 900
 *   Entao nenhuma conta pode ter saldo negativo
 *   Entao o total de dinheiro no banco deve continuar o mesmo
 */
public class BancoSteps {

    /** Apelido do cenario ("A", "B", ...) -> numero real da conta no banco. */
    private final Map<String, String> contas = new LinkedHashMap<>();

    /** Apelido -> e-mail usado para abrir a conta (o emprestimo exige o e-mail). */
    private final Map<String, String> emails = new LinkedHashMap<>();

    /** Soma dos saldos das contas do cenario, no instante inicial. */
    private Double totalInicial = null;

    /** Resultado da ultima operacao de negocio (transferencia ou emprestimo). */
    private Banco.Resultado ultimoResultado;

    // ------------------------------------------------------------ contexto

    @Dado("que a conta {string} tem saldo {bigdecimal}")
    public void que_a_conta_tem_saldo(String apelido, java.math.BigDecimal saldoDesejado) {
        // Cada execucao usa e-mails novos: dois alunos rodando ao mesmo tempo,
        // ou a mesma suite rodando duas vezes, nunca disputam a mesma conta.
        String email = "lab-" + UUID.randomUUID().toString().substring(0, 8) + "@fiap.com.br";
        String numero = Banco.abrirConta(email, "Checking", "Aluno FIAP");

        contas.put(apelido, numero);
        emails.put(apelido, email);

        // A aplicacao abre toda conta nova com saldo 100. Para chegar ao saldo
        // pedido pelo cenario, a diferenca entra como emprestimo -- usando
        // apenas a API publica, sem tocar no banco de dados.
        double atual = Banco.saldo(numero);
        double falta = saldoDesejado.doubleValue() - atual;

        if (falta > 0) {
            Banco.pedirEmprestimo(email, numero, String.valueOf(falta), "0");
        }

        double conferido = Banco.saldo(numero);
        if (Math.abs(conferido - saldoDesejado.doubleValue()) > 0.001) {
            throw new IllegalStateException(String.format(
                    "Nao foi possivel preparar a conta %s com saldo %s (ficou %s). "
                  + "Saldos abaixo de 100 nao sao montaveis: a aplicacao abre "
                  + "toda conta nova com 100.", apelido, saldoDesejado, conferido));
        }

        totalInicial = totalAtual();
    }

    // -------------------------------------------------------- operacoes

    @Quando("a conta {string} transfere {bigdecimal} para a conta {string}")
    public void a_conta_transfere_para_a_conta(String de, java.math.BigDecimal valor, String para) {
        ultimoResultado = Banco.transferir(
                conta(de), conta(para), valor.toPlainString(), "cenario de teste");
    }

    @Quando("a conta {string} transfere {bigdecimal} para ela mesma")
    public void a_conta_transfere_para_ela_mesma(String apelido, java.math.BigDecimal valor) {
        ultimoResultado = Banco.transferir(
                conta(apelido), conta(apelido), valor.toPlainString(), "cenario de teste");
    }

    @Quando("a conta {string} solicita um emprestimo de {bigdecimal} com juros de {bigdecimal}")
    public void a_conta_solicita_um_emprestimo(
            String apelido, java.math.BigDecimal valor, java.math.BigDecimal juros) {
        ultimoResultado = Banco.pedirEmprestimo(
                email(apelido), conta(apelido), valor.toPlainString(), juros.toPlainString());
    }

    // ------------------------------------------------------- verificacoes

    @Entao("a operacao deve ser {string}")
    public void a_operacao_deve_ser(String esperado) {
        exigeOperacao();

        if ("aprovada".equalsIgnoreCase(esperado)) {
            assertTrue(ultimoResultado.aprovada(),
                    "Esperava que o banco APROVASSE a operacao, mas ele recusou com a mensagem: \""
                  + ultimoResultado.mensagem() + "\"");
        } else if ("recusada".equalsIgnoreCase(esperado)) {
            assertFalse(ultimoResultado.aprovada(),
                    "Esperava que o banco RECUSASSE a operacao, mas ele APROVOU com a mensagem: \""
                  + ultimoResultado.mensagem() + "\"");
        } else {
            throw new IllegalArgumentException(
                    "Use \"aprovada\" ou \"recusada\". Recebi: \"" + esperado + "\"");
        }
    }

    @Entao("o saldo da conta {string} deve ser {bigdecimal}")
    public void o_saldo_da_conta_deve_ser(String apelido, java.math.BigDecimal esperado) {
        assertEquals(esperado.doubleValue(), Banco.saldo(conta(apelido)), 0.001,
                "O saldo da conta " + apelido + " nao e' o esperado");
    }

    @Entao("nenhuma conta pode ter saldo negativo")
    public void nenhuma_conta_pode_ter_saldo_negativo() {
        for (Map.Entry<String, String> e : contas.entrySet()) {
            double s = Banco.saldo(e.getValue());
            assertTrue(s >= 0, "A conta " + e.getKey() + " ficou com saldo NEGATIVO: " + s);
        }
    }

    @Entao("o total de dinheiro no banco deve continuar o mesmo")
    public void o_total_de_dinheiro_no_banco_deve_continuar_o_mesmo() {
        if (totalInicial == null) {
            throw new IllegalStateException(
                    "Este passo precisa de pelo menos um Dado \"que a conta ... tem saldo ...\".");
        }
        assertEquals(totalInicial, totalAtual(), 0.001,
                "O total de dinheiro nas contas do cenario MUDOU. "
              + "Uma transferencia move dinheiro, nunca cria nem destroi.");
    }

    // ------------------------------------------------------------ apoio

    private double totalAtual() {
        double t = 0;
        for (String numero : contas.values()) {
            t += Banco.saldo(numero);
        }
        return t;
    }

    private String conta(String apelido) {
        String n = contas.get(apelido);
        if (n == null) {
            throw new IllegalArgumentException("A conta \"" + apelido + "\" nao foi criada neste cenario. "
                  + "Falta um Dado \"que a conta \"" + apelido + "\" tem saldo ...\"?");
        }
        return n;
    }

    private String email(String apelido) {
        String e = emails.get(apelido);
        if (e == null) {
            throw new IllegalArgumentException("A conta \"" + apelido + "\" nao foi criada neste cenario.");
        }
        return e;
    }

    private void exigeOperacao() {
        if (ultimoResultado == null) {
            throw new IllegalStateException(
                    "Nenhuma operacao foi executada ainda. Falta um passo Quando?");
        }
    }
}
