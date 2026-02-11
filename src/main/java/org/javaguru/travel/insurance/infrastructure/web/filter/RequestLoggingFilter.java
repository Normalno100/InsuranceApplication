package org.javaguru.travel.insurance.infrastructure.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Фильтр для логирования REST запросов и ответов в JSON формате
 *
 * Логирует:
 * - Полную информацию о запросе (метод, URI, headers, body)
 * - Полную информацию об ответе (статус, headers, body)
 * - Время обработки запроса
 * - Timestamp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Пропускаем health check и статические ресурсы
        if (shouldNotLog(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Оборачиваем request и response для возможности чтения body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Выполняем запрос
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Логируем запрос и ответ
            logRequestAndResponse(wrappedRequest, wrappedResponse, duration);

            // ВАЖНО: копируем body обратно в response
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Проверяет, нужно ли логировать запрос
     */
    private boolean shouldNotLog(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // Не логируем:
        return uri.contains("/health") ||          // health checks
                uri.contains("/actuator") ||        // Spring Actuator endpoints
                uri.contains("/swagger") ||         // Swagger UI
                uri.contains("/v3/api-docs") ||     // OpenAPI docs
                uri.contains("/favicon.ico") ||     // favicon
                uri.contains("/static/") ||         // статика
                uri.contains("/webjars/");          // webjars
    }

    /**
     * Логирует запрос и ответ в JSON формате
     */
    private void logRequestAndResponse(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response,
                                       long duration) {
        try {
            Map<String, Object> logData = new LinkedHashMap<>();

            // Timestamp
            logData.put("timestamp", LocalDateTime.now().toString());

            // Время выполнения
            logData.put("duration_ms", duration);

            // Информация о запросе
            logData.put("request", buildRequestInfo(request));

            // Информация об ответе
            logData.put("response", buildResponseInfo(response));

            // Логируем в зависимости от статуса
            String jsonLog = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(logData);

            int status = response.getStatus();
            if (status >= 500) {
                log.error("REST API Call:\n{}", jsonLog);
            } else if (status >= 400) {
                log.warn("REST API Call:\n{}", jsonLog);
            } else {
                log.info("REST API Call:\n{}", jsonLog);
            }

        } catch (Exception e) {
            log.error("Error logging request/response", e);
        }
    }

    /**
     * Собирает информацию о запросе
     */
    private Map<String, Object> buildRequestInfo(ContentCachingRequestWrapper request) {
        Map<String, Object> requestInfo = new LinkedHashMap<>();

        requestInfo.put("method", request.getMethod());
        requestInfo.put("uri", request.getRequestURI());
        requestInfo.put("query_string", request.getQueryString());
        requestInfo.put("remote_addr", request.getRemoteAddr());

        // Headers (исключаем чувствительные)
        requestInfo.put("headers", getSafeHeaders(request));

        // Body
        String body = getRequestBody(request);
        if (body != null && !body.isEmpty()) {
            try {
                // Пытаемся распарсить как JSON для красивого вывода
                Object jsonBody = objectMapper.readValue(body, Object.class);
                requestInfo.put("body", jsonBody);
            } catch (Exception e) {
                // Если не JSON, пишем как строку
                requestInfo.put("body", body);
            }
        }

        return requestInfo;
    }

    /**
     * Собирает информацию об ответе
     */
    private Map<String, Object> buildResponseInfo(ContentCachingResponseWrapper response) {
        Map<String, Object> responseInfo = new LinkedHashMap<>();

        responseInfo.put("status", response.getStatus());

        // Headers
        Map<String, String> headers = new LinkedHashMap<>();
        for (String headerName : response.getHeaderNames()) {
            headers.put(headerName, response.getHeader(headerName));
        }
        responseInfo.put("headers", headers);

        // Body
        String body = getResponseBody(response);
        if (body != null && !body.isEmpty()) {
            try {
                // Пытаемся распарсить как JSON
                Object jsonBody = objectMapper.readValue(body, Object.class);
                responseInfo.put("body", jsonBody);
            } catch (Exception e) {
                responseInfo.put("body", body);
            }
        }

        return responseInfo;
    }

    /**
     * Получает headers без чувствительной информации
     */
    private Map<String, String> getSafeHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();

        // Чувствительные заголовки, которые не логируем
        Set<String> sensitiveHeaders = Set.of(
                "authorization",
                "cookie",
                "x-api-key",
                "x-auth-token"
        );

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String lowerName = headerName.toLowerCase();

            if (sensitiveHeaders.contains(lowerName)) {
                headers.put(headerName, "***REDACTED***");
            } else {
                headers.put(headerName, request.getHeader(headerName));
            }
        }

        return headers;
    }

    /**
     * Извлекает body из запроса
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Извлекает body из ответа
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }
}