package org.testcontainers.keycloak.support;

import org.keycloak.admin.client.Keycloak;

public interface KeycloakCallback {

    <T> T doWithKeycloak(Keycloak keycloak);
}
