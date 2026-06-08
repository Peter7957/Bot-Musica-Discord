#!/bin/bash
# Script para actualizar automáticamente la contraseña de VPNBook
# Se ejecuta cada 6 horas via cron

set -e

COMPOSE_DIR="/opt/dj-bot"
ENV_FILE="$COMPOSE_DIR/.env"

cd "$COMPOSE_DIR"

echo "$(date): Verificando contraseña de VPNBook..."

# Obtener contraseña actual de la web
NEW_PASSWORD=$(curl -s https://www.vpnbook.com/freevpn/openvpn | grep -oP 'Password:\s*</strong>\s*<strong>\K[^<]+' | head -1)

if [ -z "$NEW_PASSWORD" ]; then
    echo "$(date): No se pudo obtener contraseña de VPNBook"
    exit 1
fi

echo "$(date): Contraseña actual de VPNBook: $NEW_PASSWORD"

# Obtener contraseña actual del .env
CURRENT_PASSWORD=$(grep "VPNBOOK_PASSWORD=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 || echo "")

if [ "$NEW_PASSWORD" != "$CURRENT_PASSWORD" ]; then
    echo "$(date): Contraseña cambiada. Actualizando..."
    
    # Actualizar .env
    if grep -q "VPNBOOK_PASSWORD=" "$ENV_FILE"; then
        sed -i "s/VPNBOOK_PASSWORD=.*/VPNBOOK_PASSWORD=$NEW_PASSWORD/" "$ENV_FILE"
    else
        echo "VPNBOOK_PASSWORD=$NEW_PASSWORD" >> "$ENV_FILE"
    fi
    
    # Reiniciar contenedor VPN
    docker compose stop gluetun
    docker compose rm -f gluetun
    docker compose up -d gluetun
    
    echo "$(date): VPN actualizada con nueva contraseña"
else
    echo "$(date): La contraseña sigue siendo la misma"
fi
