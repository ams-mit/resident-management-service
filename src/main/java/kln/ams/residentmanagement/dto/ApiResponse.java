package kln.ams.residentmanagement.dto;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private boolean success = true;

    private String message;

    private T data;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    private String requestId;

    public static <T> ApiResponse<T> of(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation completed successfully")
                .data(data)
                .timestamp(Instant.now().toString())
                .requestId(RequestIdContext.getRequestId())
                .build();
    }

    public static <T> ApiResponse<T> of(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now().toString())
                .requestId(RequestIdContext.getRequestId())
                .build();
    }
}
