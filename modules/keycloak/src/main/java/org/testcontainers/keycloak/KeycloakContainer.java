package org.testcontainers.keycloak;

import lombok.Getter;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.keycloak.support.KeycloakOperations;
import org.testcontainers.keycloak.support.KeycloakTemplate;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Represents an keycloak docker instance which exposes by default port 8080.
 * The docker image is by default fetched from jboss/keycloak
 */
@Getter
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
    public static final String KEYCLOAK_DEFAULT_VERSION = "4.7.0.Final";

    public static final String DEFAULT_KEYCLOAK_REALM = "master";

    public static final String DEFAULT_KEYCLOAK_USER = "keycloak";

    public static final String DEFAULT_KEYCLOAK_PASSWORD = "keycloak";

    public static final String DEFAULT_ADMIN_CLI_CLIENT_ID = "admin-cli";

    public static final String DEFAULT_AUTH_PATH = "/auth";

    private Keycloak keycloakAdminClient;

    private KeycloakOperations keycloakOperations;

    private String adminClientId = DEFAULT_ADMIN_CLI_CLIENT_ID;

    private String realmName = DEFAULT_KEYCLOAK_REALM;

    private String username = DEFAULT_KEYCLOAK_USER;

    private String password = DEFAULT_KEYCLOAK_PASSWORD;

    private String importFile;

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
        addExposedPorts(KEYCLOAK_DEFAULT_PORT);
        setWaitStrategy(createWaitStrategy());
//        withLogConsumer(new Slf4jLogConsumer(logger()));
    }

    @Override
    protected void configure() {
        withEnv("KEYCLOAK_USER", username);
        withEnv("KEYCLOAK_PASSWORD", password);

        // avoid bootstrap of infinispan clustering
        withCommand("-c standalone.xml");

        if (importFile != null) {

            String pathInContainer = "/tmp/" + importFile;
            withCopyFileToContainer(MountableFile.forClasspathResource(importFile), pathInContainer);

            withEnv("KEYCLOAK_IMPORT", pathInContainer);
        }
    }

    private WaitStrategy createWaitStrategy() {

        return Wait
            .forHttp(DEFAULT_AUTH_PATH)
            .forPort(KEYCLOAK_DEFAULT_PORT)
            .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED)
            .withStartupTimeout(Duration.ofMinutes(2));
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

    public KeycloakContainer withImportFile(String importFile) {
        this.importFile = importFile;
        return self();
    }

    @Override
    protected void doStart() {
        super.doStart();
        this.keycloakAdminClient = createKeycloakClient(realmName);
        this.keycloakOperations = new KeycloakTemplate(keycloakAdminClient, realmName);
    }

    @Override
    public void stop() {

        if (this.keycloakAdminClient != null) {
            this.keycloakAdminClient.close();
            this.keycloakAdminClient = null;
        }

        super.stop();
    }

    protected Keycloak createKeycloakClient(String realmName) {
        return KeycloakBuilder.builder() //
            .realm(realmName) //
            .serverUrl(getAuthServerUrl())
            .clientId(adminClientId) //
            .username(username) //
            .password(password) //
            .build();
    }

    public KeycloakOperations getKeycloakOperations() {
        return keycloakOperations;
    }

    public Keycloak getKeycloakAdminClient() {
        return keycloakAdminClient;
    }

    public String getAuthServerUrl() {
        return String.format("http://%s:%s%s", getContainerIpAddress(), getMappedPort(KEYCLOAK_DEFAULT_PORT), DEFAULT_AUTH_PATH);
    }
}
