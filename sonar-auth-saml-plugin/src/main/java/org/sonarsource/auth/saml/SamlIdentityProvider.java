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

import com.onelogin.saml2.Auth;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;

import static java.util.Objects.requireNonNull;

@ServerSide
public class SamlIdentityProvider implements OAuth2IdentityProvider {

  private static final String KEY = "saml";

  private final SamlSettings samlSettings;

  public SamlIdentityProvider(SamlSettings samlSettings) {
    this.samlSettings = samlSettings;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    // TODO get from setting
    return "SAML";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      // URL of src/main/resources/static/saml.svg at runtime
      // TODO find a SAML icon
      .setIconPath("/static/authsaml/saml.svg")
      // TODO find a SAML color
      .setBackgroundColor("#444444")
      .build();
  }

  @Override
  public boolean isEnabled() {
    return samlSettings.isEnabled();
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return samlSettings.allowUsersToSignUp();
  }

  @Override
  public void init(InitContext context) {
    try {
      Auth auth = newAuth(initSettings(context.getCallbackUrl()), context.getRequest(), context.getResponse());
      auth.login();
    } catch (IOException | SettingsException e) {
      throw new IllegalStateException("Fail to init", e);
    }
  }

  @Override
  public void callback(CallbackContext context) {
    Auth auth = newAuth(initSettings(null), context.getRequest(), context.getResponse());
    processResponse(auth);
    handleError(auth);

    String login = auth.getNameId();
    context.authenticate(UserIdentity.builder()
      .setLogin(login)
      .setProviderLogin(login)
      .setName(requireNonNull(getAttribute(auth, samlSettings.getUserName()), "Name is missing"))
      .setEmail(getAttribute(auth, samlSettings.getUserEmail()))
      .build());
    context.redirectToRequestedPage();
  }

  private static Auth newAuth(Saml2Settings saml2Settings, HttpServletRequest request, HttpServletResponse response) {
    try {
      return new Auth(saml2Settings, request, response);
    } catch (SettingsException e) {
      throw new IllegalStateException("Fail to create Auth", e);
    }
  }

  private static void processResponse(Auth auth) {
    try {
      auth.processResponse();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to process response", e);
    }
  }

  private void handleError(Auth auth) {
    if (!auth.isAuthenticated()) {
      throw new UnauthorizedException("Not authenticated");
    }
    List<String> errors = auth.getErrors();
    if (errors.isEmpty()) {
      return;
    }
    String errorMessage = errors.stream()
      .map(Object::toString)
      .collect(Collectors.joining(", "));
    if (!auth.isDebugActive()) {
      String errorReason = auth.getLastErrorReason();
      if (errorReason != null && !errorReason.isEmpty()) {
        errorMessage += errorReason;
      }
    }
    throw new UnauthorizedException(errorMessage);
  }

  @CheckForNull
  private static String getAttribute(Auth auth, String key) {
    Collection<String> attribute = auth.getAttribute(key);
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    return attribute.iterator().next();
  }

  private Saml2Settings initSettings(@Nullable String callbackUrl) {
    try {
      Map<String, Object> samlData = new HashMap<>();
      samlData.put("onelogin.saml2.idp.entityid", samlSettings.getProviderId());
      samlData.put("onelogin.saml2.idp.single_sign_on_service.url", samlSettings.getLoginUrl());
      samlData.put("onelogin.saml2.idp.x509cert", samlSettings.getCertificate());

      // TODO load url from Server
      samlData.put("onelogin.saml2.sp.entityid", "http://localhost:9000");
      samlData.put("onelogin.saml2.sp.assertion_consumer_service.url", callbackUrl != null ? new URL(callbackUrl) : "http://localhost");
      SettingsBuilder builder = new SettingsBuilder();
      return builder
        .fromValues(samlData)
        .build();
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Fail to initialize settings", e);
    }
  }
}
