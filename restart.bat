@echo off
setlocal EnableDelayedExpansion

REM ============================================================
REM Canary + Rolling Replacement
REM
REM Usage:
REM   restart.bat 01
REM   restart.bat 02
REM   restart.bat 03
REM ============================================================

echo "Building..."
call gradlew.bat build -x test

set TARGET=%1

if "%TARGET%"=="" (
    echo.
    echo Usage: restart.bat [01^|02^|03]
    echo.
    exit /b 1
)

if not "%TARGET%"=="01" if not "%TARGET%"=="02" if not "%TARGET%"=="03" (
    echo.
    echo Invalid target: %TARGET%
    echo Usage: restart.bat [01^|02^|03]
    echo.
    exit /b 1
)

set APP=festival-app-%TARGET%

echo.
echo ============================================================
echo Canary Deployment
echo Target: %APP%
echo ============================================================
echo.


REM ============================================================
REM 1. Remove target instance from Nginx traffic
REM ============================================================

echo [1/6] Removing %APP% from traffic...

(
    echo upstream festival-app {
    if "%TARGET%"=="01" echo     server festival-app-01:8080 down;
    if "%TARGET%"=="02" echo     server festival-app-02:8080 down;
    if "%TARGET%"=="03" echo     server festival-app-03:8080 down;

    if not "%TARGET%"=="01" echo     server festival-app-01:8080;
    if not "%TARGET%"=="02" echo     server festival-app-02:8080;
    if not "%TARGET%"=="03" echo     server festival-app-03:8080;
    echo }
) > nginx/conf.d/upstream-app.conf

docker exec nginx nginx -t

if errorlevel 1 (
    echo.
    echo Nginx configuration test failed.
    exit /b 1
)

docker exec nginx nginx -s reload

if errorlevel 1 (
    echo.
    echo Nginx reload failed.
    exit /b 1
)

echo Target instance removed from traffic.


REM ============================================================
REM 2. Rebuild and restart only target instance
REM ============================================================

echo.
echo [2/6] Rebuilding %APP%...

cd docker/local

docker compose ^
  --compatibility ^
  -f docker-compose-local.yml ^
  -f docker-compose-app-01.yml ^
  -f docker-compose-app-02.yml ^
  -f docker-compose-app-03.yml ^
  up -d --build --no-deps %APP%

if errorlevel 1 (
    echo.
    echo Failed to rebuild %APP%.
    exit /b 1
)

echo %APP% rebuilt successfully.


REM ============================================================
REM 3. Health Check
REM ============================================================

echo.
echo [3/6] Health checking %APP%...

set PORT=

if "%TARGET%"=="01" set PORT=8080
if "%TARGET%"=="02" set PORT=8081
if "%TARGET%"=="03" set PORT=8082

set HEALTH_OK=0

for /L %%i in (1,1,60) do (

    set STATUS=

    for /f "delims=" %%s in ('curl -s -o nul -w "%%{http_code}" http://localhost:%PORT%/actuator/health') do (
        set STATUS=%%s
    )

    if "!STATUS!"=="200" (
        set HEALTH_OK=1
        echo Health check passed. [%%i/60]
        goto HEALTH_CHECK_DONE
    )

    echo Waiting for %APP%... [%%i/60] status=!STATUS!

    timeout /t 5 /nobreak > nul
)

:HEALTH_CHECK_DONE

if "%HEALTH_OK%"=="0" (
    echo.
    echo ========================================================
    echo Health check FAILED.
    echo %APP% will remain out of traffic.
    echo ========================================================
    exit /b 1
)


REM ============================================================
REM 4. Canary traffic
REM
REM Target: weight 2
REM Others: weight 9
REM
REM 2 / (2 + 9 + 9) = 10%%
REM ============================================================

echo.
echo [4/6] Starting Canary traffic...

(
    echo upstream festival-app {
    if "%TARGET%"=="01" echo     server festival-app-01:8080 weight=2;
    if "%TARGET%"=="02" echo     server festival-app-02:8080 weight=2;
    if "%TARGET%"=="03" echo     server festival-app-03:8080 weight=2;

    if not "%TARGET%"=="01" echo     server festival-app-01:8080 weight=9;
    if not "%TARGET%"=="02" echo     server festival-app-02:8080 weight=9;
    if not "%TARGET%"=="03" echo     server festival-app-03:8080 weight=9;

    echo }
) > nginx/conf.d/upstream-app.conf

docker exec nginx nginx -t

if errorlevel 1 (
    echo.
    echo Nginx configuration test failed.
    exit /b 1
)

docker exec nginx nginx -s reload

if errorlevel 1 (
    echo.
    echo Nginx reload failed.
    exit /b 1
)

echo.
echo ============================================================
echo Canary traffic started.
echo Target: %APP%
echo Approximate traffic: 10%%
echo ============================================================
echo.


REM ============================================================
REM 5. Observation
REM ============================================================

echo [5/6] Canary observation
echo.
echo Check:
echo   - Application logs
echo   - HTTP 5xx
echo   - Response latency
echo   - Prometheus
echo   - Grafana
echo.
echo Press any key after verification...
pause > nul


REM ============================================================
REM 6. Restore equal traffic
REM ============================================================

echo.
echo [6/6] Restoring normal traffic...

(
    echo upstream festival-app {
    echo     server festival-app-01:8080;
    echo     server festival-app-02:8080;
    echo     server festival-app-03:8080;
    echo }
) > nginx/conf.d/upstream-app.conf

docker exec nginx nginx -t

if errorlevel 1 (
    echo.
    echo Nginx configuration test failed.
    exit /b 1
)

docker exec nginx nginx -s reload

if errorlevel 1 (
    echo.
    echo Nginx reload failed.
    exit /b 1
)

echo.
echo ============================================================
echo Canary deployment completed successfully.
echo Target: %APP%
echo Traffic: 33%% / 33%% / 33%%
echo ============================================================
echo.

endlocal