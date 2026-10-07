package bddfiap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Acesso a API REST do FIAP OTEL Bank.
 *
 * Esta classe e' INFRAESTRUTURA do laboratorio: o aluno nao precisa alterar
 * nada aqui. Ela existe para que a Fase 2 do lab seja gasta escrevendo
 * CENARIOS (Gherkin), e nao encanamento HTTP.
 *
 * Endereco dos servicos: por padrao localhost. Num ambiente remoto, use
 *   mvn test -Dbanco.host=SEU_IP_EXTERNO
 */
class Banco {

    private static final String HOST = System.getProperty("banco.host", "localhost");

    private static final String ACCOUNTS     = "http://" + HOST + ":50051";
    private static final String TRANSACTIONS = "http://" + HOST + ":50052";
    private static final String LOAN         = "http://" + HOST + ":50053";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper JSON = new ObjectMapper();

    private Banco() {
    }

    /** POST com corpo JSON. Devolve a resposta already parseada. */
    private static JsonNode post(String base, String caminho, String corpoJson) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + caminho))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoJson))
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 500) {
                throw new AssertionError(
                        "O banco respondeu HTTP " + resp.statusCode() + " em " + caminho
                      + ".\nIsto e' uma falha do servidor, nao uma regra de negocio."
                      + "\nResposta: " + resumo(resp.body()));
            }
            return JSON.readTree(resp.body());

        } catch (AssertionError e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new IllegalStateException(
                    "Nao consegui falar com o banco em " + base + "."
                  + "\nO banco esta no ar? Rode:  bash lab.sh --subir", e);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao chamar " + base + caminho, e);
        }
    }

    private static String resumo(String corpo) {
        if (corpo == null) {
            return "(vazia)";
        }
        String s = corpo.replaceAll("\\s+", " ").trim();
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    // ------------------------------------------------------------- contas

    /** Abre uma conta e devolve o numero (IBAN...). */
    static String abrirConta(String email, String tipo, String nome) {
        post(ACCOUNTS, "/create-account", String.format(
                "{\"email_id\":\"%s\",\"account_type\":\"%s\",\"address\":\"Av Paulista 1106\","
              + "\"govt_id_number\":\"000000000\",\"government_id_type\":\"RG\",\"name\":\"%s\"}",
                email, tipo, nome));

        JsonNode contas = post(ACCOUNTS, "/get-all-accounts",
                String.format("{\"email_id\":\"%s\"}", email));

        for (JsonNode c : contas) {
            if (tipo.equals(c.path("account_type").asText())) {
                return c.path("account_number").asText();
            }
        }
        throw new IllegalStateException("Conta recem-criada nao foi encontrada: " + email + " / " + tipo);
    }

    /** Saldo atual da conta. */
    static double saldo(String numeroConta) {
        JsonNode r = post(ACCOUNTS, "/account-detail",
                String.format("{\"account_number\":\"%s\"}", numeroConta));

        if (!r.has("balance")) {
            throw new IllegalStateException("Conta nao encontrada: " + numeroConta);
        }
        return r.path("balance").asDouble();
    }

    // ------------------------------------------------------ transferencia

    /** Resultado de uma operacao de negocio: aprovada ou recusada, com a mensagem do banco. */
    record Resultado(boolean aprovada, String mensagem) {
    }

    static Resultado transferir(String de, String para, String valor, String motivo) {
        JsonNode r = post(TRANSACTIONS, "/transfer", String.format(
                "{\"sender_account_number\":\"%s\",\"receiver_account_number\":\"%s\","
              + "\"amount\":%s,\"reason\":\"%s\"}", de, para, valor, motivo));

        return new Resultado(r.path("approved").asBoolean(), r.path("message").asText());
    }

    // --------------------------------------------------------- emprestimo

    static Resultado pedirEmprestimo(String email, String numeroConta, String valor, String juros) {
        JsonNode r = post(LOAN, "/loan/request", String.format(
                "{\"name\":\"Aluno FIAP\",\"email\":\"%s\",\"account_type\":\"Checking\","
              + "\"account_number\":\"%s\",\"govt_id_type\":\"RG\",\"govt_id_number\":\"000000000\","
              + "\"loan_type\":\"Personal\",\"loan_amount\":%s,\"interest_rate\":%s,"
              + "\"time_period\":\"12\"}", email, numeroConta, valor, juros));

        return new Resultado(r.path("approved").asBoolean(), r.path("message").asText());
    }
}
