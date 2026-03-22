package org.javaguru.travel.insurance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Базовый класс для всех E2E интеграционных тестов.
 *
 * ЭТАП 3 (рефакторинг): Изоляция интеграционных тестов.
 *
 * БЫЛО:
 *   @Sql(scripts = "/db/test-data.sql",
 *        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
 *
 *   Данные загружались один раз на весь класс. Тесты, изменяющие состояние
 *   (например, инкремент current_usage_count у промо-кода), влияли на все
 *   последующие тесты. Порядок выполнения тестов имел значение.
 *
 * СТАЛО:
 *   @Sql(scripts = {"/db/clean.sql", "/db/test-data.sql"},
 *        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
 *
 *   Перед каждым тестом:
 *   1. clean.sql  — полностью очищает тестовые таблицы
 *   2. test-data.sql — загружает свежие данные
 *
 *   Преимущества:
 *   - Каждый тест изолирован и независим
 *   - Порядок выполнения тестов не важен
 *   - Промо-коды с лимитом использований не "засоряются" между тестами
 *   - CI зелёный при любом порядке запуска
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        scripts = {"/db/clean.sql", "/db/test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String API_ENDPOINT = "/insurance/travel/calculate";

    /**
     * Выполняет POST запрос на расчёт премии.
     */
    protected ResultActions performCalculatePremium(Object request) throws Exception {
        return mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    /**
     * Выполняет POST запрос с параметром includeDetails.
     */
    protected ResultActions performCalculatePremium(Object request, boolean includeDetails) throws Exception {
        return mockMvc.perform(post(API_ENDPOINT)
                .param("includeDetails", String.valueOf(includeDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}