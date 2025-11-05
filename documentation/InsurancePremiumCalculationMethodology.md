# 🚀 План реализации расчета страховых премий

## 📋 Оглавление

1. Обзор изменений
2. Этапы реализации
3. Миграция БД
4. Тестирование
5. Деплой
6. Мониторинг

---

## Обзор изменений

### Что добавляется

#### 1. Новые Enum'ы
- ✅ `MedicalRiskLimitLevel` - уровни медицинского покрытия
- ✅ `Country` - справочник стран с коэффициентами
- ✅ `RiskType` - типы страховых рисков

#### 2. Новые DTO
- ✅ `TravelCalculatePremiumRequestV2` - расширенный запрос
- ✅ `TravelCalculatePremiumResponseV2` - расширенный ответ с деталями

#### 3. Калькуляторы
- ✅ `AgeCalculator` - расчет возраста и коэффициентов
- ✅ `MedicalRiskPremiumCalculator` - главный калькулятор премии

#### 4. Сервисы
- ✅ `PromoCodeService` - работа с промо-кодами
- ✅ `DiscountService` - групповые и корпоративные скидки
- ✅ `CurrencyExchangeService` - конвертация валют
- ✅ `TravelCalculatePremiumServiceV2` - главный сервис

#### 5. REST API
- ✅ `TravelCalculatePremiumControllerV2` - новый контроллер v2

#### 6. База данных
- ✅ 15 новых таблиц
- ✅ Справочники и бизнес-таблицы
- ✅ Аудит и логирование

---

## Этапы реализации

### ЭТАП 1: Подготовка (1-2 дня)

#### 1.1 Настройка окружения
```bash
# 1. Обновить зависимости
# В build.gradle добавить:
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.postgresql:postgresql:42.6.0'
implementation 'org.liquibase:liquibase-core:4.24.0'

# 2. Настроить БД
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/travel_insurance
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
spring.liquibase.enabled=true
```

#### 1.2 Создать структуру пакетов
```
src/main/java/org/javaguru/travel/insurance/
├── core/
│   ├── domain/              # Enum'ы
│   ├── calculators/         # Калькуляторы
│   ├── services/            # Бизнес-сервисы
│   └── validation/          # Валидаторы
├── dto/
│   └── v2/                  # DTO версии 2
├── rest/
│   └── v2/                  # REST контроллеры v2
└── repository/              # JPA репозитории
```

---

### ЭТАП 2: База данных (2-3 дня)

#### 2.1 Создать миграции Liquibase

**Файл: `src/main/resources/db/changelog/db.changelog-master.yaml`**
```yaml
databaseChangeLog:
  - include:
      file: db/changelog/001_create_reference_tables.sql
  - include:
      file: db/changelog/002_create_business_tables.sql
  - include:
      file: db/changelog/003_create_indexes.sql
  - include:
      file: db/changelog/004_insert_initial_data.sql
```

#### 2.2 Запустить миграции
```bash
# Локально
./gradlew liquibaseUpdate

# Prod (после тестирования)
./gradlew liquibaseUpdate -Penv=prod
```

#### 2.3 Проверить структуру
```sql
-- Проверка таблиц
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public';

-- Проверка данных
SELECT COUNT(*) FROM age_coefficients;
SELECT COUNT(*) FROM countries;
SELECT COUNT(*) FROM risk_types;
```

---

### ЭТАП 3: Enum'ы и Domain модели (1 день)

#### 3.1 Создать Enum'ы
- ✅ `MedicalRiskLimitLevel.java`
- ✅ `Country.java`
- ✅ `RiskType.java`

#### 3.2 Написать unit-тесты для Enum'ов
```java
@DisplayName("MedicalRiskLimitLevel Tests")
class MedicalRiskLimitLevelTest {
    
    @Test
    void shouldFindLevelByCode() {
        assertEquals(
            MedicalRiskLimitLevel.LEVEL_50000,
            MedicalRiskLimitLevel.fromCode("50000")
        );
    }
    
    @Test
    void shouldFindLevelByAmount() {
        assertEquals(
            MedicalRiskLimitLevel.LEVEL_50000,
            MedicalRiskLimitLevel.findByAmount(new BigDecimal("45000"))
        );
    }
}
```

**Цель:** Минимум 20 тестов на каждый Enum

---

### ЭТАП 4: Калькуляторы (3-4 дня)

#### 4.1 Реализовать AgeCalculator
```java
@Component
public class AgeCalculator {
    public int calculateAge(LocalDate birthDate, LocalDate referenceDate);
    public BigDecimal getAgeCoefficient(int age);
    public AgeCalculationResult calculateAgeAndCoefficient();
}
```

**Тесты (минимум 30):**
- Расчет возраста для разных дат
- Коэффициенты для всех возрастных групп
- Граничные значения
- Валидация

