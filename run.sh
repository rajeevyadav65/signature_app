#!/bin/bash
# ============================================================
# Document Signature App - Auto Setup & Run Script
# Usage: chmod +x run.sh && ./run.sh
# Optional: SERVER_PORT=8081 ./run.sh
# ============================================================

set -e

APP_PORT="${SERVER_PORT:-8080}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "============================================"
echo "  Document Signature App - Setup and Run"
echo "============================================"
echo -e "${NC}"

echo -e "${YELLOW}[1/4] Checking Java version...${NC}"
if ! command -v java &>/dev/null; then
  echo -e "${RED}Java not found. Install JDK 17+ from https://adoptium.net${NC}"
  exit 1
fi
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ]; then
  echo -e "${RED}Java 17+ required. Found: $JAVA_VER${NC}"
  exit 1
fi
echo -e "${GREEN}Java $JAVA_VER found${NC}"

echo -e "${YELLOW}[2/4] Checking Maven...${NC}"
if ! command -v mvn &>/dev/null; then
  echo -e "${RED}Maven not found. Install from https://maven.apache.org${NC}"
  exit 1
fi
echo -e "${GREEN}Maven found: $(mvn -version 2>&1 | head -1)${NC}"

echo -e "${YELLOW}[3/4] Setting up MySQL database...${NC}"
if command -v mysql &>/dev/null; then
  mysql -u root -p12345 -e "CREATE DATABASE IF NOT EXISTS signature_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null && \
    echo -e "${GREEN}Database 'signature_db' ready${NC}" || \
    echo -e "${YELLOW}Could not auto-create DB. Create manually: CREATE DATABASE signature_db;${NC}"
else
  echo -e "${YELLOW}mysql CLI not found. Ensure MySQL is running and 'signature_db' database exists.${NC}"
fi

echo -e "${YELLOW}[4/4] Building and running application...${NC}"
mkdir -p uploads/signed

mvn clean package -DskipTests -q && \
  echo -e "${GREEN}Build successful${NC}" || \
  { echo -e "${RED}Build failed. Check pom.xml and Java version.${NC}"; exit 1; }

echo ""
echo -e "${GREEN}Starting Document Signature App on http://localhost:${APP_PORT}${NC}"
echo ""
java -jar target/signature-app-1.0.0.jar --server.port="${APP_PORT}"
