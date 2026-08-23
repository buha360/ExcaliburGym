package com.wardanger.excalibur.security;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;

import com.wardanger.excalibur.generated.model.ApiError;
import com.wardanger.excalibur.shared.time.TimeConfiguration;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ApiError(
                code,
                message,
                OffsetDateTime.ofInstant(clock.instant(), TimeConfiguration.APPLICATION_ZONE)));
    }
}
