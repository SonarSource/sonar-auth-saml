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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class SamlIdentityProviderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private SamlIdentityProvider underTest = new SamlIdentityProvider(null);

  @Test
  public void check_fields() {
    assertThat(underTest.getKey()).isEqualTo("saml");
    assertThat(underTest.getName()).isEqualTo("SAML");
    assertThat(underTest.getDisplay().getIconPath()).isEqualTo("/static/authgsaml/saml.svg");
    assertThat(underTest.getDisplay().getBackgroundColor()).isEqualTo("#444444");
  }

}
