@echo off
echo ============================================
echo   Document Signature App - Setup and Run
echo ============================================
echo.

echo [1/4] Checking Java...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java not found. Install JDK 17+ from https://adoptium.net
    pause & exit /b 1
)
echo Java found.

echo [2/4] Checking Maven...
mvn -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven not found. Install from https://maven.apache.org
    pause & exit /b 1
)
echo Maven found.

echo [3/4] Creating MySQL database...
mysql -u root -p12345 -e "CREATE DATABASE IF NOT EXISTS signature_db;" 2>nul
echo Database ready (or already exists).

echo [4/4] Building...
if not exist uploads mkdir uploads
if not exist uploads\signed mkdir uploads\signed

call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed.
    pause & exit /b 1
)

echo.
echo Build successful! Starting on http://localhost:8080 ...
echo.
java -jar target\signature-app-1.0.0.jar
pause
