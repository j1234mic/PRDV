#!/usr/bin/env bash
# ============================================================
# Demo end-to-end de la plateforme PRDV (necessite : docker compose up --build)
# ============================================================
set -euo pipefail
API=${API:-http://localhost:8080}
B="curl -s -H 'Content-Type: application/json'"
pretty() { python3 -m json.tool 2>/dev/null || cat; }

echo "==> 1. Login medecin de demo (seed actif)"
DR_TOKEN=$(curl -s $API/api/auth/login -d '{"email":"dr.sophie@prdv.local","password":"Medecin!2026demo"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
echo "    token ok: ${DR_TOKEN:0:24}..."

echo "==> 2. Inscription d'un nouveau patient"
curl -s $API/api/auth/register -d '{"email":"jean@example.org","phone":"0611223344","password":"MotDePasse!75","role":"PATIENT"}' | pretty

echo "==> 3. OTP (mode log : le code est dans 'docker compose logs api')"
OTP=$(docker compose logs api 2>/dev/null | grep -oE 'code=[0-9]{6}' | tail -1 | cut -d= -f2 || true)
if [ -z "${OTP:-}" ]; then echo "    (OTP indisponible ici - verifier les logs)"; else
curl -s $API/api/auth/verify-email -d "{\"email\":\"jean@example.org\",\"code\":\"$OTP\"}" | pretty
fi

echo "==> 4. Login patient + profil"
PT_TOKEN=$(curl -s $API/api/auth/login -d '{"email":"jean@example.org","password":"MotDePasse!75"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
curl -s -X POST $API/api/patients/me -H "Authorization: Bearer $PT_TOKEN" \
  -d '{"firstName":"Jean","lastName":"Dupont","birthDate":"1988-02-03","gender":"M","phone":"0611223344","city":"Paris","socialSecurityNumber":"188027513424748","allergies":["Penicilline"]}' | pretty || true

echo "==> 5. Annuaire public des medecins verifies"
curl -s "$API/api/doctors?limit=5" | pretty

echo "==> 6. Disponibilites du Dr Sophie (id 2 cote seed)"
DR_ID=$(curl -s "$API/api/doctors?limit=1" | python3 -c 'import sys,json;print(json.load(sys.stdin)[0]["userId"])')
curl -s "$API/api/slots/doctors/$DR_ID?days=14" | pretty | head -20

echo "==> 7. Reservation du premier creneau libre"
FIRST=$(curl -s "$API/api/slots/doctors/$DR_ID?days=14" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d[0]["start"])')
APPT=$(curl -s -X POST $API/api/appointments -H "Authorization: Bearer $PT_TOKEN" \
  -d "{\"doctorId\":$DR_ID,\"start\":\"$FIRST\",\"type\":\"FOLLOW_UP\",\"reason\":\"Consultation de controle\"}")
echo "$APPT" | pretty
APPT_ID=$(echo "$APPT" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')

echo "==> 8. Annulation (politique 24h => sans frais ici)"
curl -s -X POST $API/api/appointments/$APPT_ID/cancel -H "Authorization: Bearer $PT_TOKEN" \
  -d '{"reason":"empechement"}' | pretty

echo "==> 9. Notifications emises (transport=log)"
docker compose logs api --tail 30 | grep -E 'NOTIF|AUDIT' | tail -8 || true
echo "    -> voir aussi: liste d'attente auto-reassignee (module 4.3), logs de l'API."
