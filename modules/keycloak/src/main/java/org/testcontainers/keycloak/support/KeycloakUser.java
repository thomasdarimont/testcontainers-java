package org.testcontainers.keycloak.support;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class KeycloakUser {

    String realm;

    String username;

    String email;

    String password;

    String firstname;

    String lastname;

    /**
     * Holds the user attribute mapping
     */
    Map<String, List<String>> attributes;


    /**
     * Holds the realm-role names
     */
    Set<String> realmRoles;

    /**
     * Holds the clientId to client-role name mapping
     */
    Map<String, Set<String>> clientRoles;
}
