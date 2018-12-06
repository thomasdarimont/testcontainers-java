package org.testcontainers.keycloak;

import org.junit.Test;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.testcontainers.keycloak.support.KeycloakUser;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class KeycloakContainerTest {

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
                .attributes(Collections.singletonMap("mobile", Arrays.asList("12345"))) //
                .build();

            KeycloakUserInfo info = container.getKeycloakOperations().createUser(user);
            assertThat(info, is(notNullValue()));
            assertThat(info.getUserId(), is(notNullValue()));

            KeycloakUser found = container.getKeycloakOperations().findUserByUserId(info.getUserId());

            assertThat(found, is(notNullValue()));
            assertThat(found.getUsername(), is(equalTo(user.getUsername())));
        }
    }

    /*

    @Test
    public void shouldCreateAdminUserInKeycloak() {
        try (KeycloakContainer container = new KeycloakContainer()) {
            container.start();

            KeycloakUser user = KeycloakUser.builder() //
                .username("admin2") //
                .email("admin2@localhost") //
                .password("test") //
                .realmRoles(Collections.singleton("admin")) //
                .build();

            KeycloakUserInfo info = container.createUser(user);
            assertThat(info, is(notNullValue()));
            assertThat(info.getUserId(), is(notNullValue()));
        }
    }
*/

    @Test
    public void shouldImportExistingRealm() {
        try (KeycloakContainer container = new KeycloakContainer().withImportFile("test-realm-4.7.0.json")) {
            container.start();
        }
    }
}
