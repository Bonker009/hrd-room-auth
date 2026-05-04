package org.kshrd.hrdroomservice.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.config.security.KeycloakAuthProperties;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthServiceTest {

    @Mock private Keycloak keycloak;
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;
    @Mock private RolesResource rolesResource;
    @Mock private RoleResource studentRoleResource;
    @Mock private RoleResource teacherRoleResource;
    @Mock private RoleMappingResource roleMappingResource;
    @Mock private RoleScopeResource roleScopeResource;

    @Test
    void changeStudentToTeacher_removesStudentAndAddsTeacher() {
        UUID userId = UUID.randomUUID();
        KeycloakAuthService service =
                spy(new KeycloakAuthService(properties(), new ObjectMapper()));
        doReturn(keycloak).when(service).buildAdminClient();
        mockCommonGraph(userId);

        when(roleScopeResource.listAll()).thenReturn(List.of(role("student")));
        when(studentRoleResource.toRepresentation()).thenReturn(role("student"));
        when(teacherRoleResource.toRepresentation()).thenReturn(role("teacher"));

        service.changeStudentToTeacher(userId);

        verify(roleScopeResource).remove(anyList());
        verify(roleScopeResource).add(anyList());
    }

    @Test
    void changeStudentToTeacher_isIdempotent_whenAlreadyTeacherOnly() {
        UUID userId = UUID.randomUUID();
        KeycloakAuthService service =
                spy(new KeycloakAuthService(properties(), new ObjectMapper()));
        doReturn(keycloak).when(service).buildAdminClient();
        mockCommonGraph(userId);

        when(roleScopeResource.listAll()).thenReturn(List.of(role("teacher")));
        when(studentRoleResource.toRepresentation()).thenReturn(role("student"));
        when(teacherRoleResource.toRepresentation()).thenReturn(role("teacher"));

        service.changeStudentToTeacher(userId);

        verify(roleScopeResource, never()).remove(anyList());
        verify(roleScopeResource, never()).add(anyList());
    }

    @Test
    void changeStudentToTeacher_returnsNotFound_whenStudentIdDoesNotExist() {
        UUID userId = UUID.randomUUID();
        KeycloakAuthService service =
                spy(new KeycloakAuthService(properties(), new ObjectMapper()));
        doReturn(keycloak).when(service).buildAdminClient();

        when(keycloak.realm("hrd")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenThrow(new NotFoundException());

        ApiException ex =
                assertThrows(ApiException.class, () -> service.changeStudentToTeacher(userId));
        assertEquals("NOT_FOUND", ex.getErrorCode());
        assertEquals("User or role was not found in identity provider", ex.getMessage());
    }

    private void mockCommonGraph(UUID userId) {
        when(keycloak.realm("hrd")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(new UserRepresentation());
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("student")).thenReturn(studentRoleResource);
        when(rolesResource.get("teacher")).thenReturn(teacherRoleResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
    }

    private static KeycloakAuthProperties properties() {
        KeycloakAuthProperties properties = new KeycloakAuthProperties();
        properties.setRealm("hrd");
        properties.setAuthServerUrl("http://localhost:8080");
        properties.setClientId("client");
        properties.setClientSecret("secret");
        return properties;
    }

    private static RoleRepresentation role(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }
}
