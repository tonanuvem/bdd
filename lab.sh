#!/usr/bin/env bash
#
# FIAP - Agile Testing / BDD
#
# Prepara o laboratorio inteiro com UM comando.
#
#   bash lab.sh              sobe o banco, aquece o Maven e mostra os enderecos
#   bash lab.sh --subir      so sobe o FIAP OTEL Bank
#   bash lab.sh --parar      para o banco
#   bash lab.sh --status     mostra o que esta no ar
#   bash lab.sh --testar     roda a suite completa
#
#   bash lab.sh --extrair <servico>   tira o codigo-fonte de dentro do conteiner
#   bash lab.sh --aplicar <servico>   devolve o codigo corrigido e reinicia
#
# O que ele evita de trabalho manual:
#   - nao ha `mvn archetype:generate` com 5 perguntas interativas
#   - nao ha copiar e colar codigo Java de dentro de um documento Word
#   - as dependencias do Maven sao baixadas UMA vez, antes da aula comecar

set -uo pipefail

cd "$(dirname "$0")"

PORTA_WEB="${PORTA_WEB:-5000}"
export PORTA_WEB

ok()    { printf '  \033[32m[OK]\033[0m   %s\n' "$1"; }
erro()  { printf '  \033[31m[ERRO]\033[0m %s\n' "$1"; }
aviso() { printf '  \033[33m[!]\033[0m    %s\n' "$1"; }
titulo(){ printf '\n\033[1m%s\033[0m\n' "$1"; }

ACAO="tudo"
case "${1:-}" in
    --subir)  ACAO="subir" ;;
    --parar)  ACAO="parar" ;;
    --status) ACAO="status" ;;
    --testar) ACAO="testar" ;;
    --extrair) ACAO="extrair"; SERVICO="${2:-}" ;;
    --aplicar) ACAO="aplicar"; SERVICO="${2:-}" ;;
    -h|--help) sed -n '3,20p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    "") ;;
    *) erro "opcao desconhecida: $1 (use --help)"; exit 1 ;;
esac

# ---------------------------------------------------------------- pre-requisitos

verificar_docker() {
    command -v docker >/dev/null 2>&1 || {
        erro "Docker nao encontrado nesta maquina."
        exit 1
    }
    docker info >/dev/null 2>&1 || {
        erro "O Docker esta instalado mas o daemon nao responde."
        echo "         Inicie com: sudo systemctl start docker"
        echo "         Falta de permissao? sudo usermod -aG docker \$USER"
        echo "         (nesse caso, saia e entre de novo na sessao)"
        exit 1
    }
    COMPOSE="docker compose"
    $COMPOSE version >/dev/null 2>&1 || COMPOSE="docker-compose"
    $COMPOSE version >/dev/null 2>&1 || {
        erro "Nem 'docker compose' nem 'docker-compose' funcionam aqui."
        exit 1
    }
}

verificar_maven() {
    if command -v mvn >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
        MVN="mvn"
        ok "Maven e Java encontrados na maquina"
    else
        # Sem Java instalado, roda o Maven em contêiner. O aluno nao precisa
        # instalar JDK nenhum -- e a saida no terminal e' identica.
        MVN="docker run --rm -v $PWD:/proj -w /proj -v $HOME/.m2:/root/.m2 \
             --network host maven:3.9-eclipse-temurin-17 mvn"
        aviso "Java/Maven nao encontrados -- vou usar o Maven em contêiner"
    fi
}

# --------------------------------------------------------------------- acoes

subir_banco() {
    titulo "SUBINDO O FIAP OTEL BANK"
    echo "  Primeira vez: o download das imagens leva alguns minutos."
    $COMPOSE up -d || { erro "falha ao subir os contêineres"; exit 1; }

    printf '\n  Aguardando os servicos responderem'
    for _ in $(seq 1 40); do
        if curl -s -o /dev/null -X POST "http://localhost:50051/get-all-accounts" \
                -H 'Content-Type: application/json' -d '{"email_id":"ping@fiap"}' 2>/dev/null; then
            printf '\n'; ok "API de contas no ar (50051)"
            return 0
        fi
        printf '.'; sleep 3
    done
    printf '\n'
    aviso "os servicos demoraram mais que o esperado -- veja: $COMPOSE logs"
}

aquecer_maven() {
    titulo "BAIXANDO AS DEPENDENCIAS DO MAVEN"
    echo "  Feito uma vez agora, para o 'mvn test' da aula ser instantaneo."
    if $MVN -q -B dependency:go-offline 2>/dev/null; then
        ok "dependencias em cache"
    else
        aviso "nao consegui pre-baixar tudo (o 'mvn test' baixa o que faltar)"
    fi
}

