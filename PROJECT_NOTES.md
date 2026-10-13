# Aldun Automation - Project Record

Running record of packages, classes, config keys and verified application facts.
Update this file whenever a phase adds or changes something.

## Phase status
| Phase | Status |
|---|---|
| 1. Environment check | Done (2026-10-13). Edge 155; Temurin JDK 25.0.4.1 (JAVA_HOME set, code targets Java 17 via `<release>17</release>`); Maven 3.9.16 at C:\Users\Qaami\tools\apache-maven-3.9.16 (MAVEN_HOME + user PATH); Git 2.55.0; Eclipse 2026-09 at C:\Users\Qaami\eclipse\java-2026-09\eclipse |
| 2. Maven project + pom.xml | Done. `mvn clean compile` succeeded (all dependencies resolved) |
| 3-5, 7, 9 (login module) | Done 2026-10-13. Selenium upgraded 4.27.0 -> 4.51.0 (old one used retired msedgedriver.azureedge.net), TestNG 7.10.2 -> 7.12.0 (removes SLF4J 1.x warning). UI suite: 9/9 PASS in Edge 155. Auth suite written, NOT yet run |
| 6. ExcelUtils + TestData.xlsx | Pending (DataProvider done) |
| 8. Logout, Attendance, Interaction, Admission | Pending - need page details |
| 10. Docs / interview notes | Pending |

## Classes and public methods
- `base.DriverFactory`: `initDriver()`, `getDriver()`, `quitDriver()` - browsers: edge, chrome
- `utils.ConfigReader`: `get`, `getInt`, `getBoolean`, `getSecret(envName)`
- `utils.WaitUtils(driver[, seconds])`: `waitForVisible`, `waitForClickable`, `waitForUrlNotContaining`, `waitForAttribute`, `waitForNonEmptyText`
- `utils.ScreenshotUtil.capture(driver, name)` -> absolute path; `utils.DateUtils.fileTimestamp()`
- `pages.BasePage` (abstract): `click`, `type` (Ctrl+A/Delete clear for React), `isDisplayed`, `getCurrentUrl`
- `pages.LoginPage`: `enterEmail`, `enterPassword`, `clickLogin`, `login`, `showPassword`, `hidePassword`, `is*Displayed`, `isLoginButtonEnabled`, `getPasswordFieldType`, `getPasswordFieldValue`, `isOnLoginPage`, `getCurrentToastText`, `waitForErrorMessage`
- `pages.DashboardPage`: `isLoaded` (only checks URL left /login - real locators TBD)
- `base.BaseTest` (test): fresh driver per @Test, exposes `driver`, `loginPage`
- `tests.TestData`: DataProviders `loginButtonDisabled`, `invalidLogin`; `resolve(token)`
- `listeners.TestListener`: Extent report, logs, failure screenshots

## Config keys (config.properties)
baseUrl, loginPath, browser, headless, explicitWait, pageLoadTimeout, screenshotDir, reportDir

## Suites
- `testng-ui.xml` - group "ui" only, never submits the form
- `testng.xml` - everything, including group "auth" (submits real logins)

## Packages
| Source root | Package | Classes |
|---|---|---|
| src/main/java | base | DriverFactory |
| src/main/java | pages | LoginPage, DashboardPage, AttendancePage, InteractionPage, AdmissionPage |
| src/main/java | utils | ConfigReader, WaitUtils, ScreenshotUtil, ExcelUtils, DateUtils |
| src/test/java | base | BaseTest |
| src/test/java | tests | LoginTest, LogoutTest, AttendanceTest, InteractionTest, AdmissionTest, TestData |
| src/test/java | listeners | TestListener |

## Login page - verified facts (inspected 2026-10-13, without signing in)
URL: https://team.aldun.com/login (title "Log in · Aldun Team"). Expo / React Native Web app, no HTML <form>.

| Element | Locator | Notes |
|---|---|---|
| Email input | `[data-testid='login-email-input']` | type=email, placeholder "you@aldun.com" |
| Password input | `[data-testid='login-password-input']` | type=password, placeholder "••••••••" |
| Show/hide password | `button[aria-label='Show password']` / `button[aria-label='Hide password']` | Toggles input type password <-> text; aria-label flips |
| Log in button | `button[aria-label='Log in']` | **Disabled (disabled + aria-disabled="true") when fields are blank** |
| Forgot password | button with text "Forgot password?" | no test id |
| SSO | `[data-testid='login-sso-btn']` | |
| Report issue | `[data-testid='login-report-issue-button']` | |
| Toast container | `[data-testid='toastAnimatedContainer']` | Likely where error messages appear - UNVERIFIED |

Button state (verified 2026-10-13): both blank -> disabled; password blank -> disabled;
**email blank + password filled -> ENABLED (possible bug)**; malformed email "abc" -> enabled, no inline validation.

## Still unknown
- Error message text/location for invalid email and/or wrong password (tests assume the toast area)
- URL and a stable element after successful login (dashboard)
- Logout location and what the app shows after logout
- Pages/locators for Attendance, Interactions, Admissions

## Secrets
Credentials come from environment variables `ALDUN_EMAIL` / `ALDUN_PASSWORD` - never in source or config.properties.
