#!/usr/bin/env bash
# Hits the running app with a mixed traffic mix (existing keys → cache hits + misses,
# unknown keys → 404 path, and writes / claims) so the Grafana dashboards have data
# to draw. Loops forever; Ctrl+C to stop.
#
#   make load                  # default host / ispb
#   HOST=http://10.0.0.5:8080 ./scripts/load.sh
#
# Prerequisites: app running on the simulator profile (`make run-sim`) and
# Grafana stack up (`make up`).

set -u

HOST="${HOST:-http://localhost:8080}"
ISPB="${ISPB:-12345678}"
DELAY_MS="${DELAY_MS:-50}"

# Existing keys (seeded in InMemoryDictStore) → expected to flip cache hit/miss
EXISTING=(
    "EMAIL/loja@merchant.com"
    "CPF/12345678901"
    "PHONE/+5511987654321"
    "CNPJ/12345678000199"
    "EVP/550e8400-e29b-41d4-a716-446655440000"
)

# Unknown keys → 404 path (NOT_FOUND outcome, errors counter)
UNKNOWN=(
    "EMAIL/unknown@nowhere.com"
    "CPF/00000000000"
    "PHONE/+5511000000000"
)

curl_silent() {
    curl -s -o /dev/null -w "%{http_code}\n" -H "X-Payer-Ispb: $ISPB" "$1" || true
}

trap 'echo; echo "stopping load"; exit 0' INT TERM

echo "loading $HOST (ispb=$ISPB, delay=${DELAY_MS}ms). Ctrl+C to stop."
i=0
while true; do
    for k in "${EXISTING[@]}"; do
        curl_silent "$HOST/v1/dict/entries/$k" >/dev/null
        sleep "0.0$((DELAY_MS / 10))"
    done
    # one in five iterations sprinkle in some unknowns to drive 404 / NOT_FOUND outcomes
    if (( i % 5 == 0 )); then
        for k in "${UNKNOWN[@]}"; do
            curl_silent "$HOST/v1/dict/entries/$k" >/dev/null
            sleep "0.0$((DELAY_MS / 10))"
        done
    fi
    i=$((i + 1))
done
