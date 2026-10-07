# LAB : Agile Testing / BDD

Laboratório de **BDD** com **Cucumber**, **Amazon Q Developer** e o
**FIAP Bank**.

```bash
git clone https://github.com/tonanuvem/bddfiap && cd bddfiap
bash lab.sh
```

Um comando sobe o sistema sob teste, baixa as dependências do Maven e mostra
os endereços. 

---

## Do que o laboratório trata

A IA inverteu a economia do BDD. Escrever *step definitions* era a parte cara;
hoje o Amazon Q faz isso em segundos. O que a IA NÃO faz sozinha: decidir **quais cenários devem existir**.

Por isso o aluno decide e especifica, descobrindo que uma regra que ninguém escreveu vira defeito
em produção.

Arquivo `.feature` muda durante as fases:

| Momento | O que o cenário é |
|---|---|
| Fase 2 | uma **especificação** do que o negócio quer |
| Fase 2 | uma **acusação**: o sistema não cumpre a regra |
| Fase 4 | a **definição de pronto** da correção |
| depois | um **teste de regressão**: acusa se o defeito voltar |

---

## As fases

### LAB — individual (≈ 55 min)

| Fase | O que o aluno faz | Tempo |
|---|---|---|
| **1** | Vê o ciclo RED → GREEN. O Amazon Q implementa os *step definitions*. | 12 min |
| **2** | **Caça ao bug**: escreve cenários Gherkin contra o FIAP OTEL Bank, encontra defeitos reais e os confirma na tela do banco. | 25 min |
| **3** | Pede ao Amazon Q que escreva os cenários a partir das **mesmas** regras, e compara com os seus. | 7 min |
| **4** | **Corrige o sistema** e vê os mesmos cenários, sem alterar uma linha deles, ficarem verdes. | 10 min |

### LAB EXTRA — em grupos (≈ 18 min)

| Fase | O que o grupo faz |
|---|---|
| **5** | Aplica a técnica ao **próprio projeto**: produz o *TRABALHO 1* (incluindo Cenários de Testes) a partir do Domain Story do grupo, com apoio do Amazon Q. |

---

## Comandos

```bash
bash lab.sh              # prepara tudo: sobe o banco, aquece o Maven, mostra os endereços
bash lab.sh --subir      # só sobe o FIAP OTEL Bank
bash lab.sh --status     # mostra o que está no ar
bash lab.sh --parar      # derruba o banco

bash lab.sh --fase1      # roda os cenários de pagamento   (tag @venda)
bash lab.sh --fase2      # roda os cenários do banco       (tag @banco)
bash lab.sh --testar     # roda a suíte completa

bash lab.sh --extrair transactions   # traz o código de dentro do contêiner
bash lab.sh --aplicar transactions   # devolve o código corrigido e reinicia
```

Os testes rodam na mesma máquina do banco e usam `localhost` — não é preciso
configurar nada. O IP público que o `lab.sh` mostra é para abrir no **navegador**.
Só defina `BANCO_HOST` se for rodar o Maven de outro computador:

```bash
BANCO_HOST=SEU_IP_EXTERNO bash lab.sh --fase2
```

---

## O sistema sob teste

O **FIAP OTEL Bank** sobe a partir de imagens **já publicadas** no Docker Hub
(`tonanuvem/fiap-bank-*-otel:lab`). Não há build e não há código-fonte da
aplicação neste repositório — de propósito: um teste de aceitação enxerga o
sistema pela porta da frente, como o cliente.

| Endereço | O que é |
|---|---|
| `http://IP:3000` | Interface do banco — para **ver** o defeito na tela (`PORTA_UI`) |
| `http://IP:5000` | BFF, chamado pelo navegador (`PORTA_WEB`) |
| `http://IP:8000` | Login da interface — porta **fixa**, exigida pela UI |
| `http://IP:50051` | API de contas — usada pelos testes |
| `http://IP:50052` | API de transferências — usada pelos testes |
| `http://IP:50053` | API de empréstimos — usada pelos testes |

A instrumentação OpenTelemetry das imagens fica **desligada**
(`OTEL_SDK_DISABLED`): este laboratório é de BDD, não de observabilidade, e sem
isso cada serviço tentaria exportar telemetria para um collector inexistente.

### Como a Fase 4 funciona sem código no repositório

Na Fase 4 o código é **extraído da imagem em execução**, e só nesse momento:

```bash
bash lab.sh --extrair transactions   # cria correcao/transaction.py
# edita (à mão ou com o Amazon Q)
bash lab.sh --aplicar transactions   # devolve ao contêiner e reinicia
bash lab.sh --fase2                  # os MESMOS cenários
```

É a **ordem** que protege o teste, não a ignorância: quando o aluno abre o
código, os cenários já existem e o código não tem mais como influenciar o que
ele decidiu que o sistema deveria fazer. A pasta `correcao/` fica fora do git.

---

## Estrutura

```
docker-compose.yml              FIAP OTEL Bank (imagens publicadas, sem build)
lab.sh                          setup e execução em um comando
pom.xml                         Cucumber 7.22 + JUnit 5 + Jackson

src/test/resources/bddfiap/
    venda.feature               Fase 1 — pagamento (cenário pronto)
    banco.feature               Fase 2 — o aluno escreve os cenários aqui

src/test/java/bddfiap/
    RunCucumberTest.java        runner JUnit 5 (não precisa mexer)
    Banco.java                  cliente REST do banco (infraestrutura)
    BancoSteps.java             vocabulário Gherkin do banco (já implementado)
```

