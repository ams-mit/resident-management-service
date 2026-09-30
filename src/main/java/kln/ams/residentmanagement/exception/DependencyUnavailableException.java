package kln.ams.residentmanagement.exception;

import lombok.Getter;

@Getter
public class DependencyUnavailableException extends RuntimeException {

    private final String code;
    private final String serviceName;

    public DependencyUnavailableException(String serviceName, String message) {
        super(message);
        this.code = "DEPENDENCY_UNAVAILABLE";
        this.serviceName = serviceName;
    }

    public DependencyUnavailableException(String code, String serviceName, String message) {
        super(message);
        this.code = code;
        this.serviceName = serviceName;
    }
}
