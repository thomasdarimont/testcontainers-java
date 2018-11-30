package org.testcontainers.keycloak;

import lombok.Data;

@Data
public class KeycloakUserInfo {

    String realm;

    String userId;

    String username;
}
