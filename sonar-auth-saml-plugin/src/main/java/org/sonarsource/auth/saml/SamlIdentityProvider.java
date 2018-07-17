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
package org.sonarsource.auth.saml;

import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@ServerSide
public class SamlIdentityProvider implements OAuth2IdentityProvider {

  static final String KEY = "saml";

  private static final Logger LOGGER = Loggers.get(SamlIdentityProvider.class);

  public SamlIdentityProvider() {
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return "GitHub";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      // URL of src/main/resources/static/saml.svg at runtime
      .setIconPath("/static/authsaml/saml.svg")
      .setBackgroundColor("#444444")
      .build();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return true;
  }

  @Override
  public void init(InitContext context) {
    // TODO
  }

  @Override
  public void callback(CallbackContext context) {
    // TODO
  }
}
