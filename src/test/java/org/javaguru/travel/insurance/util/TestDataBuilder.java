package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.ValidationError;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.javaguru.travel.insurance.util.TestDataConstants.Requests;
import static org.javaguru.travel.insurance.util.TestDataConstants.Responses;

/**
 * Builder для создания тестовых данных
 * Комбинирует загрузку из JSON и программное создание объектов
 */
public class TestDataBuilder {

    /**
     * Загружает базовый валидный запрос и позволяет модифицировать его
     */
    public static RequestBuilder request() {
        return new RequestBuilder();
    }

    /**
     * Загружает базовый успешный ответ и позволяет модифицировать его
     */
    public static ResponseBuilder response() {
        return new ResponseBuilder();
    }

    // ========== REQUEST BUILDER ==========

    public static class RequestBuilder {
        private TravelCalculatePremiumRequest request;

        public RequestBuilder() {
            try {
                // Загружаем базовый валидный запрос
                this.request = TestDataLoader.loadRequest(
                        Requests.VALID,
                        TravelCalculatePremiumRequest.class
                );
            } catch (IOException e) {
                // Fallback: создаем вручную
                this.request = new TravelCalculatePremiumRequest();
                this.request.setPersonFirstName("John");
                this.request.setPersonLastName("Smith");
                this.request.setAgreementDateFrom(LocalDate.now());
                this.request.setAgreementDateTo(LocalDate.now().plusDays(10));
            }
        }

        /**
         * Загружает запрос из конкретного JSON файла
         */
        public RequestBuilder fromFile(String fileName) {
            try {
                this.request = TestDataLoader.loadRequest(
                        fileName,
                        TravelCalculatePremiumRequest.class
                );
            } catch (IOException e) {
                throw new RuntimeException("Failed to load request from file: " + fileName, e);
            }
            return this;
        }

        public RequestBuilder withFirstName(String firstName) {
            request.setPersonFirstName(firstName);
            return this;
        }

        public RequestBuilder withLastName(String lastName) {
            request.setPersonLastName(lastName);
            return this;
        }

        public RequestBuilder withDateFrom(LocalDate dateFrom) {
            request.setAgreementDateFrom(dateFrom);
            return this;
        }

        public RequestBuilder withDateTo(LocalDate dateTo) {
            request.setAgreementDateTo(dateTo);
            return this;
        }

        public RequestBuilder withEmptyFirstName() {
            request.setPersonFirstName("");
            return this;
        }

        public RequestBuilder withEmptyLastName() {
            request.setPersonLastName("");
            return this;
        }

        public RequestBuilder withNullDateFrom() {
            request.setAgreementDateFrom(null);
            return this;
        }

        public RequestBuilder withNullDateTo() {
            request.setAgreementDateTo(null);
            return this;
        }

        public RequestBuilder withInvalidDateOrder() {
            LocalDate today = LocalDate.now();
            request.setAgreementDateFrom(today.plusDays(10));
            request.setAgreementDateTo(today);
            return this;
        }

        public RequestBuilder withSameDates() {
            LocalDate today = LocalDate.now();
            request.setAgreementDateFrom(today);
            request.setAgreementDateTo(today);
            return this;
        }

        public RequestBuilder withPeriodDays(int days) {
            LocalDate today = LocalDate.now();
            request.setAgreementDateFrom(today);
            request.setAgreementDateTo(today.plusDays(days));
            return this;
        }

        public RequestBuilder withSpecialCharacters() {
            request.setPersonFirstName("Jean-Pierre");
            request.setPersonLastName("O'Connor");
            return this;
        }

        public RequestBuilder withCyrillicNames() {
            request.setPersonFirstName("Иван");
            request.setPersonLastName("Петров");
            return this;
        }

        public TravelCalculatePremiumRequest build() {
            return request;
        }
    }

    // ========== RESPONSE BUILDER ==========

    public static class ResponseBuilder {
        private TravelCalculatePremiumResponse response;

