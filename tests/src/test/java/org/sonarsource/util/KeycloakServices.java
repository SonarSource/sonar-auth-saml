/*
 * SAML 2.0 Authentication for SonarQube
 * Copyright (C) 2018-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.util.ItUtils.SAML_REAML;
import static org.sonarsource.util.ItUtils.SAML_SERVER_URL;

public class KeycloakServices extends ExternalResource {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private static final String MASTER_REAML = "master";
  private static final String ADMIN_CLIENT_ID = "admin-cli";
  private static final String ADMIN_LOGIN = "admin";
  private static final String ADMIN_PASSWORD = "admin";

  private final RealmResource realmResource;
  private Set<String> roles = new HashSet<>();

  public KeycloakServices() {
    Keycloak keycloak = KeycloakBuilder.builder()
      .serverUrl(SAML_SERVER_URL + "/auth")
      .realm(MASTER_REAML)
      .username(ADMIN_LOGIN)
      .password(ADMIN_PASSWORD)
      .clientId(ADMIN_CLIENT_ID)
      .build();
    this.realmResource = keycloak.realm(SAML_REAML);
  }

  @Override
  protected void before() {
    roles = new HashSet<>();
  }

  @Override
  protected void after() {
    realmResource.users().list().forEach(this::deleteUser);
    roles.forEach(this::deleteRole);
    roles = new HashSet<>();
  }

  private UserRepresentation searchUser(String login) {
    List<UserRepresentation> search = realmResource.users().search(login);
    assertThat(search.size()).isEqualTo(1);
    return search.get(0);
  }

  public UserRepresentation createUser() {
    return createUser("User Name");
  }

  public UserRepresentation createUser(@Nullable String name) {
    int id = ID_GENERATOR.getAndIncrement();
    String login = "login" + id;
    CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
    credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
    credentialRepresentation.setValue(login);

    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername(login);
    userRepresentation.setCredentials(singletonList(credentialRepresentation));
    userRepresentation.setEnabled(true);
    if (name != null) {
      userRepresentation.singleAttribute("Name", name);
    }
    realmResource.users().create(userRepresentation);

    return searchUser(login);
  }

  public void updateUser(UserRepresentation user) {
    realmResource.users().get(user.getId()).update(user);
  }

  private void deleteUser(UserRepresentation userRepresentation) {
    realmResource.users().delete(userRepresentation.getId());
  }

  public void addRolesToUser(UserRepresentation user, String... roles) {
    List<RoleRepresentation> roleRepresentations = stream(roles)
      .map(role -> realmResource.roles().get(role))
      .map(RoleResource::toRepresentation)
      .collect(Collectors.toList());
    realmResource.users().get(user.getId()).roles().realmLevel().add(roleRepresentations);
  }

  public void createRoles(String... roleNames) {
    stream(roleNames).forEach(roleName -> {
      realmResource.roles().create(new RoleRepresentation(roleName, null, false));
      this.roles.add(roleName);
    });
  }

  private void deleteRole(String roleName) {
    realmResource.roles().deleteRole(roleName);
  }

}
