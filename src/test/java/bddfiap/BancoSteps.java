package bddfiap;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BancoSteps {

    private final Map<String, String> contas = new HashMap<>();

    private Banco.Resultado resultado;

    @Dado("que a conta {string} tem saldo {bigdecimal}")
    public void que_a_conta_tem_saldo(String nome, BigDecimal saldo) {

        String email = "teste-" + UUID.randomUUID() + "@fiap.com.br";

        String numero = Banco.abrirConta(
                email,
                "Checking",
                "Aluno FIAP"
        );

        contas.put(nome, numero);

        // Toda conta começa com 100.
        // Para preparar um saldo maior, usamos a API pública de empréstimo.
        double atual = Banco.saldo(numero);
        double diferenca = saldo.doubleValue() - atual;

        if (diferenca > 0) {
            Banco.pedirEmprestimo(
                    email,
                    numero,
                    String.valueOf(diferenca),
                    "0"
            );
        }

        assertEquals(
                saldo.doubleValue(),
                Banco.saldo(numero),
                0.001,
                "Não foi possível preparar o saldo da conta " + nome
        );
    }

    @Quando("a conta {string} transfere {bigdecimal} para a conta {string}")
    public void a_conta_transfere_para_a_conta(
            String origem,
            BigDecimal valor,
            String destino) {

        resultado = Banco.transferir(
                conta(origem),
                conta(destino),
                valor.toPlainString(),
                "cenario de teste"
        );
    }

    @Entao("a operacao deve ser {string}")
    public void a_operacao_deve_ser(String esperado) {

        boolean aprovada = resultado.aprovada();

        assertEquals(
                "aprovada".equalsIgnoreCase(esperado),
                aprovada,
                "Esperava operação " + esperado +
                ", mas o banco retornou: " + resultado.mensagem()
        );
    }

    @Entao("o saldo da conta {string} deve ser {bigdecimal}")
    public void o_saldo_da_conta_deve_ser(
            String nome,
            BigDecimal esperado) {

        assertEquals(
                esperado.doubleValue(),
                Banco.saldo(conta(nome)),
                0.001,
                "Saldo incorreto da conta " + nome
        );
    }

    private String conta(String nome) {
        String numero = contas.get(nome);

        if (numero == null) {
            throw new IllegalArgumentException(
                    "Conta \"" + nome + "\" não foi criada."
            );
        }

        return numero;
    }
}
