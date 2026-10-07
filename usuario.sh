# ==================================================
# CRIAR USUARIO DE TESTE
# ==================================================


TEST_NAME="Teste"
TEST_EMAIL="teste@teste.com"
TEST_PASSWORD="Teste@123"

echo
echo "=================================================="
echo "CRIANDO USUARIO DE TESTE"
echo "=================================================="

AUTH_READY=false

echo
echo "Aguardando Customer Auth..."

for i in {1..30}; do

    if curl -s --max-time 3 "http://localhost:8000/api/users/" >/dev/null 2>&1; then

        AUTH_READY=true
        break

    fi

    echo "Aguardando Customer Auth... ($i/30)"
    sleep 2

done

if [ "$AUTH_READY" = "true" ]; then

    echo "✅ Customer Auth disponivel."

    echo
    echo "Tentando criar usuario..."

    # 30s e nao 10s: no primeiro cadastro o Node ainda esta esquentando, o
    # mongoose abre a conexao e o bcrypt gera o hash. Numa EC2 modesta os 10s
    # originais estouravam.
    REGISTER_RESPONSE=$(curl -s \
        --max-time 30 \
        -X POST \
        "http://localhost:8000/api/users/" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"$TEST_NAME\",
            \"email\": \"$TEST_EMAIL\",
            \"password\": \"$TEST_PASSWORD\"
        }")
    CURL_RC=$?

    if [ "$CURL_RC" -ne 0 ]; then
        echo
        echo "⚠️ O curl falhou (codigo $CURL_RC)."
        case "$CURL_RC" in
            28) echo "   Timeout: o customer-auth demorou demais para responder." ;;
            7)  echo "   Conexao recusada: o customer-auth nao esta ouvindo na 8000." ;;
        esac
        echo "   Veja: docker compose -f $COMPOSE_FILE logs --tail 30 customer-auth"
        echo "   O usuario pode ser criado depois pela propria tela de cadastro."
    fi

    echo
    echo "Resposta:"
    echo "$REGISTER_RESPONSE" | sed -E 's/"token":"[^"]+"/"token":"***OCULTO***"/g'

    if echo "$REGISTER_RESPONSE" | grep -qiE 'already exists|user already|duplicate'; then

        echo
        echo "ℹ️ Usuario ja existe."

    elif echo "$REGISTER_RESPONSE" | grep -qiE '"token"|"email"|success|created'; then

        echo
        echo "✅ Usuario criado."

    else

        echo
        echo "⚠️ Nao foi possivel confirmar o cadastro."

    fi

else
    COMPOSE_FILE="docker-compose.yml"
    echo
    echo "❌ Customer Auth nao respondeu."
    echo
    docker compose -f "$COMPOSE_FILE" logs --tail 30 customer-auth || true

fi
