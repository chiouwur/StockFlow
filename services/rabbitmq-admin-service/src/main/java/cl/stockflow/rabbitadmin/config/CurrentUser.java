package cl.stockflow.rabbitadmin.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class CurrentUser {
    private CurrentUser() { }

    public static String id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a instanceof JwtAuthenticationToken j) {
            Jwt t = j.getToken();
            String oid = t.getClaimAsString("oid");
            return oid != null ? oid : t.getSubject();
        }
        return "local-user";
    }
}
