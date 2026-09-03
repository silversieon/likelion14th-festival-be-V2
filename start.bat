@echo off
echo Building...
call gradlew.bat build -x test

echo Starting docker...
cd docker/local
docker compose ^
  --compatibility ^
  -f docker-compose-app-01.yml ^
  -f docker-compose-app-02.yml ^
  -f docker-compose-app-03.yml ^
   -f docker-compose-local.yml ^
  up -d --build

echo Done!
cd ..