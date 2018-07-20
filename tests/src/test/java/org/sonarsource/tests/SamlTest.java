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
package org.sonarsource.tests;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.WsUsers.GroupsWsResponse.Group;
import org.sonarqube.ws.WsUsers.SearchWsResponse.User;
import org.sonarqube.ws.client.user.GroupsRequest;
import org.sonarqube.ws.client.user.SearchRequest;
import org.sonarqube.ws.client.user.UsersService;
import org.sonarqube.ws.client.usergroup.CreateWsRequest;
import org.sonarqube.ws.client.usergroup.DeleteWsRequest;
import org.sonarsource.util.KeycloakServices;

import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.util.ItUtils.SAML_CLIENT_ID;
import static org.sonarsource.util.ItUtils.SAML_REAML;
import static org.sonarsource.util.ItUtils.SAML_SERVER_URL;
import static org.sonarsource.util.ItUtils.newAdminWsClient;
import static org.sonarsource.util.ItUtils.openBrowser;
import static org.sonarsource.util.ItUtils.setGlobalSetting;

public class SamlTest {

  @ClassRule
  public static Orchestrator orchestrator = SamlTestSuite.ORCHESTRATOR;

  /**
   * Note that the KeycloakServices is not isolated : each test is using the saml realm 'QA' and all users are deleted after each test.
   * As a consequence, there may appear some FPs if 2 tests are executed at the same time (2 PRs, 1 PR and one local execution, etc.)
   */
  @Rule
  public KeycloakServices keycloakServices = new KeycloakServices();

  @After
  public void deleteUsers() {
    UsersService users = newAdminWsClient(orchestrator).users();
    users.search(SearchRequest.builder().build()).getUsersList()
      .stream()
      .filter(u -> !"admin".equals(u.getLogin()))
      .forEach(
        user -> users.deactivate(user.getLogin()));
  }

  @Test
  public void authenticate_simple_user() {
    initSamlSettings();
    UserRepresentation userRepresentation = keycloakServices.createUser();
    String login = userRepresentation.getUsername();

    openBrowser(orchestrator)
      .logIn()
      .useOAuth2()
      .submitCredentials(login, login)
      .shouldBeLoggedIn();

    User user = getUser(login);
    assertThat(user.getName()).isEqualTo("User Name");
    assertThat(user.hasEmail()).isFalse();
    assertThat(getGroups(login)).containsExactlyInAnyOrder("sonar-users");
  }

  @Test
  public void authenticate_user_with_email() {
    initSamlSettings();
    UserRepresentation userRepresentation = keycloakServices.createUser();
    userRepresentation.setEmail("user@email.com");
    keycloakServices.updateUser(userRepresentation);
    String login = userRepresentation.getUsername();

    openBrowser(orchestrator)
      .logIn()
      .useOAuth2()
      .submitCredentials(login, login)
      .shouldBeLoggedIn();

    User user = getUser(login);
    assertThat(user.getEmail()).isEqualTo("user@email.com");
  }

  @Test
  public void authenticate_user_with_groups() {
    try {
      initSamlSettings();
      // 3 groups are created in SonarQube
      createGroups("group1", "group2", "group3");
      // 3 roles are created in keycloak and associated to the user, but the last one has a different name
      keycloakServices.createRoles("group1", "group2", "group4");
      UserRepresentation userRepresentation = keycloakServices.createUser();
      keycloakServices.addRolesToUser(userRepresentation, "group1", "group2", "group4");
      String login = userRepresentation.getUsername();

      openBrowser(orchestrator)
        .logIn()
        .useOAuth2()
        .submitCredentials(login, login)
        .shouldBeLoggedIn();

      // Only the 2 groups having same name in SQ and Keycloak are associated to the user in SonarQube
      assertThat(getGroups(login)).containsExactlyInAnyOrder("sonar-users", "group1", "group2");
    } finally {
      deleteGroups("group1", "group2", "group3");
    }
  }