#### 4.2 Реализовать MedicalRiskPremiumCalculator
```java
@Component
public class MedicalRiskPremiumCalculator {
    public BigDecimal calculatePremium(TravelCalculatePremiumRequestV2);
    public PremiumCalculationResult calculatePremiumWithDetails();
}
```

**Тесты (минимум 50):**
- Расчет для разных комбинаций параметров
- Проверка формулы
- Округление
- Детали расчета

---

### ЭТАП 5: Сервисы скидок и промо-кодов (2-3 дня)

#### 5.1 PromoCodeService
```java
@Service
public class PromoCodeService {
    public PromoCodeResult applyPromoCode(...);
    private ValidationResult validatePromoCode(...);
    private BigDecimal calculateDiscount(...);
}
```

**Тесты (минимум 40):**
- Применение валидного промо-кода
- Проверка периода действия
- Лимиты использования
- Минимальная сумма
- Максимальная скидка

#### 5.2 DiscountService
```java
@Service
public class DiscountService {
    public List<DiscountResult> calculateApplicableDiscounts(...);
    public Optional<DiscountResult> calculateBestDiscount(...);
}
```

**Тесты (минимум 35):**
- Групповые скидки
- Корпоративные скидки
- Сезонные скидки
- Комбинирование скидок

---

### ЭТАП 6: Главный сервис и API (3-4 дня)

#### 6.1 TravelCalculatePremiumServiceV2
```java
@Service
public class TravelCalculatePremiumServiceV2 {
    public TravelCalculatePremiumResponseV2 calculatePremium(
        TravelCalculatePremiumRequestV2 request
    );
}
```

**Интеграция компонентов:**
1. Валидация → 2. Расчет → 3. Промо-коды → 4. Скидки → 5. Ответ

**Тесты (минимум 60):**
- Happy path сценарии
- Валидация ошибок
- Применение промо-кодов
- Применение скидок
- Комбинации

#### 6.2 REST API v2
```java
@RestController
@RequestMapping("/insurance/travel/v2")
public class TravelCalculatePremiumControllerV2 {
    @PostMapping("/calculate")
    public ResponseEntity<TravelCalculatePremiumResponseV2> calculatePremium(...);
}
```

**Тесты (минимум 25):**
- Все эндпоинты
- Валидация JSON
- HTTP статусы
- Обработка ошибок

---

### ЭТАП 7: Интеграционные тесты (2-3 дня)

#### 7.1 End-to-End тесты
```java
@SpringBootTest
@AutoConfigureMockMvc
class TravelInsuranceE2ETest {
    
    @Test
    void shouldCalculateComplexPremium() {
        // Given: сложный запрос
        // When: отправляем на /v2/calculate
        // Then: проверяем детальный ответ
    }
}
```

**Сценарии (минимум 20):**
- Простой отпуск в Европе
- Горнолыжный тур
- Экзотическое путешествие
- Пожилой турист
- Групповая поездка
- С промо-кодом
- Корпоративный клиент

#### 7.2 Performance тесты
```java
@Test
void shouldHandleHighLoad() {
    // 1000 запросов за 10 секунд
    // Средний response time < 200ms
}
```

---

### ЭТАП 8: Документация (1-2 дня)

#### 8.1 API Documentation (OpenAPI/Swagger)
```yaml
openapi: 3.0.0
info:
  title: Travel Insurance API v2
  version: 2.0.0
paths:
  /insurance/travel/v2/calculate:
    post:
      summary: Calculate insurance premium
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CalculateRequest'
```

#### 8.2 User Guide
- Примеры использования API
- Описание всех полей
- Примеры расчетов
- FAQ

#### 8.3 Developer Guide
- Архитектура решения
- Как добавить новую страну
- Как изменить коэффициенты
- Как добавить новый тип риска

---

## Миграция БД

### Стратегия миграции

#### Вариант 1: Blue-Green Deployment
```
1. Развернуть новую версию (v2) параллельно
2. Новые клиенты → v2
3. Старые клиенты → v1 (deprecated)
4. Через 3 месяца: отключить v1
```

#### Вариант 2: Постепенная миграция
```
1. v1 и v2 работают одновременно
2. Feature flag для переключения
3. A/B тестирование
4. Постепенный переход
```

### План миграции данных

#### Шаг 1: Создать новые таблицы
```sql
-- Liquibase создаст все таблицы
-- Данные не затрагиваются
```

#### Шаг 2: Заполнить справочники
```sql
-- Автоматически из миграций
INSERT INTO countries ...
INSERT INTO age_coefficients ...
INSERT INTO risk_types ...
```

#### Шаг 3: Миграция существующих договоров (опционально)
```sql
-- Скрипт для миграции старых договоров в новую структуру
INSERT INTO persons (first_name, last_name, birth_date)
SELECT DISTINCT person_first_name, person_last_name, '1990-01-01'
FROM old_agreements;
```

