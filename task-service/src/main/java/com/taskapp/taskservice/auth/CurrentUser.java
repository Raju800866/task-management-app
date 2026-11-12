package com.taskapp.taskservice.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/** The caller, read from the verified JWT. The id never comes from the request body. */
public record CurrentUser(Long id, String email, String name) {

    public static CurrentUser from(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return new CurrentUser(Long.valueOf(jwt.getSubject()), email, displayName(jwt.getClaimAsString("name"), email));
    }

    // Tokens issued before the "name" claim existed fall back to the email's local part
    private static String displayName(String name, String email) {
        if (name != null && !name.isBlank()) return name.trim();
        if (email != null && email.contains("@")) return email.substring(0, email.indexOf('@'));
        return "Someone";
    }
}