O aluno **não escreve Java na Fase 2**: `BancoSteps.java` já traz o vocabulário
pronto, e o trabalho é escrever cenários.

```gherkin
Dado que a conta "A" tem saldo 1000
Quando a conta "A" transfere 100 para a conta "B"
Quando a conta "A" transfere 100 para ela mesma
Quando a conta "A" solicita um emprestimo de 5000 com juros de 2
Entao a operacao deve ser "aprovada"
Entao o saldo da conta "A" deve ser 900
Entao nenhuma conta pode ter saldo negativo
Entao o total de dinheiro no banco deve continuar o mesmo
```

---

## Para o instrutor

### O que esperar

Os defeitos do banco são **reais e verificados**, não plantados. Rodando o
gabarito (`gabarito/caca-ao-bug.feature`):

| Momento | Resultado |
|---|---|
| Antes da correção | `6 Scenarios (5 failed, 1 passed)` |
| Depois da Fase 4 | `6 Scenarios (2 failed, 4 passed)` |

Os defeitos que os cenários encontram:

| Cenário | O que o sistema faz |
|---|---|
| Transferir **−500** de A para B | `approved: true` — A **ganha** 500, B vai a **−400** |
| A transfere 100 **para ela mesma** | saldo 100 → 200 → 300 → 400: **imprime dinheiro** |
| Empréstimo de **999.999.999** | aprovado na hora, sem análise de crédito |
| Empréstimo de **R$ 0,99** | recusado — a única regra é `valor < 1` |
| Juros de **−50 %** | aprovado |

> O contraste entre as duas linhas de empréstimo (**nega R$ 0,99, aprova R$ 1
> bilhão**) costuma ser o momento em que a turma entende o exercício.

### Os dois cenários que sobram vermelhos na Fase 4

É intencional e é o melhor momento da fase: eles estão no serviço `loan`,
enquanto a correção foi em `transactions`. Mostra que correção tem **escopo de
componente** e que o vermelho restante é um **mapa do que falta**, não um erro.
Ele desemboca na pergunta certa — *qual é o teto de um empréstimo?* — que não
tem resposta técnica. É a pergunta **P3** da folha de regras, que ficou em
aberto na reunião.

### Restaurar o ambiente entre turmas

```bash
bash lab.sh --parar && bash lab.sh --subir
```

Os contêineres são recriados a partir da imagem e os defeitos voltam.

### Gabarito

| Arquivo | Conteúdo |
|---|---|
| `gabarito/Stepdefs.java` | Fase 1, versão GREEN |
| `gabarito/caca-ao-bug.feature` | Fase 2, os cenários que encontram os defeitos |
| `gabarito/CORRECAO.md` | Fase 4, a correção comentada (+ a opcional do `loan`) |

---

## Ligação com o resto do curso

| Momento | Quem escreve o Gherkin | A partir de quê |
|---|---|---|
| **TRABALHO 1**, slide 5 | o grupo, à mão | o Domain Story do slide 4 |
| **LAB 2**, Fase 5 | o grupo, com o Amazon Q | o Domain Story do grupo |
| **LAB 3** (próxima aula) | a IA, sozinha | os requisitos gerados pelo agente anterior |

No LAB 3, o arquivo `2-arquitetura/arquitetura.md` pede ao agente, literalmente,
*"Domain Storytelling (texto)"* e *"Cenários de Teste (BDD — Gherkin) baseado no
Domain Storytelling"*; e `3-implementacao/testes.md` manda o agente seguinte
**"usar os cenários de BDD definidos em Gherkin"**, com Cucumber.

Ou seja: lá a IA produz, em minutos, o Gherkin e os testes de uma solução
inteira, e **o aluno é o revisor**. A pergunta que este laboratório treina —
*"onde está a regra que ninguém escreveu?"* — é o que separa uma revisão de
verdade de um "parece bom" apressado.

O LAB 2 também já instala e autentica o Amazon Q, para o LAB 3 não gastar tempo
de aula com isso.

---

## Requisitos

- **Docker** e **Docker Compose**
- **Java 17 e Maven** — *opcionais*: sem eles o `lab.sh` roda o Maven em
  contêiner, e a saída no terminal é idêntica
- **Amazon Q Developer** (`q`), instalado com:

```bash
cd config && git pull && bash amazon-q-install.sh && cd ..
q login     # escolher: Use for FREE with Build ID
```

### Se alguma porta estiver ocupada

A UI e o BFF são ajustáveis por variável de ambiente:

```bash
PORTA_UI=3001 PORTA_WEB=5001 bash lab.sh
```

Duas ressalvas conhecidas:

- no **macOS**, a porta 5000 é ocupada pelo AirPlay Receiver — use `PORTA_WEB`;
- a porta **8000** (`customer-auth`, o login) **não é ajustável**: a interface do
  banco tem esse endereço fixo no código. Se ela estiver ocupada, libere-a — ou
  siga sem a UI, porque as Fases 1 a 4 só dependem das APIs (50051–50053).