---

## Тестирование

### Покрытие тестами

| Компонент             | Unit Tests  | Integration Tests  | Total   |
|-----------------------|-------------|--------------------|---------|
| Enum'ы                | 60          | -                  | 60      |
| AgeCalculator         | 30          | -                  | 30      |
| MedicalRiskCalculator | 50          | -                  | 50      |
| PromoCodeService      | 40          | -                  | 40      |
| DiscountService       | 35          | -                  | 35      |
| Main Service          | 60          | 20                 | 80      |
| REST API              | 25          | 20                 | 45      |
| **ИТОГО**             | **300**     | **40**             | **340** |

### План тестирования

#### Week 1: Unit Tests
- День 1-2: Enum'ы и Domain
- День 3-4: Калькуляторы
- День 5: Сервисы

#### Week 2: Integration Tests
- День 1-2: API тесты
- День 3: E2E сценарии
- День 4: Performance
- День 5: Документация

---

## Деплой

### Checklist перед деплоем

- [ ] Все тесты проходят (340/340)
- [ ] Code coverage > 85%
- [ ] Нет критических багов в Sonar
- [ ] API документация обновлена
- [ ] Миграции БД проверены на staging
- [ ] Rollback plan подготовлен
- [ ] Мониторинг настроен
- [ ] Алерты настроены

### Процесс деплоя

#### 1. Staging
```bash
# 1. Deploy на staging
./deploy.sh staging

# 2. Smoke tests
./run_smoke_tests.sh staging

# 3. Full regression
./run_regression_tests.sh staging
```

#### 2. Production
```bash
# 1. Blue-Green deployment
./deploy.sh production --blue-green

# 2. Health check
curl https://api.travel-insurance.com/v2/health

# 3. Monitor
# Проверить метрики 30 минут
# Если OK → switch traffic
# Если не OK → rollback
```

---

## Мониторинг

### Метрики для отслеживания

#### 1. Performance
- Response time (p50, p95, p99)
- Throughput (requests/sec)
- Error rate

#### 2. Business Metrics
- Количество расчетов
- Средняя премия
- Применение промо-кодов
- Использование скидок

#### 3. Алерты
```yaml
alerts:
  - name: high_error_rate
    condition: error_rate > 5%
    severity: critical
  
  - name: slow_response
    condition: p95_response_time > 500ms
    severity: warning
  
  - name: database_connection
    condition: connection_errors > 0
    severity: critical
```

### Dashboard

```
┌─────────────────────────────────────────────┐
│  Travel Insurance V2 - Real-time Dashboard  │
├─────────────────────────────────────────────┤
│                                             │
│  Requests/sec:  [=====>    ] 127            │
│  Avg Response:  [===>      ] 143ms          │
│  Error Rate:    [          ] 0.2%           │
│  Success Rate:  [=========>] 99.8%          │
│                                             │
│  Top Countries: ES (34%), DE (21%), FR...   │
│  Promo Codes:   SUMMER2025 (67 uses)        │
│  Avg Premium:   €234.56                     │
│                                             │
└─────────────────────────────────────────────┘
```

---

## Оценка времени

| Этап                  | Время      | Примечание           |
|-----------------------|------------|----------------------|
| 1. Подготовка         | 2 дня      | Настройка окружения  |
| 2. БД                 | 3 дня      | Миграции + тесты     |
| 3. Enum'ы             | 1 день     | + unit tests         |
| 4. Калькуляторы       | 4 дня      | + полное покрытие    |
| 5. Сервисы            | 3 дня      | Промо-коды, скидки   |
| 6. Main Service + API | 4 дня      | Интеграция           |
| 7. Integration Tests  | 3 дня      | E2E + performance    |
| 8. Документация       | 2 дня      | API + guides         |
| 9. Деплой             | 2 дня      | Staging + production |
| **ИТОГО**             | **24 дня** | ~1 месяц работы      |

---

## Риски и митигация

| Риск                    | Вероятность   | Влияние     | Митигация                          |
|-------------------------|---------------|-------------|------------------------------------|
| Проблемы с миграцией БД | Средняя       | Высокое     | Тщательное тестирование на staging |
| Performance деградация  | Низкая        | Высокое     | Load testing перед prod            |
| Несовместимость с v1    | Низкая        | Среднее     | Параллельная работа v1 и v2        |
| Bugs в расчетах         | Средняя       | Критическое | 340 тестов + QA review             |

---

## Поддержка после релиза

### Week 1
- Ежедневный мониторинг метрик
- Hotfix готовность
- Сбор feedback

### Month 1
- Weekly review
- Bug fixes
- Performance optimization

### Quarter 1
- Deprecate v1 API
- Полная миграция клиентов
- Lessons learned

---

**Статус:** ✅ План готов к исполнению  
**Следующий шаг:** Подготовка окружения (ЭТАП 1)