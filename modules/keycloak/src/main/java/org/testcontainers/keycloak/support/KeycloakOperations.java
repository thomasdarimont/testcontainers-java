package org.testcontainers.keycloak.support;

import org.testcontainers.keycloak.KeycloakUserInfo;

public interface KeycloakOperations {

    <T> T execute(KeycloakCallback callback);

    KeycloakUserInfo createUser(KeycloakUser user);

    KeycloakUser findUserByUserId(String userId);

    KeycloakUser findUserByUserId(String userId, String realm);
}
