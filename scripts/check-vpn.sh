#!/bin/bash
# Verifica si la VPN de VPNBook está funcionando correctamente
# Uso: ./check-vpn.sh

CONTAINER_NAME="dj-vpn"

echo "Verificando estado de VPNBook..."

# Verificar si el contenedor está corriendo
if ! docker ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
    echo "La VPN no está corriendo. Intentando iniciar..."
    cd /opt/dj-bot && docker compose up -d gluetun
    sleep 10
fi

# Verificar IP pública
IP=$(docker exec $CONTAINER_NAME wget -qO- https://ipinfo.io/ip 2>/dev/null || echo "")

if [ -n "$IP" ]; then
    echo "VPN activa. IP pública: $IP"
    
    # Verificar que no sea IP de Hetzner (Alemania)
    if echo "$IP" | grep -qE "^(78|88|116|159|162|172|176|178|188|195)\."; then
        echo "ADVERTENCIA: La IP parece ser de Hetzner (Alemania). La VPN podría no estar funcionando."
        exit 1
    fi
    
    exit 0
else
    echo "ERROR: No se pudo obtener IP. La VPN podría estar fallando."
    echo "Verificando logs..."
    docker logs $CONTAINER_NAME --tail 20
    exit 1
fi
