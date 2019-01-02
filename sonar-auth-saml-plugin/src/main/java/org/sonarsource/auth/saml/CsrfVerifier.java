/*
 * SAML 2.0 Authentication for SonarQube
 * Copyright (C) 2018-2019 SonarSource SA
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

import java.math.BigInteger;
import java.security.SecureRandom;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.UnauthorizedException;

import static java.lang.String.format;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.sonarsource.auth.saml.Cookies.findCookie;
import static org.sonarsource.auth.saml.Cookies.newCookieBuilder;

/**
 * This class is a copy of org.sonar.server.authentication.OAuthCsrfVerifier from SonarQube codebase
 *
 * It's still not possible to use the OAuthCsrfVerifier as SAML is using a different response parameter : 'RelayState', instead of 'state'.
 * TODO When using SonarQube 7.3 (https://jira.sonarsource.com/browse/SONAR-11072), this class should be removed
 */
@ServerSide
public class CsrfVerifier {

  private static final String CSRF_STATE_COOKIE = "OAUTHSTATE";

  String generateState(HttpServletRequest request, HttpServletResponse response) {
    // Create a state token to prevent request forgery.
    // Store it in the session for later validation.
    String state = new BigInteger(130, new SecureRandom()).toString(32);
    response.addCookie(newCookieBuilder(request).setName(CSRF_STATE_COOKIE).setValue(sha256Hex(state)).setHttpOnly(true).setExpiry(-1).build());
    return state;
  }

  void verifyState(HttpServletRequest request, HttpServletResponse response) {
    Cookie cookie = findCookie(CSRF_STATE_COOKIE, request)
      .orElseThrow(() -> new UnauthorizedException(format("Cookie '%s' is missing", CSRF_STATE_COOKIE)));
    String hashInCookie = cookie.getValue();

    // remove cookie
    response.addCookie(newCookieBuilder(request).setName(CSRF_STATE_COOKIE).setValue(null).setHttpOnly(true).setExpiry(0).build());

    String stateInRequest = request.getParameter("RelayState");
    if ((stateInRequest == null || stateInRequest.isEmpty()) || !sha256Hex(stateInRequest).equals(hashInCookie)) {
      throw new UnauthorizedException("CSRF state value is invalid");
    }
  }

}
