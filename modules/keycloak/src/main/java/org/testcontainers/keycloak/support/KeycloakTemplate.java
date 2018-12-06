package org.testcontainers.keycloak.support;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.keycloak.KeycloakUserInfo;

import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class KeycloakTemplate implements KeycloakOperations {

    private final Keycloak keycloak;

    private final String realm;

    @Override
    public <T> T execute(KeycloakCallback callback) {
        return callback.doWithKeycloak(keycloak);
    }

    @Override
    public KeycloakUser findUserByUserId(String userId, String realm) {

        UsersResource usersResource = getUsersResource(realm);
        UserRepresentation rep = usersResource.get(userId).toRepresentation();

        return new KeycloakUser(realm, rep.getUsername(), rep.getEmail(), "*", rep.getFirstName(), rep.getLastName(), rep.getAttributes(), Collections.emptySet(), Collections.emptyMap());
    }

    @Override
    public KeycloakUser findUserByUserId(String userId) {
        return findUserByUserId(userId, this.realm);
    }

    public KeycloakUserInfo createUser(KeycloakUser user) {

        String realm = getRealm(user);

        UsersResource usersResource = getUsersResource(realm);

        try (CloseableResponse response = new CloseableResponse(usersResource.create(toUserRepresentation(user)))) {

            KeycloakUserInfo info = new KeycloakUserInfo();
            info.setRealm(realm);
            info.setUsername(user.getUsername());
            info.setUserId(CreatedResponseUtil.getCreatedId(response.getDelegate()));

            if (!isEmpty(user.getRealmRoles())) {
//                RolesResource rolesResource = realmResource.roles();
                List<RoleRepresentation> rolesToAdd = toRoles(user.getRealmRoles());

//                // Get realm role "tester" (requires view-realm role)
//                RoleRepresentation testerRealmRole = rolesResource//
//                    .get("tester").toRepresentation();

                usersResource.get(info.getUserId()).roles().realmLevel().add(rolesToAdd);
            }

            if (!isEmpty(user.getClientRoles())) {
                for (Map.Entry<String, Set<String>> clientEntry : user.getClientRoles().entrySet()) {
                    List<RoleRepresentation> rolesToAdd = toRoles(clientEntry.getValue());
                    usersResource.get(info.getUserId()).roles().clientLevel(clientEntry.getKey()).add(rolesToAdd);
                }
            }

            return info;
        }
    }

    private UsersResource getUsersResource(String realm) {
        RealmResource realmResource = keycloak.realm(realm);
        return realmResource.users();
    }

    private String getRealm(KeycloakUser user) {
        return Optional.ofNullable(user.getRealm()).orElse(this.realm);
    }

    protected RoleRepresentation toRoleRepresentation(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }

    protected UserRepresentation toUserRepresentation(KeycloakUser user) {

        UserRepresentation rep = new UserRepresentation();

        rep.setUsername(user.getUsername());
        rep.setFirstName(user.getFirstname());
        rep.setLastName(user.getLastname());
        rep.setEmail(user.getEmail());

        rep.setAttributes(user.getAttributes());

        rep.setCredentials(Collections.singletonList(toPasswordCredential(user.getPassword())));

        return rep;
    }

    protected CredentialRepresentation toPasswordCredential(String password) {

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);

        return cred;
    }

    private boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    private boolean isEmpty(Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    @NotNull
    private List<RoleRepresentation> toRoles(Set<String> roleNames) {
        return roleNames.stream().map(this::toRoleRepresentation).collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    static class CloseableResponse implements Closeable {

        private final Response delegate;

        @Override
        public void close() {
            delegate.close();
        }

        public Response getDelegate() {
            return delegate;
        }
    }
}
