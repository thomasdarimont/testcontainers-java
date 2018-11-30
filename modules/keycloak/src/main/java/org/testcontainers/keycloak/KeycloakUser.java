package org.testcontainers.keycloak;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeycloakUser {

    String realm;

    String username;

    String email;

    String password;

    String firstname;

    String lastname;
}
