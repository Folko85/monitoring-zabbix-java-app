#!/bin/bash

set -e

echo "=== Настройка мониторинга Spring Boot + Zabbix ==="

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Проверка зависимостей
echo -e "${YELLOW}Проверка зависимостей...${NC}"

if ! command -v docker &> /dev/null; then
    echo "Ошибка: Docker не установлен"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "Ошибка: Docker Compose не установлен"
    exit 1
fi

echo -e "${GREEN}Все зависимости установлены${NC}"

# Запуск Docker Compose с автоматической сборкой (multi-stage build)
echo -e "${YELLOW}Сборка и запуск Docker Compose (multi-stage build)...${NC}"
docker-compose up -d --build

# Ожидание запуска сервисов
echo -e "${YELLOW}Ожидание запуска сервисов...${NC}"
sleep 30

# Проверка статуса
echo -e "${YELLOW}Проверка статуса сервисов...${NC}"
docker-compose ps

echo -e "${GREEN}=== Готово! ===${NC}"
echo ""
echo "Zabbix Web UI: http://localhost:8080"
echo "  Логин: Admin"
echo "  Пароль: zabbix"
echo ""
echo "Java приложение: http://localhost:8081"
echo "Actuator Health: http://localhost:8081/actuator/health"
echo ""
echo "Для остановки: docker-compose down"
