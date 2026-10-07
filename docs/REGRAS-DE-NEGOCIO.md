# Regras de Negócio

> Este documento é a **saída da conversa** entre a área de negócio, o
> desenvolvimento e o QA (o "Three Amigos"). É tudo o que você tem.

---

## PARTE A — Regras acordadas

Estas regras foram fechadas na reunião. São o contrato.

| # | Regra |
|---|---|
| **R1** | Toda conta nova é aberta com um bônus de boas-vindas de **100**. |
| **R2** | O cliente só consegue transferir se tiver **saldo suficiente**. |
| **R3** | Uma conta **nunca** pode ficar com saldo negativo. O banco não oferece cheque especial. |
| **R4** | Uma transferência **move** dinheiro de uma conta para outra. O banco não cria nem destrói dinheiro numa transferência: se você somar o saldo de todas as contas antes e depois, o total tem que ser o mesmo. |

---

## PARTE B — Perguntas que ficaram abertas

A reunião acabou antes de fechar estes pontos. **Ninguém decidiu.** A área de
negócio disse "implemente o óbvio" e seguiu para a próxima pauta.

É aqui que está o seu trabalho mais importante: para cada pergunta, **decida
qual deveria ser o comportamento correto** e escreva o cenário que verifica
isso. Uma pergunta que ninguém respondeu não pode virar um defeito
em produção.

| # | Pergunta aberta |
|---|---|
| **P1** | O cliente pode transferir um valor **negativo**? O que deveria acontecer? |
| **P2** | O cliente pode transferir dinheiro **para a própria conta**? O que deveria acontecer com o saldo dele? |
| **P3** | O mesmo cliente pode abrir **várias contas** e receber o bônus de boas-vindas em cada uma? |

---

## Vocabulário Gherkin disponível

Exemplos de cenários.

```gherkin
Dado que a conta "A" tem saldo 1000
Quando a conta "A" transfere 100 para a conta "B"
Quando a conta "A" transfere 100 para ela mesma
Quando a conta "A" solicita um emprestimo de 5000 com juros de 2
Entao a operacao deve ser "aprovada"
Entao a operacao deve ser "recusada"
Entao o saldo da conta "A" deve ser 900
Entao nenhuma conta pode ter saldo negativo
Entao o total de dinheiro no banco deve continuar o mesmo
```
