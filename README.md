# bddfiap — LAB 2: Agile Testing / BDD

Laboratório de BDD com **Cucumber 7**, **Amazon Q Developer** e o
**FIAP OTEL Bank**.

```bash
git clone https://github.com/tonanuvem/bddfiap && cd bddfiap
bash lab.sh
```

Um comando sobe o banco, baixa as dependências do Maven e mostra os
endereços. Não há `mvn archetype:generate` com perguntas interativas e não
há código Java para copiar de dentro de um documento.

---

## As fases

| Fase | O que o aluno faz | Tempo |
|---|---|---|
| **1** | Vê o ciclo RED → GREEN. O Amazon Q implementa os step definitions. | 12 min |
| **2** | **Caça ao bug**: escreve cenários Gherkin contra o FIAP OTEL Bank e encontra defeitos reais. | 28 min |
| **3** | Pede ao Amazon Q que escreva os cenários a partir das mesmas regras, e compara com os seus. | 7 min |
| **4** | **Corrige o sistema** e vê os mesmos cenários, sem alterá-los, ficarem verdes. | 10 min |
| **5** | *(LAB EXTRA, em grupos)* Aplica a técnica ao projeto do **Trabalho 1**. | 18 min |

---

## Comandos

```bash
bash lab.sh            # prepara tudo (banco + Maven) e mostra os endereços
bash lab.sh --fase1    # roda os cenários de pagamento   (tag @venda)
bash lab.sh --fase2    # roda os cenários do banco       (tag @banco)
bash lab.sh --status   # mostra o que está no ar
bash lab.sh --parar    # derruba o banco

bash lab.sh --extrair transactions   # traz o código de dentro do contêiner
bash lab.sh --aplicar transactions   # devolve o código corrigido e reinicia
```

O par `--extrair` / `--aplicar` é a Fase 4: o código-fonte **não está neste
repositório**, ele é extraído da imagem em execução no momento da correção.
Assim a Fase 2 continua sendo um teste de caixa-preta, e nada se perde ao
reiniciar — `bash lab.sh --parar && bash lab.sh --subir` recria os contêineres
a partir da imagem e os defeitos voltam.

Se o laboratório estiver numa máquina remota, informe o IP:

```bash
BANCO_HOST=SEU_IP_EXTERNO bash lab.sh --fase2
```

---

## Estrutura

```
docker-compose.yml              FIAP OTEL Bank (imagens publicadas, sem build)
lab.sh                          setup e execução em um comando
pom.xml                         Cucumber 7.22 + JUnit 5 + Jackson

docs/REGRAS-DE-NEGOCIO.md       a folha de regras da Fase 2  <-- comece por aqui
docs/PROMPTS-AMAZON-Q.md        os prompts das Fases 1, 3, 4 e 5
correcao/                       criada na Fase 4 (fora do git)

src/test/resources/bddfiap/
    venda.feature               Fase 1 — pagamento (cenário pronto)
    banco.feature               Fase 2 — você escreve os cenários aqui

src/test/java/bddfiap/
    RunCucumberTest.java        runner (não precisa mexer)
    Banco.java                  cliente REST do banco (infraestrutura)
    BancoSteps.java             vocabulário Gherkin do banco (já implementado)

gabarito/                       para o instrutor
```

---

## O sistema sob teste

O **FIAP OTEL Bank** sobe a partir de imagens já publicadas no Docker Hub.
**O código-fonte da aplicação não está neste repositório, de propósito:**
um teste de aceitação enxerga o sistema pela porta da frente, como o cliente.
Na Fase 4, depois que os cenários já existem, o código é extraído da imagem em
execução com `lab.sh --extrair` — a ordem é que protege o teste.

| Endereço | O que é |
|---|---|
| `http://IP:3000` | Interface do banco — para **ver** o defeito na tela |
| `http://IP:50051` | API de contas |
| `http://IP:50052` | API de transferências |
| `http://IP:50053` | API de empréstimos |

A instrumentação OpenTelemetry das imagens fica desligada: este laboratório
é de BDD, não de observabilidade.

---

## Requisitos

- Docker e Docker Compose
- Java 17 e Maven — **opcionais**: sem eles, o `lab.sh` roda o Maven em contêiner
- Amazon Q Developer (`q`), instalado com `bash config/amazon-q-install.sh`
