package kln.ams.residentmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class SecurityUtils {

    public static String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        String targetAuthority = "ROLE_" + role;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (targetAuthority.equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isManagerOrAdmin() {
        return hasRole("SYSTEM_ADMINISTRATOR") || hasRole("APARTMENT_MANAGER");
    }

    public static boolean isSelfOrManager(String targetUserId) {
        if (isManagerOrAdmin()) {
            return true;
        }
        String currentUserId = getAuthenticatedUserId();
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    public static String getAuthenticatedService() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String type = jwt.getClaimAsString("type");
            if ("service".equals(type)) {
                return jwt.getSubject();
            }
        }
        return null;
    }
}
