package org.javaguru.travel.insurance.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.v3.TravelCalculatePremiumRequestV3;
import org.javaguru.travel.insurance.application.dto.v3.TravelCalculatePremiumResponseV3;
import org.javaguru.travel.insurance.application.service.TravelCalculatePremiumServiceV3;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер V3 для расчёта страховых премий нескольких персон.
 *
 * task_135: Новый контроллер для v3 API.
 *
 * НОВЫЕ ЭНДПОИНТЫ:
 *   POST /insurance/travel/v3/calculate    — расчёт для нескольких персон
 *   GET  /insurance/travel/v3/countries    — справочник стран (совместим с v2)
 *   GET  /insurance/travel/v3/coverage-levels — уровни медицинского покрытия
 *   GET  /insurance/travel/v3/risk-types   — типы рисков
 *
 * ОБРАТНАЯ СОВМЕСТИМОСТЬ:
 *   V2 контроллер (TravelCalculatePremiumController) не изменяется.
 *   Оба контроллера используют один сервисный слой через разные адаптеры.
 *
 * HTTP-СТАТУСЫ (те же что и в V2):
 *   200 OK          → SUCCESS
 *   400 Bad Request → VALIDATION_ERROR
 *   202 Accepted    → REQUIRES_REVIEW
 *   422 Unprocessable → DECLINED
 */
@Slf4j
@RestController
@RequestMapping("/insurance/travel/v3")
@RequiredArgsConstructor
@Tag(name = "Premium Calculation V3", description = "Расчёт страховой премии V3 с поддержкой нескольких застрахованных")
public class TravelCalculatePremiumControllerV3 {

    private final TravelCalculatePremiumServiceV3 calculatePremiumServiceV3;

