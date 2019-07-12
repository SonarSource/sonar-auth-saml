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
package org.sonarsource.tests;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  SamlTest.class
})
public class SamlTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    String defaultRuntimeVersion = "true".equals(System.getenv("SONARSOURCE_QA")) ? null : "7.9";
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", defaultRuntimeVersion));

    String pluginVersion = System.getProperty("samlVersion");
    Location pluginLocation;
    if (StringUtils.isEmpty(pluginVersion) || pluginVersion.endsWith("-SNAPSHOT")) {
      pluginLocation = FileLocation.byWildcardMavenFilename(new File("../sonar-auth-saml-plugin/build/libs"), "sonar-auth-saml-plugin-*-all.jar");
    } else {
      pluginLocation = MavenLocation.of("org.sonarsource.auth.saml", "sonar-auth-saml-plugin", pluginVersion);
    }
    builder.addPlugin(pluginLocation);

    ORCHESTRATOR = builder.build();
  }

}
