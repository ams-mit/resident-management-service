package kln.ams.residentmanagement.security;

import java.util.UUID;

public class RequestIdContext {

    private static final ThreadLocal<String> CURRENT_REQUEST_ID = new ThreadLocal<>();

    public static void setRequestId(String requestId) {
        CURRENT_REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        String id = CURRENT_REQUEST_ID.get();
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            CURRENT_REQUEST_ID.set(id);
        }
        return id;
    }

    public static String getRequestIdHeaderName() {
        return "X-Request-ID";
    }

    public static void clear() {
        CURRENT_REQUEST_ID.remove();
    }
}
