@echo off
echo Building...
call gradlew.bat build -x test

echo Starting docker...
cd docker
docker compose --compatibility -f docker-compose-local.yml up -d --build

echo Done!
cd ..