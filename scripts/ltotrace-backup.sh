#!/bin/bash
# =====================================================================
# LTOTRACE - Respaldo diario de la base de datos atopa
# Se ejecuta como usuario postgres: usa la autenticación local del
# servidor, por lo que el script no contiene ninguna contraseña.
# =====================================================================
set -euo pipefail

DESTINO=/var/backups/ltotrace
RETENCION_DIAS=30
BASE=atopa
ARCHIVO="$DESTINO/${BASE}_$(date +%F_%H%M).dump"

# 1) Copia completa en formato personalizado (comprimido, restaurable con pg_restore)
pg_dump --format=custom --dbname="$BASE" --file="$ARCHIVO"

# 2) Comprueba que el archivo generado se puede leer
pg_restore --list "$ARCHIVO" > /dev/null

# 3) Elimina las copias con más de RETENCION_DIAS días
find "$DESTINO" -name "${BASE}_*.dump" -mtime +"$RETENCION_DIAS" -delete

echo "Respaldo generado: $ARCHIVO ($(du -h "$ARCHIVO" | cut -f1))"
