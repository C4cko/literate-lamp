#!/usr/bin/env bash
# Roda os três níveis de teste e guarda a saída e os relatórios do JUnit em resultados/.
# Uso: ./executar-testes.sh            (os três níveis)
#      ./executar-testes.sh unitario   (só um nível: unitario | integracao | sistema)
set -u

RAIZ="$(cd "$(dirname "$0")" && pwd)"
SAIDA="$RAIZ/resultados"
mkdir -p "$SAIDA"
cd "$RAIZ/testes" || exit 1

rodar() {
    local nivel="$1"; shift
    echo "==> $nivel"
    rm -rf target/surefire-reports
    mvn -B test "$@" 2>&1 | tee "$SAIDA/$nivel-saida.txt"
    local codigo=${PIPESTATUS[0]}
    mkdir -p "$SAIDA/$nivel"
    cp target/surefire-reports/*.xml target/surefire-reports/*.txt "$SAIDA/$nivel/" 2>/dev/null
    echo "$nivel terminou com código $codigo" | tee -a "$SAIDA/resumo.txt"
}

date "+Execução em %Y-%m-%d %H:%M:%S" | tee "$SAIDA/resumo.txt"
java -version 2>&1 | head -1 | tee -a "$SAIDA/resumo.txt"

case "${1:-todos}" in
    unitario)   rodar unitario ;;
    integracao) rodar integracao -Pintegracao ;;
    sistema)    rodar sistema -Psistema ;;
    todos)
        rodar unitario
        rodar integracao -Pintegracao
        rodar sistema -Psistema
        ;;
    *) echo "nível desconhecido: $1"; exit 2 ;;
esac
