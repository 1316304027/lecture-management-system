#!/bin/bash
# EC2 上一键部署（SSH 连上后执行）
# 用法: cd ~/lecture-management-system/demo && bash scripts/deploy-ec2.sh

set -e
cd "$(dirname "$0")/.."
REPO_ROOT="$(cd .. && pwd)"

echo "=== 1. 拉取最新代码（保留 EC2 上的 docker-compose.yml）==="
cd "$REPO_ROOT"
git stash push -m "ec2-compose-backup" -- demo/docker-compose.yml 2>/dev/null || true
git pull origin main
git stash pop 2>/dev/null || true

echo "=== 2. Maven 打包 ==="
cd "$REPO_ROOT/demo"
chmod +x mvnw
./mvnw clean package -DskipTests -q

echo "=== 3. 复制 jar 到 docker 挂载目录 ==="
mkdir -p "$REPO_ROOT/target"
cp -f target/demo-0.0.1-SNAPSHOT.jar "$REPO_ROOT/target/"

echo "=== 4. 重启容器 ==="
docker compose restart app nginx 2>/dev/null || docker compose up -d

echo "=== 5. 检查 ==="
sleep 5
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost/login || true
docker compose ps
echo "完成。浏览器打开: http://35.77.43.229/login"
