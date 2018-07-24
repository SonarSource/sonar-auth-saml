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

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.setting.ResetRequest;
import org.sonarqube.ws.client.setting.SetRequest;
import org.sonarsource.util.pageobjects.Navigation;

public class ItUtils {

  public final static String SAML_SERVER_URL = "http://keycloak-1.sonarsource.com:8080";
  public final static String SAML_REAML = "QA";
  public final static String SAML_CLIENT_ID = "sonarqube";

  /**
   * Open a new browser session. Cookies are deleted.
   */
  public static Navigation openBrowser(Orchestrator orchestrator) {
    return Navigation.create(orchestrator, "/");
  }

  public static void setGlobalSetting(Orchestrator orchestrator, String... properties) {
    for (int i = 0; i < properties.length; i += 2) {
      newAdminWsClient(orchestrator).settings().set(SetRequest.builder().setKey(properties[i]).setValue(properties[i + 1]).build());
    }
  }

  public static void resetGlobalSettings(Orchestrator orchestrator, String... keyValues) {
    newAdminWsClient(orchestrator).settings().reset(ResetRequest.builder().setKeys(keyValues).build());
  }

  public static WsClient newAdminWsClient(Orchestrator orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .credentials("admin", "admin")
      .url(orchestrator.getServer().getUrl())
      .build());
  }
}
