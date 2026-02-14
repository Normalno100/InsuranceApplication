package org.javaguru.travel.insurance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Базовый класс для всех E2E интеграционных тестов
 *
 * Версия с загрузкой данных ОДИН РАЗ перед всеми тестами
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // Позволяет использовать @BeforeAll с @Autowired
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    protected static final String API_ENDPOINT = "/insurance/travel/calculate";

    /**
     * Загружает тестовые данные ОДИН РАЗ перед всеми тестами
     */
    @BeforeAll
    void loadTestData() {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/test-data.sql"));
            populator.setContinueOnError(true);  // Continue even if some data already exists
            populator.execute(dataSource);
            System.out.println("✅ Test data loaded successfully");
        } catch (Exception e) {
            System.err.println("⚠️  Test data loading warning: " + e.getMessage());
            // Continue - data might already be loaded or schema might differ
        }
    }

    /**
     * Выполняет POST запрос на расчёт премии
     */
    protected ResultActions performCalculatePremium(Object request) throws Exception {
        return mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    /**
     * Выполняет POST запрос с параметром includeDetails
     */
    protected ResultActions performCalculatePremium(Object request, boolean includeDetails) throws Exception {
        return mockMvc.perform(post(API_ENDPOINT)
                .param("includeDetails", String.valueOf(includeDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}