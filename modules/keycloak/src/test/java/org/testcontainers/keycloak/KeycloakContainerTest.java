package org.testcontainers.keycloak;

import org.junit.After;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.ServerInfoRepresentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class KeycloakContainerTest {

    private Keycloak keycloak;


    @After
    public void stopRestClient() {
        if (keycloak != null) {
            keycloak.close();
            keycloak = null;
        }
    }

    @Test
    public void shouldStartKeycloakAndReturnServerInfo() {
        try (KeycloakContainer container = new KeycloakContainer()) {
            container.start();

            ServerInfoRepresentation info = container.getKeycloakAdminClient().serverInfo().getInfo();
            assertThat(info, is(notNullValue()));
        }
    }


    @Test
    public void shouldCreateUserInKeycloak() {
        try (KeycloakContainer container = new KeycloakContainer()) {
            container.start();

            KeycloakUser user = KeycloakUser.builder() //
                .username("tester") //
                .email("tester@localhost") //
                .password("test") //
                .build();

            KeycloakUserInfo info = container.createUser(user);
            assertThat(info, is(notNullValue()));
            assertThat(info.getUserId(), is(notNullValue()));
        }
    }
}
