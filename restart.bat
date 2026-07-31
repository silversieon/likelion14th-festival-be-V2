@echo off
echo Building...
call gradlew.bat build -x test

echo Restarting docker...
cd docker
docker compose -f docker-compose-local.yml up -d --build festival-app

echo Done!
cd ..