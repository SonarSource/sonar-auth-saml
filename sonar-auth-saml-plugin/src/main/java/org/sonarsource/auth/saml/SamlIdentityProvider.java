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
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
    return "SAML";
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
    try {
      Map<String, Object> samlData = new HashMap<>();
      samlData.put("onelogin.saml2.sp.entityid", "http://localhost:8080/saml-servlet-filter");
      samlData.put("onelogin.saml2.sp.assertion_consumer_service.url", new URL("http://localhost:8080/saml-servlet-filter"));
      samlData.put("onelogin.saml2.security.want_xml_validation", true);
      samlData.put("onelogin.saml2.sp.x509cert",
        "MIIB1DCCAT0CBgFJGP5dZDANBgkqhkiG9w0BAQsFADAwMS4wLAYDVQQDEyVodHRwOi8vbG9jYWxob3N0OjgwODAvc2FsZXMtcG9zdC1zaWcvMB4XDTE0MTAxNjEyNDQyM1oXDTI0MTAxNjEyNDYwM1owMDEuMCwGA1UEAxMlaHR0cDovL2xvY2FsaG9zdDo4MDgwL3NhbGVzLXBvc3Qtc2lnLzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA1RvGu8RjemSJA23nnMksoHA37MqY1DDTxOECY4rPAd9egr7GUNIXE0y1MokaR5R2crNpN8RIRwR8phQtQDjXL82c6W+NLQISxztarQJ7rdNJIYwHY0d5ri1XRpDP8zAuxubPYiMAVYcDkIcvlbBpwh/dRM5I2eElRK+eSiaMkCUCAwEAATANBgkqhkiG9w0BAQsFAAOBgQCLms6htnPaY69k1ntm9a5jgwSn/K61cdai8R8B0ccY7zvinn9AfRD7fiROQpFyY29wKn8WCLrJ86NBXfgFUGyR5nLNHVy3FghE36N2oHy53uichieMxffE6vhkKJ4P8ChfJMMOZlmCPsQPDvjoAghHt4mriFiQgRdPgIy/zDjSNw==");

      SettingsBuilder builder = new SettingsBuilder();
      Saml2Settings settings = builder.fromValues(samlData).build();

      Auth auth = new Auth(settings, context.getRequest(), context.getResponse());
      auth.login();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to init", e);
    }
  }

  @Override
  public void callback(CallbackContext context) {
    // TODO
  }
}