  @Test
  public void fail_to_authenticate_user_without_name() {
    initSamlSettings();
    UserRepresentation userRepresentation = keycloakServices.createUser(null);
    String login = userRepresentation.getUsername();

    openBrowser(orchestrator)
      .logIn()
      .useOAuth2()
      .submitCredentials(login, login);
    $("#bd").shouldHave(Condition.text("You're not authorized to access this page. Please contact the administrator"));

    WsUsers.SearchWsResponse search = newAdminWsClient(orchestrator).users().search(SearchRequest.builder().setQuery(login).build());
    assertThat(search.getUsersList()).isEmpty();
  }

  private User getUser(String login) {
    WsUsers.SearchWsResponse search = newAdminWsClient(orchestrator).users().search(SearchRequest.builder().setQuery(login).build());
    assertThat(search.getUsersList()).hasSize(1);
    return search.getUsers(0);
  }

  private Set<String> getGroups(String login) {
    WsUsers.GroupsWsResponse groups = newAdminWsClient(orchestrator).users().groups(GroupsRequest.builder().setLogin(login).build());
    return groups.getGroupsList().stream().map(Group::getName).collect(Collectors.toSet());
  }

  private void createGroups(String... groups) {
    Arrays.stream(groups).forEach(group -> newAdminWsClient(orchestrator).userGroups().create(CreateWsRequest.builder().setName(group).build()));
  }

  private void deleteGroups(String... groups) {
    Arrays.stream(groups).forEach(group -> newAdminWsClient(orchestrator).userGroups().delete(DeleteWsRequest.builder().setName(group).build()));
  }

  private void initSamlSettings() {
    setGlobalSetting(orchestrator,
      "sonar.auth.saml.applicationId", SAML_CLIENT_ID,
      "sonar.auth.saml.providerId", SAML_SERVER_URL + "/auth/realms/" + SAML_REAML,
      "sonar.auth.saml.loginUrl", SAML_SERVER_URL + "/auth/realms/" + SAML_REAML + "/protocol/saml",
      "sonar.auth.saml.certificate.secured",
      "MIICkzCCAXsCBgFk0cmrXTANBgkqhkiG9w0BAQsFADANMQswCQYDVQQDDAJRQTAeFw0xODA3MjUxNDExNTJaFw0yODA3MjUxNDEzMzJaMA0xCzAJBgNVBAMMAlFBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApLFvBUttI9vZcjGZ7jGOSfPvxws23rzpF1LnDRP0E5Faa0D7PrI0NgJp1ictI+3KzbuWY2hjoxjaNE3DbfZ7wwIUGMKNIiHKZoeYLlvLnkSiQwXCWNoxYMC1qzIj8wNfihXC8tlmaaSF6o7g2da2Hx3/3cCm5ISFJFmphoPiUIC7b9yTtDHJWXVXEPzq+D7zi5AMSoglaPqQMOVHqIIXwx816lSUKQ+vwMAVlKTAm2+e237nN7eT6CkhxfyarjLflN5oog+5V4B0g7AfVvCnWFio/F9qzKqEJfWe5L2Sz4v4bIiYZFxhRji5u+Ty0rwI+t6HSA8x03cWS1tWy2m2BQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQAmE3vxr4OVfgLGc3bah0pSyveEAIxT1sfWKgOWN24JYzhz6yhhsEsYE4u4VdK9a0284oKSY3wvElbmXvFqa+CUV2WTx4Z1aZ9IEmcFeYpJzz3pp78RbZ5leGWmHcOZ4bVoo3TPihas1psFu5KI1B7XpJsG2/w4USHpClFO7ts22IzUqKctFELOOROP4sqPmbvQ8FdH/TfmygPpo4t2KYuLKkIAGzWcRIldIqpJbdWHxKkrumlEaF9jIrmdolS0cG7LO8l83b4iDT3MAdjA01wH9plZolj/Ai39xfZb6kBcr3KFWNX58AkNdTcIPwa8sLjVgwEpjCg+UdS2Ihm/Qsgu",
      "sonar.auth.saml.user.login", "login",
      "sonar.auth.saml.user.name", "name",
      "sonar.auth.saml.user.email", "email",
      "sonar.auth.saml.group.name", "groups",
      "sonar.auth.saml.enabled", "true");
  }
}
