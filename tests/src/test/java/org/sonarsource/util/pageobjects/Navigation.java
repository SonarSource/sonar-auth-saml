/*
 * SAML 2.0 Authentication for SonarQube
 * Copyright (C) 2018-2020 SonarSource SA
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
package org.sonarsource.util.pageobjects;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.sonar.orchestrator.Orchestrator;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Consumer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.html5.WebStorage;
import org.sonarsource.util.SelenideConfig;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static java.lang.String.format;

public class Navigation {

  public Navigation() {
    $("#content").shouldBe(exist);
  }

  public static Navigation create(Orchestrator orchestrator) {
    return create(orchestrator, "/");
  }

  public static Navigation create(Orchestrator orchestrator, String path) {
    WebDriver driver = SelenideConfig.configure(orchestrator);
    driver.manage().deleteAllCookies();
    clearStorage(d -> d.getLocalStorage().clear());
    clearStorage(d -> d.getSessionStorage().clear());
    clearBrowserLocalStorage();
    return Selenide.open(path, Navigation.class);
  }

  private static void clearStorage(Consumer<WebStorage> cleaner) {
    try {
      cleaner.accept((WebStorage) WebDriverRunner.getWebDriver());
    } catch (Exception e) {
      // ignore, it may occur when the first test opens browser. No pages are loaded
      // and local/session storages are not available yet.
      // Example with Chrome: "Failed to read the 'localStorage' property from 'Window': Storage is disabled inside 'data:' URLs."
    }
  }

  /**
   * Do not call {@link #clearStorage(Consumer)} for {@link Selenide#clearBrowserLocalStorage} as it's failing on Firefox 46
   */
  private static void clearBrowserLocalStorage() {
    try {
      Selenide.clearBrowserLocalStorage();
    } catch (Exception e) {
      // ignore, it may occur when the first test opens browser. No pages are loaded
      // and local/session storages are not available yet.
      // Example with Chrome: "Failed to read the 'localStorage' property from 'Window': Storage is disabled inside 'data:' URLs."
    }
  }

  public Navigation openHome() {
    return open("/", Navigation.class);
  }

  public LoginPage openLogin() {
    return open("/sessions/login", LoginPage.class);
  }

  public void open(String relativeUrl) {
    Selenide.open(relativeUrl);
  }

  public <P> P open(String relativeUrl, Class<P> pageObjectClassClass) {
    return Selenide.open(relativeUrl, pageObjectClassClass);
  }

  public Navigation shouldBeLoggedIn() {
    loggedInDropdown().should(visible);
    return this;
  }

  public Navigation shouldNotBeLoggedIn() {
    logInLink().should(visible);
    return this;
  }

  public LoginPage logIn() {
    logInLink().click();
    return Selenide.page(LoginPage.class);
  }

  public Navigation logOut() {
    SelenideElement dropdown = loggedInDropdown();
    // click must be on the <a> but not on the dropdown <li>
    // for compatibility with phantomjs
    dropdown.find(".dropdown-toggle").click();
    dropdown.find(By.linkText("Log out")).click();
    return this;
  }

  public SelenideElement getErrorMessage() {
    return $("#error");
  }

  private static SelenideElement logInLink() {
    return $(By.linkText("Log in"));
  }

  private static SelenideElement loggedInDropdown() {
    return $(".js-user-authenticated");
  }

  /**
   * Safe encoding for  URL parameters
   *
   * @param parameter the parameter to escape value
   * @return the escaped value of parameter
   */
  private static String escape(String parameter) {
    try {
      return URLEncoder.encode(parameter, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(format("Unable to escape [%s]", parameter));
    }
  }

  public LoginPage shouldBeRedirectedToLogin() {
    $("#login_form").should(visible);
    return Selenide.page(LoginPage.class);
  }

}
