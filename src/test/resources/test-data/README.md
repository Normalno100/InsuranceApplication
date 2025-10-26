# Test Data Documentation

Документация тестовых данных для проекта Travel Insurance.

## Структура папок

```
test-data/
├── requests/      - JSON файлы запросов
└── responses/     - JSON файлы ответов
```

## Запросы (requests/)

### Валидные запросы

| Файл | Описание | Период | Ожидаемая цена |
|------|----------|--------|----------------|
| `valid-request.json` | Стандартный валидный запрос | 10 дней | 10 |
| `same-dates-request.json` | Запрос с одинаковыми датами | 0 дней | 0 |
| `long-trip-request.json` | Длительная поездка | 60 дней | 60 |
| `special-chars-request.json` | Имена со спец. символами | 10 дней | 10 |
| `cyrillic-request.json` | Имена на кириллице | 10 дней | 10 |

### Невалидные запросы

| Файл | Описание | Ошибка |
|------|----------|--------|
| `empty-first-name-request.json` | Пустое имя | personFirstName |
| `empty-last-name-request.json` | Пустая фамилия | personLastName |
| `null-date-from-request.json` | Отсутствует дата начала | agreementDateFrom |
| `null-date-to-request.json` | Отсутствует дата окончания | agreementDateTo |
| `invalid-date-order-request.json` | Неверный порядок дат | agreementDateTo |
| `all-invalid-request.json` | Все поля невалидны | Все 4 поля |

## Ответы (responses/)

### Успешные ответы

| Файл | Описание | Цена |
|------|----------|------|
| `successful-response.json` | Стандартный успешный ответ | 10 |
| `zero-price-response.json` | Цена для нулевого периода | 0 |
| `long-trip-response.json` | Цена для длительной поездки | 60 |

### Ответы с ошибками

| Файл | Описание | Количество ошибок |
|------|----------|-------------------|
| `error-first-name-response.json` | Ошибка имени | 1 |
| `error-last-name-response.json` | Ошибка фамилии | 1 |
| `error-date-from-response.json` | Ошибка даты начала | 1 |
| `error-date-to-response.json` | Ошибка даты окончания | 1 |
| `error-date-order-response.json` | Ошибка порядка дат | 1 |
| `all-errors-response.json` | Все возможные ошибки | 4 |

## Использование

### Загрузка запроса как объекта

```java
TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
    "valid-request.json", 
    TravelCalculatePremiumRequest.class
);
```

### Загрузка запроса как строки (для MockMvc)

```java
String requestJson = TestDataLoader.loadRequestAsString("valid-request.json");
```

### Загрузка ответа

```java
TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
    "successful-response.json", 
    TravelCalculatePremiumResponse.class
);
```

## Правила именования

- **Запросы**: `{сценарий}-request.json`
- **Ответы**: `{сценарий}-response.json` или `error-{поле}-response.json`
- Используйте kebab-case для имен файлов
- Имена должны быть описательными

## Добавление новых файлов

При добавлении нового JSON файла:

1. Создайте файл в соответствующей папке
2. Обновите эту документацию
3. Добавьте тест, использующий новый файл
4. Убедитесь, что JSON валиден

## Формат дат

Даты должны быть в формате ISO 8601: `yyyy-MM-dd`

Пример: `"2023-01-01"`

## Версионирование

При изменении структуры DTO необходимо обновить все соответствующие JSON файлы.

**Последнее обновление:** 2025-10-22