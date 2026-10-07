# language: pt
@banco
Funcionalidade: Movimentacao de dinheiro
  O cliente movimenta dinheiro entre contas e solicita credito.
  Valor para a organizacao: operacao que o banco cobra e que, se estiver
  errada, gera prejuizo.

  # ---------------------------------------------------------------------------
  # CENARIO MODELO -- ja escrito, serve de exemplo do vocabulario disponivel.
  # ---------------------------------------------------------------------------
  @transferencia
  Cenario: Transferencia comum entre duas contas
    Dado que a conta "A" tem saldo 1000
    E que a conta "B" tem saldo 100
    Quando a conta "A" transfere 300 para a conta "B"
    Entao a operacao deve ser "aprovada"
    E o saldo da conta "A" deve ser 700
    E o saldo da conta "B" deve ser 400