    /**
     * Расчёт страховой премии V3 для нескольких застрахованных персон.
     *
     * Поле persons[] в запросе заменяет personFirstName/personLastName/personBirthDate.
     * В ответе добавляется массив personPremiums[] с индивидуальными расчётами.
     */
    @Operation(
            summary = "Расчёт страховой премии V3 (multi-person)",
            description = """
                    Рассчитывает страховую премию для одного или нескольких застрахованных.
                    
                    ОТЛИЧИЕ ОТ V2:
                    - Запрос: поле persons[] вместо одиночных personFirstName/personLastName/personBirthDate
                    - Ответ: массив personPremiums[] с индивидуальными расчётами по каждой персоне
                    
                    Ошибки валидации адресуются с индексом персоны:
                      persons[0].personBirthDate — Must not be empty
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Премия успешно рассчитана",
                    content = @Content(schema = @Schema(implementation = TravelCalculatePremiumResponseV3.class))),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации"),
            @ApiResponse(responseCode = "202", description = "Требуется ручная проверка андеррайтера"),
            @ApiResponse(responseCode = "422", description = "Заявка отклонена")
    })
    @PostMapping(
            path = "/calculate",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<TravelCalculatePremiumResponseV3> calculatePremium(
            @RequestBody TravelCalculatePremiumRequestV3 request) {

        log.info("V3 premium calculation request: {} persons, country: {}",
                request.getPersons() != null ? request.getPersons().size() : 0,
                request.getCountryIsoCode());

        TravelCalculatePremiumResponseV3 response = calculatePremiumServiceV3.calculatePremium(request);

        logResponse(response);

        return ResponseEntity
                .status(determineHttpStatus(response))
                .body(response);
    }

    /**
     * Справочник стран (совместим с V2).
     *
     * Возвращает список стран, доступных для страхования.
     * Идентичен эндпоинту V2 /insurance/travel/v2/countries.
     */
    @Operation(
            summary = "Справочник стран V3",
            description = "Возвращает список стран, доступных для страхования. Совместим с V2."
    )
    @GetMapping(path = "/countries", produces = "application/json")
    public ResponseEntity<CountriesResponse> getCountries() {
        log.debug("V3 countries reference request");
        return ResponseEntity.ok(CountriesResponse.placeholder());
    }

    /**
     * Уровни медицинского покрытия.
     */
    @Operation(
            summary = "Уровни медицинского покрытия V3",
            description = "Возвращает список уровней медицинского покрытия с базовыми дневными ставками."
    )
    @GetMapping(path = "/coverage-levels", produces = "application/json")
    public ResponseEntity<CoverageLevelsResponse> getCoverageLevels() {
        log.debug("V3 coverage levels reference request");
        return ResponseEntity.ok(CoverageLevelsResponse.placeholder());
    }

    /**
     * Типы рисков.
     */
    @Operation(
            summary = "Типы рисков V3",
            description = "Возвращает список доступных типов рисков с коэффициентами."
    )
    @GetMapping(path = "/risk-types", produces = "application/json")
    public ResponseEntity<RiskTypesResponse> getRiskTypes() {
        log.debug("V3 risk types reference request");
        return ResponseEntity.ok(RiskTypesResponse.placeholder());
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    /**
     * Определяет HTTP статус на основе статуса ответа.
     */
    private int determineHttpStatus(TravelCalculatePremiumResponseV3 response) {
        return switch (response.getStatus()) {
            case SUCCESS          -> 200;
            case VALIDATION_ERROR -> 400;
            case DECLINED         -> 422;
            case REQUIRES_REVIEW  -> 202;
        };
    }

    /**
     * Логирует ответ с соответствующим уровнем.
     */
    private void logResponse(TravelCalculatePremiumResponseV3 response) {
        switch (response.getStatus()) {
            case SUCCESS -> {
                var premium = response.getPricing() != null
                        ? response.getPricing().getTotalPremium()
                        : null;
                var currency = response.getPricing() != null
                        ? response.getPricing().getCurrency()
                        : "EUR";
                int personsCount = response.getPersonPremiums() != null
                        ? response.getPersonPremiums().size()
                        : 0;
                log.info("V3 premium calculated: {} {} for {} person(s) (requestId: {})",
                        premium, currency, personsCount, response.getRequestId());
            }
            case VALIDATION_ERROR ->
                    log.warn("V3 validation errors (requestId: {}): {}",
                            response.getRequestId(),
                            response.getErrors() != null ? response.getErrors().size() : 0);
            case DECLINED ->
                    log.warn("V3 application declined (requestId: {}): {}",
                            response.getRequestId(),
                            response.getUnderwriting() != null
                                    ? response.getUnderwriting().getReason()
                                    : "Unknown reason");
            case REQUIRES_REVIEW ->
                    log.info("V3 application requires manual review (requestId: {}): {}",
                            response.getRequestId(),
                            response.getUnderwriting() != null
                                    ? response.getUnderwriting().getReason()
                                    : "Unknown reason");
        }
    }

    // ── Вспомогательные DTO для справочных эндпоинтов ─────────────────────────

    /**
     * Заглушка для ответа со странами.
     * В production должна загружаться из CountryRepository.
     */
    public record CountriesResponse(String message) {
        public static CountriesResponse placeholder() {
            return new CountriesResponse(
                    "Use GET /insurance/travel/countries for full list. V3 endpoint placeholder."
            );
        }
    }

    /**
     * Заглушка для ответа с уровнями покрытия.
     */
    public record CoverageLevelsResponse(String message) {
        public static CoverageLevelsResponse placeholder() {
            return new CoverageLevelsResponse(
                    "Use GET /insurance/travel/coverage-levels for full list. V3 endpoint placeholder."
            );
        }
    }

    /**
     * Заглушка для ответа с типами рисков.
     */
    public record RiskTypesResponse(String message) {
        public static RiskTypesResponse placeholder() {
            return new RiskTypesResponse(
                    "Use GET /insurance/travel/risk-types for full list. V3 endpoint placeholder."
            );
        }
    }
}