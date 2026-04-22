package org.kshrd.hrdroomservice.config.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object claim = jwt.getClaim("realm_access");
        if (!(claim instanceof Map<?, ?> realm)) {
            return List.of();
        }
        Object rolesObj = realm.get("roles");
        if (!(rolesObj instanceof List<?> raw)) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object o : raw) {
            if (o == null) {
                continue;
            }
            String role = o.toString().trim();
            if (role.isEmpty()) {
                continue;
            }
            if (role.toLowerCase(Locale.ROOT).startsWith("role_")) {
                role = role.substring(5);
            }
            role = role.toLowerCase(Locale.ROOT);
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }
}
