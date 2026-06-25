@echo off
echo Starting docker...
cd docker
docker compose --compatibility -f docker-compose-local.yml up -d --build

echo Done!
cd ..