package kln.ams.residentmanagement.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import kln.ams.residentmanagement.security.RequestIdContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    @Builder.Default
    private boolean success = false;

    private String message;

    private ErrorDetail error;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    private String requestId;

    public static ApiErrorResponse of(String code, String message) {
        return ApiErrorResponse.builder()
                .success(false)
                .message(message)
                .error(new ErrorDetail(code, null))
                .timestamp(Instant.now().toString())
                .requestId(RequestIdContext.getRequestId())
                .build();
    }

    public static ApiErrorResponse of(String code, String message, Object details) {
        return ApiErrorResponse.builder()
                .success(false)
                .message(message)
                .error(new ErrorDetail(code, details))
                .timestamp(Instant.now().toString())
                .requestId(RequestIdContext.getRequestId())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private Object details;
    }
}
