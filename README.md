# Мониторинг Spring Boot приложения через Zabbix

Проект для настройки мониторинга Java приложения с Spring Boot Actuator через Zabbix.

## Структура проекта

```
.
├── docker-compose.yml          # Docker Compose конфигурация для стенда
├── java-app/                   # Spring Boot приложениеµ
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── zabbix/
│   ├── agent/                  # Конфигурация Zabbix Agent
│   │   ├── zabbix_agentd.conf
│   │   └── user_parameters.conf
│   ├── items/                  # Экспорт Items из Zabbix
│   └── dashboards/             # Экспорт Dashboards
├── ansible/                     # Ansible playbooks для автоматизации
│   ├── playbook.yml
│   ├── inventory.yml
│   └── templates/
└── README.md
```

## Требования

- Docker и Docker Compose
- Ansible (для автоматизации настройки Zabbix Agent)

> **Примечание:** Maven используется только внутри Docker образа для multi-stage сборки, отдельная установка не требуется.

## Быстрый старт

### 1. Запуск стенда

Сборка Java приложения происходит автоматически при запуске Docker Compose (multi-stage build):

```bash
docker-compose up -d --build
```

Или для пересборки без кэша:

```bash
docker-compose build --no-cache
docker-compose up -d
```

Это запустит:
- PostgreSQL для Zabbix
- Zabbix Server (версия 6.4.20)
- Zabbix Web UI (порт 8080)
- Java приложение с Actuator (порт 8081)
- Zabbix Agent

### 3. Доступ к Zabbix Web UI

- URL: http://localhost:8080
- Логин: `Admin`
- Пароль: `zabbix`

### 4. Настройка Zabbix Agent через Ansible

```bash
cd ansible
ansible-playbook -i inventory.yml playbook.yml
```

Или для удаленного хоста:

```bash
ansible-playbook -i inventory.yml playbook.yml -e "ansible_host=your-host-ip"
```

## Настройка мониторинга

### Импорт Items в Zabbix

1. Войдите в Zabbix Web UI
2. Перейдите в Configuration → Templates
3. Импортируйте файл `zabbix/items/spring-boot-actuator-items.xml`
4. Примените template к хосту с Java приложением

### Создание Dashboard

1. Перейдите в Monitoring → Dashboards
2. Создайте новый dashboard
3. Импортируйте конфигурацию из `zabbix/dashboards/spring-boot-dashboard.json`
4. Настройте items согласно вашим ID

## Доступные метрики

### JVM метрики
- `jvm.memory.heap.used` - Использование heap памяти
- `jvm.memory.heap.max` - Максимальная heap память
- `jvm.memory.nonheap.used` - Использование non-heap памяти
- `jvm.threads.live` - Количество живых потоков
- `jvm.threads.daemon` - Количество daemon потоков

### Системные метрики
- `system.cpu.usage` - Использование CPU системой
- `process.cpu.usage` - Использование CPU процессом
- `process.uptime` - Время работы процесса

### Application метрики
- `actuator.health` - Статус здоровья приложения
- `http.server.requests.total` - Общее количество HTTP запросов

## Проверка работы

### Проверка Actuator endpoints

```bash
# Health check
curl http://localhost:8081/actuator/health

# Все метрики
curl http://localhost:8081/actuator/metrics

# Конкретная метрика
curl http://localhost:8081/actuator/metrics/jvm.memory.used?tag=area:heap
```

### Проверка Zabbix Agent

```bash
# На хосте с Zabbix Agent
zabbix_agentd -t actuator.health
zabbix_agentd -t jvm.memory.heap.used
```

## Конфигурация

### Изменение портов

Отредактируйте `docker-compose.yml` для изменения портов.

### Настройка Ansible

Отредактируйте `ansible/inventory.yml` для настройки хостов и параметров.

### Добавление новых метрик

1. Добавьте user parameter в `zabbix/agent/user_parameters.conf`
2. Обновите Ansible template `ansible/templates/user_parameters.conf.j2`
3. Добавьте item в `zabbix/items/spring-boot-actuator-items.xml`
4. Примените изменения через Ansible

## Troubleshooting

### Zabbix Agent не получает метрики

1. Проверьте доступность Java приложения:
   ```bash
   curl http://java-app:8080/actuator/health
   ```

2. Проверьте логи Zabbix Agent:
   ```bash
   docker logs zabbix-agent
   ```

3. Проверьте конфигурацию:
   ```bash
   docker exec zabbix-agent zabbix_agentd -t actuator.health
   ```

### Метрики не отображаются в Zabbix

1. Убедитесь, что host добавлен в Zabbix
2. Проверьте, что template применен к хосту
3. Проверьте статус items в Zabbix UI
4. Проверьте логи Zabbix Server

## Остановка стенда

```bash
docker-compose down
```

Для удаления всех данных (включая базу данных):

```bash
docker-compose down -v
```