        public ResponseBuilder() {
            try {
                // Загружаем базовый успешный ответ
                this.response = TestDataLoader.loadResponse(
                        Responses.SUCCESSFUL,
                        TravelCalculatePremiumResponse.class
                );
            } catch (IOException e) {
                // Fallback: создаем вручную
                this.response = new TravelCalculatePremiumResponse();
                this.response.setPersonFirstName("John");
                this.response.setPersonLastName("Smith");
                this.response.setAgreementDateFrom(LocalDate.now());
                this.response.setAgreementDateTo(LocalDate.now().plusDays(10));
                this.response.setAgreementPrice(new BigDecimal("10"));
            }
        }

        /**
         * Загружает ответ из конкретного JSON файла
         */
        public ResponseBuilder fromFile(String fileName) {
            try {
                this.response = TestDataLoader.loadResponse(
                        fileName,
                        TravelCalculatePremiumResponse.class
                );
            } catch (IOException e) {
                throw new RuntimeException("Failed to load response from file: " + fileName, e);
            }
            return this;
        }

        public ResponseBuilder withFirstName(String firstName) {
            response.setPersonFirstName(firstName);
            return this;
        }

        public ResponseBuilder withLastName(String lastName) {
            response.setPersonLastName(lastName);
            return this;
        }

        public ResponseBuilder withDateFrom(LocalDate dateFrom) {
            response.setAgreementDateFrom(dateFrom);
            return this;
        }

        public ResponseBuilder withDateTo(LocalDate dateTo) {
            response.setAgreementDateTo(dateTo);
            return this;
        }

        public ResponseBuilder withPrice(BigDecimal price) {
            response.setAgreementPrice(price);
            return this;
        }

        public ResponseBuilder withPrice(long price) {
            response.setAgreementPrice(new BigDecimal(price));
            return this;
        }

        public ResponseBuilder withError(String field, String message) {
            if (response.getErrors() == null) {
                response.setErrors(new ArrayList<>());
            }
            response.getErrors().add(new ValidationError(field, message));
            return this;
        }

        public ResponseBuilder withErrors(List<ValidationError> errors) {
            response.setErrors(errors);
            return this;
        }

        public ResponseBuilder basedOnRequest(TravelCalculatePremiumRequest request) {
            response.setPersonFirstName(request.getPersonFirstName());
            response.setPersonLastName(request.getPersonLastName());
            response.setAgreementDateFrom(request.getAgreementDateFrom());
            response.setAgreementDateTo(request.getAgreementDateTo());
            return this;
        }

        public TravelCalculatePremiumResponse build() {
            return response;
        }
    }

    // ========== CONVENIENCE METHODS ==========

    /**
     * Создает валидный запрос из JSON
     */
    public static TravelCalculatePremiumRequest validRequest() {
        return request().build();
    }

    /**
     * Создает валидный запрос с указанным периодом
     */
    public static TravelCalculatePremiumRequest validRequestWithPeriod(int days) {
        return request().withPeriodDays(days).build();
    }

    /**
     * Создает запрос с пустым именем
     */
    public static TravelCalculatePremiumRequest requestWithEmptyFirstName() {
        return request().withEmptyFirstName().build();
    }

    /**
     * Создает запрос с пустой фамилией
     */
    public static TravelCalculatePremiumRequest requestWithEmptyLastName() {
        return request().withEmptyLastName().build();
    }

    /**
     * Создает запрос с неверным порядком дат
     */
    public static TravelCalculatePremiumRequest requestWithInvalidDateOrder() {
        return request().withInvalidDateOrder().build();
    }

    /**
     * Создает успешный ответ
     */
    public static TravelCalculatePremiumResponse successfulResponse() {
        return response().build();
    }

    /**
     * Создает успешный ответ на основе запроса с указанной ценой
     */
    public static TravelCalculatePremiumResponse successfulResponse(
            TravelCalculatePremiumRequest request,
            long price) {
        return response()
                .basedOnRequest(request)
                .withPrice(price)
                .build();
    }

    /**
     * Создает ответ с ошибкой валидации
     */
    public static TravelCalculatePremiumResponse errorResponse(String field, String message) {
        return new TravelCalculatePremiumResponse(
                List.of(new ValidationError(field, message))
        );
    }
}