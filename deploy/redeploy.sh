#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."

docker build -t reality-app-be:local .
sudo docker save reality-app-be:local | sudo k3s ctr images import -
kubectl rollout restart deployment/reality-app-be -n reality-app
kubectl rollout status deployment/reality-app-be -n reality-app
