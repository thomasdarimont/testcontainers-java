package org.testcontainers.keycloak;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.Base58;

import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.Collections;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.Optional.ofNullable;

/**
 * Represents an keycloak docker instance which exposes by default port 8080.
 * The docker image is by default fetched from jboss/keycloak
 */
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    /**
     * Keycloak Default HTTP port
     */
    private static final int KEYCLOAK_DEFAULT_PORT = 8080;

    /**
     * Keycloak Docker base Image
     */
    public static final String KEYCLOAK_DEFAULT_IMAGE = "jboss/keycloak";

    /**
     * Keycloak Default version
     */
    public static final String KEYCLOAK_DEFAULT_VERSION = "4.6.0.Final";

    public static final String DEFAULT_KEYCLOAK_REALM = "master";

    public static final String DEFAULT_KEYCLOAK_USER = "keycloak";

    public static final String DEFAULT_KEYCLOAK_PASSWORD = "keycloak";

    public static final String DEFAULT_ADMIN_CLI_CLIENT_ID = "admin-cli";

    public static final String DEFAULT_AUTH_PATH = "/auth";

    private Keycloak keycloakAdminClient;

    private String realmName = DEFAULT_KEYCLOAK_REALM;

    private String username = DEFAULT_KEYCLOAK_USER;

    private String password = DEFAULT_KEYCLOAK_PASSWORD;

    public KeycloakContainer() {
        this(KEYCLOAK_DEFAULT_IMAGE + ":" + KEYCLOAK_DEFAULT_VERSION);
    }

    /**
     * Create an Keycloak Container by passing the full docker image name
     *
     * @param dockerImageName Full docker image name, like: jboss/keycloak:4.5.0.Final
     */
    public KeycloakContainer(String dockerImageName) {
        super(dockerImageName);
        logger().info("Starting an keycloak container using [{}]", dockerImageName);
        withNetworkAliases("keycloak-" + Base58.randomString(6));
        withEnv("KEYCLOAK_USER", DEFAULT_KEYCLOAK_USER);
        withEnv("KEYCLOAK_PASSWORD", DEFAULT_KEYCLOAK_PASSWORD);
        addExposedPorts(KEYCLOAK_DEFAULT_PORT);
        setWaitStrategy(createWaitStrategy());
        withLogConsumer(new Slf4jLogConsumer(logger()));
    }

    private WaitStrategy createWaitStrategy() {
        return new HttpWaitStrategy()
            .forPort(KEYCLOAK_DEFAULT_PORT)
            .forPath(DEFAULT_AUTH_PATH)
            .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED)
            .withStartupTimeout(Duration.ofMinutes(2));
    }

    public String getServerUrl() {
        return String.format("%s://%s:%s%s", "http", getContainerIpAddress(), getMappedPort(KEYCLOAK_DEFAULT_PORT), DEFAULT_AUTH_PATH);
    }

    public KeycloakContainer withRealm(String realmName) {
        this.realmName = realmName;
        return self();
    }

    public KeycloakContainer withUsername(String username) {
        this.username = username;
        return self();
    }

    public KeycloakContainer withPassword(String password) {
        this.password = password;
        return self();
    }

    @Override
    protected void doStart() {
        super.doStart();
        this.keycloakAdminClient = createKeycloakClient(realmName);
    }

    public Keycloak getKeycloakAdminClient() {
        return keycloakAdminClient;
    }

    protected Keycloak createKeycloakClient(String realmName) {
        return KeycloakBuilder.builder() //
            .realm(realmName) //
            .serverUrl(getServerUrl())
            .clientId(DEFAULT_ADMIN_CLI_CLIENT_ID) //
            .username(username) //
            .password(password) //
            .build();
    }

    public KeycloakUserInfo createUser(KeycloakUser user) {
        String realm = ofNullable(this.realmName).orElse(DEFAULT_KEYCLOAK_REALM);

        RealmResource realmResource = getKeycloakAdminClient().realm(realm);
        Response response = realmResource.users().create(toUserRepresentation(user));

        KeycloakUserInfo info = new KeycloakUserInfo();
        info.setRealm(realm);
        info.setUsername(user.getUsername());
        info.setUserId(CreatedResponseUtil.getCreatedId(response));

        return info;
    }

    protected UserRepresentation toUserRepresentation(KeycloakUser user) {

        UserRepresentation rep = new UserRepresentation();

        rep.setUsername(user.getUsername());
        rep.setFirstName(user.getFirstname());
        rep.setLastName(user.getLastname());
        rep.setEmail(user.getEmail());

        rep.setCredentials(Collections.singletonList(toPasswordCredential(user.getPassword())));

        return rep;
    }

    protected CredentialRepresentation toPasswordCredential(String password) {

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);

        return cred;
    }
}