mostrar_enderecos() {
    bash usuario.sh
    
    local ip
    ip="$(curl -s --max-time 3 http://checkip.amazonaws.com 2>/dev/null)"
    [ -z "$ip" ] && ip="localhost"

    titulo "LABORATORIO PRONTO"
    cat <<FIM
  
  Acessar Frontend do APP Exemplo FIAP Bank :
  
      http://$ip:3000

  LOGIN DE TESTE: 

      Email : teste@teste.com
      Senha : Teste@123

FIM
}

# Caminho do arquivo principal de cada servico DENTRO da imagem publicada.
caminho_no_conteiner() {
    case "$1" in
        transactions) echo "/service/transactions/transaction.py" ;;
        loan)         echo "/service/loan/loan.py" ;;
        accounts)     echo "/service/accounts/accounts.py" ;;
        *) erro "servico desconhecido: '$1'"
           echo "         use: transactions, loan ou accounts"
           exit 1 ;;
    esac
}

extrair_fonte() {
    [ -n "${SERVICO:-}" ] || { erro "informe o servico: bash lab.sh --extrair transactions"; exit 1; }
    local dentro; dentro="$(caminho_no_conteiner "$SERVICO")"
    local arquivo; arquivo="$(basename "$dentro")"

    titulo "EXTRAINDO O CODIGO-FONTE DE '$SERVICO'"
    mkdir -p correcao
    $COMPOSE cp "$SERVICO:$dentro" "correcao/$arquivo" >/dev/null 2>&1 || {
        erro "nao consegui copiar de $SERVICO. O banco esta no ar? (bash lab.sh --status)"
        exit 1
    }
    ok "correcao/$arquivo"
    echo ""
    echo "  Ate agora voce testou a aplicacao como CAIXA-PRETA, e foi assim"
    echo "  que escreveu os cenarios. Agora que os cenarios ja existem, pode"
    echo "  abrir o codigo: ele nao tem mais como influenciar o que voce"
    echo "  decidiu que o sistema DEVERIA fazer."
    echo ""
    echo "  Edite correcao/$arquivo e depois rode:"
    echo "      bash lab.sh --aplicar $SERVICO"
}

aplicar_fonte() {
    [ -n "${SERVICO:-}" ] || { erro "informe o servico: bash lab.sh --aplicar transactions"; exit 1; }
    local dentro; dentro="$(caminho_no_conteiner "$SERVICO")"
    local arquivo; arquivo="$(basename "$dentro")"

    [ -f "correcao/$arquivo" ] || {
        erro "correcao/$arquivo nao existe. Rode antes: bash lab.sh --extrair $SERVICO"
        exit 1
    }

    titulo "APLICANDO A CORRECAO EM '$SERVICO'"
    # Erro de sintaxe aqui derruba o conteiner e o teste vira 'connection
    # refused' -- uma mensagem que nao ajuda ninguem. Melhor avisar antes.
    if command -v python3 >/dev/null 2>&1; then
        python3 -m py_compile "correcao/$arquivo" 2>/dev/null \
            && ok "sintaxe Python validada" \
            || { erro "erro de sintaxe em correcao/$arquivo -- corrija antes de aplicar"
                 python3 -m py_compile "correcao/$arquivo"; exit 1; }
    fi

    $COMPOSE cp "correcao/$arquivo" "$SERVICO:$dentro" >/dev/null || {
        erro "falha ao copiar para o conteiner"; exit 1; }
    ok "codigo enviado para o conteiner"

    $COMPOSE restart "$SERVICO" >/dev/null 2>&1 || { erro "falha ao reiniciar"; exit 1; }
    printf '  aguardando o servico voltar'
    for _ in $(seq 1 20); do printf '.'; sleep 1; done
    printf '\n'
    ok "'$SERVICO' reiniciado com a sua correcao"
    echo ""
    echo "  Agora rode os testes de novo:  bash lab.sh --testar"
    echo "  Para desfazer tudo:            bash lab.sh --parar && bash lab.sh --subir"
}

rodar_testes() {
    local tags="${1:-}"
    local ip="${BANCO_HOST:-localhost}"
    titulo "EXECUTANDO OS TESTES"
    if [ -n "$tags" ]; then
        $MVN -B test -Dcucumber.filter.tags="$tags" -Dbanco.host="$ip"
    else
        $MVN -B test -Dbanco.host="$ip"
    fi
}

# --------------------------------------------------------------------- main

case "$ACAO" in
    parar)
        verificar_docker
        titulo "PARANDO O FIAP OTEL BANK"
        $COMPOSE down && ok "contêineres removidos"
        ;;
    status)
        verificar_docker
        titulo "CONTEINERES DO LABORATORIO"
        $COMPOSE ps
        ;;
    subir)
        verificar_docker
        subir_banco
        mostrar_enderecos
        ;;
    testar)
        verificar_maven
        rodar_testes ""
        ;;
    extrair)
        verificar_docker
        extrair_fonte
        ;;
    aplicar)
        verificar_docker
        aplicar_fonte
        ;;
    tudo)
        verificar_docker
        verificar_maven
        subir_banco
        aquecer_maven
        mostrar_enderecos
        ;;
esac
