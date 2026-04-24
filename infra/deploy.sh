#!/usr/bin/env bash
# Script de deploy do Contab360 para servidor Linux
# Uso: ./infra/deploy.sh
# Pre-requisito: prod.env preenchido na raiz do projeto no servidor

set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$APP_DIR/target/contab360-0.0.1-SNAPSHOT.jar"
ENV_FILE="$APP_DIR/prod.env"
SERVICE="contab360"

echo "[$(date)] Verificando pre-requisitos..."

if [ ! -f "$ENV_FILE" ]; then
  echo "ERRO: prod.env nao encontrado em $APP_DIR"
  echo "Copie prod.env.example para prod.env e preencha as variaveis."
  exit 1
fi

# Carregar variaveis de ambiente
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

# Validar variaveis obrigatorias
for VAR in CONTAB360_MYSQL_USER CONTAB360_MYSQL_PASSWORD CONTAB360_UPLOAD_DIR CONTAB360_LOG_DIR; do
  if [ -z "${!VAR:-}" ]; then
    echo "ERRO: variavel $VAR nao definida em prod.env"
    exit 1
  fi
done

echo "[$(date)] Criando diretorios necessarios..."
mkdir -p "$CONTAB360_UPLOAD_DIR" "$CONTAB360_LOG_DIR"

echo "[$(date)] Compilando aplicacao..."
cd "$APP_DIR"
./mvnw clean package -DskipTests -q

if [ ! -f "$JAR" ]; then
  echo "ERRO: JAR nao gerado em $JAR"
  exit 1
fi

echo "[$(date)] Parando servico anterior (se existir)..."
systemctl is-active --quiet "$SERVICE" && systemctl stop "$SERVICE" || true

echo "[$(date)] Iniciando novo processo..."
# Roda como servico systemd (ver infra/contab360.service)
systemctl start "$SERVICE"

echo "[$(date)] Aguardando aplicacao subir (max 60s)..."
for i in $(seq 1 20); do
  if curl -sf http://localhost:8080/ > /dev/null 2>&1; then
    echo "[$(date)] Aplicacao no ar!"
    break
  fi
  sleep 3
  if [ "$i" -eq 20 ]; then
    echo "ERRO: aplicacao nao respondeu em 60s. Verifique: journalctl -u $SERVICE -n 50"
    exit 1
  fi
done

echo "[$(date)] Deploy concluido com sucesso."
