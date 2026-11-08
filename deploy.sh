#!/bin/bash
# =============================================================================
# deploy.sh — one-command redeploy of TaskFlow to AWS
#
# Usage (from the project root on your Mac):
#   ./deploy.sh            # deploy backend + frontend
#   ./deploy.sh backend    # only auth-service / task-service (EC2)
#   ./deploy.sh frontend   # only the React app (S3 + CloudFront)
#
# What it does:
#   1. Checks your code is committed and pushes it to GitHub
#   2. Allows your current Wi-Fi IP to SSH into the server
#   3. Backend:  server pulls from GitHub, rebuilds containers, waits until healthy
#   4. Frontend: builds React, uploads to S3, refreshes the CloudFront cache
#   5. Checks the live URL responds
# =============================================================================
set -euo pipefail

# ---- Settings for this deployment ------------------------------------------
REGION=ap-southeast-2
SERVER_IP=16.178.2.129
SSH_KEY="$HOME/.ssh/taskapp-key.pem"
EC2_SG=sg-0e1f8bddbb424be5c
BUCKET=taskapp-frontend-324673386287
DIST_ID=E17LHPPDI9Q2I9
LIVE_URL=https://d2mdtqoz9h6pyu.cloudfront.net
# ----------------------------------------------------------------------------

TARGET="${1:-all}"
case "$TARGET" in
  all|backend|frontend) ;;
  *) echo "Usage: ./deploy.sh [all|backend|frontend]"; exit 1 ;;
esac

cd "$(dirname "$0")"
START=$(date +%s)
step() { printf "\n\033[1;34m▶ %s\033[0m\n" "$1"; }
ok()   { printf "  \033[0;32m✔ %s\033[0m\n" "$1"; }
fail() { printf "  \033[0;31m✘ %s\033[0m\n" "$1"; exit 1; }

SSH_OPTS=(-i "$SSH_KEY" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=10 -o ServerAliveInterval=30)

# ---- 1. Git: committed and pushed ------------------------------------------
step "Checking Git"
if [ -n "$(git status --porcelain)" ]; then
  git status --short
  fail "You have uncommitted changes. Commit them first:  git add . && git commit -m \"your message\""
fi
git push --quiet origin HEAD
ok "Code is on GitHub ($(git log -1 --format='%h %s'))"

# ---- 2. Backend on EC2 ------------------------------------------------------
deploy_backend() {
  step "Allowing this network's IP to reach the server"
  MY_IP=$(curl -s https://checkip.amazonaws.com | tr -d '[:space:]')
  if OUT=$(aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$EC2_SG" \
        --protocol tcp --port 22 --cidr "$MY_IP/32" 2>&1); then
    ok "Added $MY_IP (waiting a few seconds for it to apply)"
    sleep 5
  elif echo "$OUT" | grep -q "InvalidPermission.Duplicate"; then
    ok "$MY_IP is already allowed"
  else
    echo "$OUT"; fail "Could not update the security group"
  fi

  step "Connecting to the server"
  for i in 1 2 3 4 5; do
    if ssh "${SSH_OPTS[@]}" "ubuntu@$SERVER_IP" true 2>/dev/null; then ok "Connected"; break; fi
    [ "$i" = 5 ] && fail "Cannot SSH to $SERVER_IP. Is the server running?"
    echo "  …retrying ($i/5)"; sleep 5
  done

  step "Pulling code and rebuilding containers on the server (backend)"
  ssh "${SSH_OPTS[@]}" "ubuntu@$SERVER_IP" 'bash -s' <<'REMOTE'
set -e
cd ~/task-management-app
git pull --ff-only
cd deploy
docker compose -f docker-compose.prod.yml up -d --build
# Reload the gateway so it picks up nginx.conf changes
docker compose -f docker-compose.prod.yml restart gateway
echo "  Waiting for services to become healthy…"
for i in $(seq 1 40); do
  if curl -fs localhost/api/auth/health >/dev/null && curl -fs localhost/api/tasks/health >/dev/null; then
    echo "  Both services are healthy"
    docker image prune -f >/dev/null   # free disk space from old images
    exit 0
  fi
  sleep 3
done
echo "  Services did not become healthy in time. Recent logs:"
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail=40 auth-service task-service
exit 1
REMOTE
  ok "Backend deployed"
}

# ---- 3. Frontend on S3 + CloudFront ----------------------------------------
deploy_frontend() {
  step "Building the React app (frontend)"
  (
    cd frontend
    [ -d node_modules ] || npm install --no-audit --no-fund
    npm run build
  )
  ok "Build ready in frontend/dist"

  step "Uploading to S3"
  aws s3 sync frontend/dist/ "s3://$BUCKET" --delete --exclude index.html \
    --cache-control "public,max-age=31536000,immutable" --only-show-errors
  aws s3 cp frontend/dist/index.html "s3://$BUCKET/index.html" \
    --cache-control "no-cache" --only-show-errors
  ok "Files uploaded"

  step "Refreshing the CloudFront cache"
  INV_ID=$(aws cloudfront create-invalidation --distribution-id "$DIST_ID" --paths "/*" \
    --query 'Invalidation.Id' --output text)
  aws cloudfront wait invalidation-completed --distribution-id "$DIST_ID" --id "$INV_ID"
  ok "CloudFront now serves the new version"
}

if [ "$TARGET" = all ] || [ "$TARGET" = backend ]; then deploy_backend; fi
if [ "$TARGET" = all ] || [ "$TARGET" = frontend ]; then deploy_frontend; fi

# ---- 4. Smoke test through the live URL ------------------------------------
step "Checking the live URL"
curl -fs "$LIVE_URL/api/auth/health"  >/dev/null && ok "Auth API reachable via CloudFront"  || fail "Auth API not reachable"
curl -fs "$LIVE_URL/api/tasks/health" >/dev/null && ok "Task API reachable via CloudFront"  || fail "Task API not reachable"
curl -fs "$LIVE_URL/" | grep -q "<div id=\"root\">" && ok "Frontend reachable" || fail "Frontend not reachable"

printf "\n\033[1;32m🚀 Deployed in %ss → %s\033[0m\n\n" "$(( $(date +%s) - START ))" "$LIVE_URL"
