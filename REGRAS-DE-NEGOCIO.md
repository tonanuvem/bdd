# FIAP OTEL Bank — Regras de Negócio

> Este documento é a **saída da conversa** entre a área de negócio, o
> desenvolvimento e o QA (o "Three Amigos"). É tudo o que você tem.
>
> **Não leia o código da aplicação.** Um teste de aceitação enxerga o sistema
> pela porta da frente, como o cliente. Se você for olhar a implementação,
> vai escrever um teste que confirma o que o código faz — e não o que o
> negócio pediu.

---

## PARTE A — Regras acordadas

Estas regras foram fechadas na reunião. São o contrato.

| # | Regra |
|---|---|
| **R1** | Toda conta nova é aberta com um bônus de boas-vindas de **100**. |
| **R2** | O cliente só consegue transferir se tiver **saldo suficiente**. |
| **R3** | Uma conta **nunca** pode ficar com saldo negativo. O banco não oferece cheque especial. |
| **R4** | Uma transferência **move** dinheiro de uma conta para outra. O banco não cria nem destrói dinheiro numa transferência: se você somar o saldo de todas as contas antes e depois, o total tem que ser o mesmo. |
| **R5** | Um empréstimo aprovado é **creditado na conta** do cliente e aparece no extrato. |
| **R6** | O empréstimo é **recusado** quando o valor pedido é muito pequeno para valer a operação. |

---

## PARTE B — Perguntas que ficaram abertas

A reunião acabou antes de fechar estes pontos. **Ninguém decidiu.** A área de
negócio disse "é óbvio" e seguiu para a próxima pauta.

É aqui que está o seu trabalho mais importante: para cada pergunta, **decida
qual deveria ser o comportamento correto** e escreva o cenário que verifica
isso. Uma pergunta que ninguém respondeu na conversa costuma virar um defeito
em produção — e o seu cenário é o que prova.

| # | Pergunta aberta |
|---|---|
| **P1** | O cliente pode transferir um valor **negativo**? O que deveria acontecer? |
| **P2** | O cliente pode transferir dinheiro **para a própria conta**? O que deveria acontecer com o saldo dele? |
| **P3** | Existe **teto** para o empréstimo? Um cliente com saldo de 1.000 pode pedir 999.999.999? |
| **P4** | A **taxa de juros** pode ser negativa? Quem pagaria quem, nesse caso? |
| **P5** | O mesmo cliente pode abrir **várias contas** e receber o bônus de boas-vindas (R1) em cada uma? |

---

## Vocabulário Gherkin disponível

Estes passos **já estão implementados** em `BancoSteps.java`. Você não precisa
escrever Java nesta fase: escreva cenários.

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

Observações:

- Os apelidos `"A"`, `"B"`, ... são contas novas, criadas na hora. Cada
  execução usa contas diferentes, então o seu teste nunca atrapalha o do colega.
- Valores podem ser negativos e podem ter decimais: `-500`, `0.99`.
- `Dado que a conta "A" tem saldo 1000` não consegue montar saldos **abaixo de
  100**, porque a aplicação abre toda conta nova com 100 (R1).

---

## Como ler o resultado

Quando um cenário **falha**, leia a mensagem antes de mexer em qualquer coisa.
Ela diz em linguagem de negócio o que o banco fez:

```
Esperava que o banco RECUSASSE a operacao,
mas ele APROVOU com a mensagem: "Transaction is Successful."
```

Agora decida qual dos dois está errado:

- **O sistema está errado** → você encontrou um defeito. O seu cenário é a prova.
- **O seu cenário está errado** → você entendeu mal a regra. Corrija o cenário.

**Vermelho não é sinônimo de bug encontrado.** Saber distinguir os dois casos é
a competência que esta fase treina.
