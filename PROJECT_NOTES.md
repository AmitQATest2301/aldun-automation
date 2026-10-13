# Aldun Automation - Project Record

Running record of suites, classes, config keys and verified application facts (last updated 2026-10-13).

## Environment
Edge 155; Temurin JDK 25.0.4.1 (code compiled for Java 17); Maven 3.9.16 at C:\Users\Qaami\tools\apache-maven-3.9.16;
Git 2.55.0; Eclipse 2026-09. Selenium 4.51.0 (4.27 used Microsoft's retired driver URL), TestNG 7.12.0.

## How to run
| Command | What runs |
|---|---|
| `mvn clean test` | **Default: testng-20-process.xml** - the positive flow as 20 test cases in one browser |
| `-DsuiteXmlFile=src/test/resources/testng-30-cases.xml` | 30 independent cases, one browser, logged out between tests |
| `-DsuiteXmlFile=src/test/resources/testng.xml` | Full regression, 48 cases in 7 areas |
| `-DsuiteXmlFile=src/test/resources/testng-login.xml` | Daily workflow, 10 steps (saved best run: 10/10 pass) |
| `-DsuiteXmlFile=src/test/resources/testng-create-interaction.xml` | Create Interaction flow, 7 steps |
| `-DsuiteXmlFile=src/test/resources/testng-ui.xml` | Login-screen checks only - never logs in |
| `run-aldun-tests.bat` / `schedule-daily.bat` | Double-click run / daily Windows schedule (09:05) |

Credentials: `config.local.properties` (git-ignored) or env vars `ALDUN_EMAIL` / `ALDUN_PASSWORD`.

## Report
`reports/TestSummary_<time>.html` opens automatically when the run ends. Columns:
**Test Case ID | Type | Test Steps | Expected Result | Actual Result** (Passed / Failed: reason / Skipped: reason),
sorted by ID, plus failure details with screenshots. Detailed Extent report linked from it.
Each test describes itself with `@TestCase(id, type, steps, expected)`; `RunInIdOrder` runs tests in ID order.

## Test classes
| Class | Browser | Cases |
|---|---|---|
| `tests.EndToEndProcessTest` | one session | ALD001-ALD020 positive flow (log in, check in, new interaction, create, Today, Interactions, check out, log out) |
| `tests.cases.*` (LoginScreenCases, LoginCases, InteractionCases, NavigationCases) | one browser for all (`IndependentTest`) | ALD001-ALD030 |
| `tests.DailyWorkflowTest` | one session | 10-step daily workflow |
| `tests.CreateInteractionFlowTest` | one session (`SessionTest`) | 7-step create flow |
| `tests.LoginPageTest`, `AccessControlTest`, `LoginAttemptTest`, `InteractionValidationTest`, `InteractionSearchTest`, `MenuAndLogoutTest` | one session per class | full regression |

## Page objects (src/main/java/pages)
BasePage (type with verify/retry, typeRaw, openPath, confirmIfAsked = "Create anyway"), LoginPage, DashboardPage,
InteractionPage, CreateInteractionFormPage, InteractionsListPage, NavigationMenu (menu items, Log out with confirm/cancel).

## Verified application facts (from runs and the user's screenshots, 2026-10-13)
- Login: `[data-testid='login-email-input']`, `[data-testid='login-password-input']`, `button[aria-label='Log in']`.
  Log in disabled when both blank or password blank; **ENABLED when email blank (likely defect - ALD007 in 30 cases)**.
- Home `/staff`: Today, Hello, Test, status Not checked in / Checked in, Your shift 09:00-18:00, Hospitals,
  Check in / Check out button, Log Interaction.
- Check in: `/staff/check-in` "Checking in..." then Interactions list `/staff/my-work`.
- Check out: "Checked out" popup, then the app may move to the Interactions list.
- Interactions list: search "Phone, Adm ID, UHID or Name", "Created by me", cards, New interaction button.
- New Interaction `/staff/my-work/interaction/new?hospitalId=YUKUTRCY`: Attender Phone No *; after a number:
  "Aldun history for this number" (New / Known), Admissions & Interactions counts, Create Interaction button.
- Create New Interaction `/interaction/edit/?hospitalId=...&phoneNo=...`: note "For Onboarding Now, either Admission Id
  or UHID is required."; Admission Id, UHID, Attender Phone No *, Patient Name *, Admission Date, Source *, Status *,
  ClaimIt Yes/No; Create disabled until required fields filled.
- Duplicate: "Duplicate interaction found" dialog with Cancel / **Create anyway**.
- After save: Patient profile `/interaction/<id>/profile` with phone, name, UHID.
- Menu (three lines): Test User / Hops staff; Today, Interactions, Admissions, Master Search, Performance,
  Leaderboard, Reports, Report Issue; Log out -> "Log out?" dialog with Cancel / Log out -> login screen.

## Test data (tests/TestData.java)
Phone 9811465840, Admission Id AD556142, UHID UH558440, Patient Rahul Sharma; negative data for short/letters/11-digit
phone, unknown/malformed email, wrong password (only one wrong attempt per run to avoid lock-out).
