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

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.server.ServerSide;

import static java.lang.String.valueOf;
import static org.sonar.api.PropertyType.BOOLEAN;

@ServerSide
public class SamlSettings {

  private static final String ENABLED = "sonar.auth.saml.enabled";
  private static final String PROVIDER_ID = "sonar.auth.saml.providerId";
  private static final String LOGIN_URL = "sonar.auth.saml.loginUrl";
  private static final String CERTIFICATE = "sonar.auth.saml.certificate.secured";

  private static final String USER_NAME_ATTRIBUTE = "sonar.auth.saml.user.name";
  private static final String USER_EMAIL_ATTRIBUTE = "sonar.auth.saml.user.email";

  private static final String ALLOW_USERS_TO_SIGN_UP = "sonar.auth.saml.allowUsersToSignUp";

  private static final String CATEGORY = "saml";
  private static final String SUBCATEGORY = "authentication";

  private final Configuration configuration;

  public SamlSettings(Configuration configuration) {
    this.configuration = configuration;
  }

  public String getProviderId() {
    return urlWithEndingSlash(configuration.get(PROVIDER_ID).orElseThrow(() -> new IllegalArgumentException("Provider ID is missing")));
  }

  public String getLoginUrl() {
    return urlWithEndingSlash(configuration.get(LOGIN_URL).orElseThrow(() -> new IllegalArgumentException("Login URL is missing")));
  }

  public String getCertificate() {
    return configuration.get(CERTIFICATE).orElseThrow(() -> new IllegalArgumentException("Certificate is missing"));
  }

  public String getUserName() {
    return configuration.get(USER_NAME_ATTRIBUTE).orElseThrow(() -> new IllegalArgumentException("User name attribute is missing"));
  }

  public String getUserEmail() {
    return configuration.get(USER_EMAIL_ATTRIBUTE).orElseThrow(() -> new IllegalArgumentException("User email attribute is missing"));
  }

  public boolean isEnabled() {
    return configuration.getBoolean(ENABLED).orElse(false) &&
      configuration.hasKey(LOGIN_URL) &&
      configuration.hasKey(CERTIFICATE) &&
      configuration.hasKey(USER_NAME_ATTRIBUTE) &&
      configuration.hasKey(USER_EMAIL_ATTRIBUTE);
  }

  public boolean allowUsersToSignUp() {
    return configuration.getBoolean(ALLOW_USERS_TO_SIGN_UP).orElse(false);
  }

  @CheckForNull
  private static String urlWithEndingSlash(@Nullable String url) {
    if (url != null && !url.endsWith("/")) {
      return url + "/";
    }
    return url;
  }

  public static List<PropertyDefinition> definitions() {
    int index = 1;
    return Arrays.asList(
      PropertyDefinition.builder(ENABLED)
        .name("Enabled")
        .description("Enable SAML users to login. Value is ignored if Provider ID, Login URL and certificate are not defined.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(index++)
        .build(),
      PropertyDefinition.builder(PROVIDER_ID)
        .name("Provider ID")
        .description("Identifier (URI) of the identity provider, the entity that provides SAML authentication..")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(LOGIN_URL)
        .name("SAML login url")
        .description("SAML login URL for the identity provider.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(CERTIFICATE)
        .name("Provider certificate")
        .description("X.509 certificate for the identity provider.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(USER_NAME_ATTRIBUTE)
        .name("SAML user name attribute")
        .description("Attribute defining the user name in SAML.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(USER_EMAIL_ATTRIBUTE)
        .name("SAML user email attribute")
        .description("Attribute defining the user email in SAML.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(ALLOW_USERS_TO_SIGN_UP)
        .name("Allow users to sign-up")
        .description("Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(true))
        .index(index++)
        .build());
  }
}
