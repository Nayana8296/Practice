package com.example;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class EdgeMain {

    private static final String APP_URL = "https://st1-lpp.oss.att.com/";
    private static final String LOGIN_HOST = "login.microsoftonline.com";

    private static final String USER_EMAIL =
            System.getenv().getOrDefault("ST1_USER", "nk5730@atttest.com");

    // Never hard-code the password: this file is in source control, and a credential
    // committed once stays in the history even after it is deleted. Supply it for the
    // session that runs the automation:
    //     $env:ST1_PASSWORD = "<your password>"
    // Normally it is not needed at all - the dedicated Edge profile below keeps the
    // signed-in session, so sign-in only happens if that session has expired.
    private static final String USER_PASSWORD = System.getenv("ST1_PASSWORD");

    // Dedicated Edge profile for automation. Sign in (incl. MFA) once by hand and the
    // session is reused on every later run, so no login step is needed after that.
    // Must NOT be your everyday Edge profile - Edge locks a profile that is in use.
    private static final Path PROFILE_DIR = Path.of(
            System.getProperty("user.home"), "selenium-edge-profile");

    private static final Path EDGE_DRIVER = Path.of(
            System.getProperty("user.home"),
            ".cache", "selenium", "msedgedriver", "win64", "150.0.4078.144", "msedgedriver.exe");

    // The run is driven by a workbook - one row per cabinet + equipment to create. The
    // path is never hard-coded into the flow: see resolveDataFile for the lookup order,
    // so the sheet can live anywhere on your drive and be swapped without touching code.
    private static final String DATA_PROPERTY = "ams.data";
    private static final String DATA_ENV = "AMS_DATA_FILE";
    private static final String DRY_RUN_PROPERTY = "ams.dryRun";
    // The sheet that is actually maintained. Kept outside the project so it survives a
    // clean/rebuild, and used even when AMS_DATA_FILE is not visible to the shell - a
    // VS Code terminal inherits the environment VS Code started with, so a setx made
    // afterwards is not seen until VS Code is restarted.
    private static final Path DEFAULT_DATA_FILE = Path.of(
            System.getProperty("user.home"), "Documents", "AMS", "inventory-data.xlsx");
    private static final String DATA_SHEET = "Inventory";

    public static void main(String[] args) {
        quietenPoiLogging();

        Path dataFile = resolveDataFile(args);

        // First run on a new machine: leave a filled-in example behind instead of failing.
        if (!Files.exists(dataFile)) {
            ExcelData.writeTemplate(dataFile, DATA_SHEET);
            System.out.println("No workbook at " + dataFile.toAbsolutePath());
            System.out.println("A template with one example row was written there.");
            System.out.println("Fill it in and run again.");
            return;
        }

        List<ExcelData.DataRow> rows = ExcelData.read(dataFile, DATA_SHEET);
        if (rows.isEmpty()) {
            System.out.println("No data rows in " + dataFile.toAbsolutePath());
            return;
        }
        System.out.println("Loaded " + rows.size() + " row(s) from " + dataFile.toAbsolutePath());

        // Lets you check the sheet is the one you think it is, and that every column reads
        // back correctly, without opening a browser or touching AMS.
        if (isDryRun(args)) {
            printRows(rows);
            return;
        }

        System.setProperty("webdriver.edge.driver", EDGE_DRIVER.toString());

        EdgeOptions options = new EdgeOptions();
        options.addArguments("--user-data-dir=" + PROFILE_DIR);
        options.addArguments("--profile-directory=Default");

        WebDriver driver = new EdgeDriver(options);
        Scanner scanner = new Scanner(System.in);

        try {
            driver.manage().window().maximize();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            driver.get(APP_URL);

            WebElement inventory = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("ams_inv")));
            inventory.click();

            switchToNewWindowIfAny(driver);
            signInIfNeeded(driver, wait, scanner);

            System.out.println("Logged in. Current URL: " + driver.getCurrentUrl());

            // One spreadsheet row = one full cabinet + equipment cycle. A row that blows
            // up is recorded and the run carries on, so a single bad value does not cost
            // the whole sheet.
            Path cabinetFile = cabinetRegistryFile(dataFile);
            Properties cabinets = loadCabinets(cabinetFile);

            List<String> failures = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                ExcelData.DataRow row = rows.get(i);
                System.out.println();
                System.out.println("===== Excel row " + row.excelRow()
                        + "  (" + (i + 1) + " of " + rows.size() + ") =====");

                if (i > 0) {
                    returnHome(driver);
                }

                try {
                    createInventory(driver, row, cabinets, cabinetFile);
                } catch (RuntimeException e) {
                    String reason = e.getClass().getSimpleName() + " - " + firstLine(e.getMessage());
                    failures.add("Excel row " + row.excelRow() + ": " + reason);
                    System.out.println("Excel row " + row.excelRow() + " FAILED: " + reason);
                }
            }

            System.out.println();
            System.out.println("===== Summary =====");
            System.out.println("Rows processed: " + rows.size()
                    + ", succeeded: " + (rows.size() - failures.size())
                    + ", failed: " + failures.size());
            for (String failure : failures) {
                System.out.println("  " + failure);
            }

            System.out.println("Browser is open. Press Enter in this terminal to close it.");
            scanner.nextLine();
        } catch (RuntimeException e) {
            // Leave the browser up so the failing page can be inspected.
            System.out.println();
            System.out.println("FAILED: " + e.getClass().getSimpleName() + " - "
                    + firstLine(e.getMessage()));
            System.out.println("Browser left open. Press Enter in this terminal to close it.");
            scanner.nextLine();
            throw e;
        } finally {
            driver.quit();
            scanner.close();
        }
    }

    /**
     * Puts the equipment from one spreadsheet row into a cabinet, creating that cabinet
     * first only if the row's Complex has not had one yet. Every value on the screens
     * comes from the sheet - the column headings are the same text AMS prints next to
     * each field, so a heading doubles as the locator.
     *
     * <p>One cabinet per Complex is the rule: a cabinet is built the first time a Complex
     * is seen, its AMS-generated name is remembered in {@code cabinetFile}, and every
     * later row carrying that same Complex - in this run or any run afterwards - is filed
     * into it. Changing the Complex in the sheet is what asks for a new cabinet.
     */
    private static void createInventory(WebDriver driver, ExcelData.DataRow row,
            Properties cabinets, Path cabinetFile) {
        String complex = row.get(ExcelData.COMPLEX);

        // A name typed into the sheet wins: it is how an existing AMS cabinet is adopted
        // without creating anything at all.
        String cabinetName = row.getOrDefault(
                ExcelData.CABINET_NAME, cabinets.getProperty(complex, ""));

        if (cabinetName.isEmpty()) {
            cabinetName = createCabinet(driver, row);
            cabinets.setProperty(complex, cabinetName);
            rememberCabinets(cabinetFile, cabinets);
        } else {
            System.out.println("Complex " + complex + " already has cabinet " + cabinetName
                    + " - reusing it, no new cabinet created.");
        }

        openInventoryEditor(driver, "Equipment");

        // "New Inventory - Add Equipment Parameters" - the cabinet just created.
        fillEquipmentParameters(driver, row, cabinetName);

        // Picking the radio reloads the page and can come back with the boxes blank.
        clickRadioByLabel(driver, "Equipment Using Template");
        if (isFieldEmpty(driver, ExcelData.COMPLEX)) {
            System.out.println("The reload cleared the cabinet details - typing them again.");
            fillEquipmentParameters(driver, row, cabinetName);
        }

        // "NEW Inventory Definition" - all three are dropdowns, and each one fills the
        // next, so they have to be taken in this order.
        fillField(driver, ExcelData.EQUIP_TYPE, row.get(ExcelData.EQUIP_TYPE));
        fillField(driver, ExcelData.EQUIP_MODEL, row.get(ExcelData.EQUIP_MODEL));
        fillField(driver, ExcelData.TEMPLATE_NAME, row.get(ExcelData.TEMPLATE_NAME));

        clickButton(driver, "Search");

        // "Add Inventory - Add Equipment Search Results". The equipment row is a grid,
        // so each box is found by its column heading rather than by a label.
        fillGridField(driver, ExcelData.SHELF, row.get(ExcelData.SHELF));
        fillGridField(driver, ExcelData.LOGICAL_NODE, row.get(ExcelData.LOGICAL_NODE));
        fillGridField(driver, ExcelData.CRITICAL_SERVICE, row.get(ExcelData.CRITICAL_SERVICE));
        fillGridField(driver, ExcelData.FIRSTNET_INDICATOR, row.get(ExcelData.FIRSTNET_INDICATOR));
        fillGridField(driver, ExcelData.EQUIP_CLLI, row.get(ExcelData.EQUIP_CLLI));
        fillGridField(driver, ExcelData.LOCAL_CLLI, row.get(ExcelData.LOCAL_CLLI));
        fillGridField(driver, ExcelData.ASSOC_CLLI, row.get(ExcelData.ASSOC_CLLI));
        fillGridField(driver, ExcelData.VOAVPN, row.get(ExcelData.VOAVPN));

        // The rows are already ticked, so the left-hand nav "Save" link commits them.
        Set<String> windowsBeforeSave = driver.getWindowHandles();
        clickInAnyFrame(driver, By.xpath(
                "//a[normalize-space(.)='Save']"
                + " | //input[@type='submit' or @type='button']"
                + "[translate(@value,'SAVE','save')='save']"), Duration.ofSeconds(30));
        System.out.println("Equipment saved.");

        // Save raises the "Inv Options Details Window" popup.
        switchToWindowShowing(driver, "Inv Options Details", windowsBeforeSave,
                Duration.ofSeconds(30));
        String optionsPopup = driver.getWindowHandle();

        // "VAN 4.0" and "VAN 4.0/ICE" both start the same, so the exact option wins.
        fillField(driver, ExcelData.VAN_RELEASE, row.get(ExcelData.VAN_RELEASE));

        // OK closes the popup, so click it straight and hop back to the main window
        // rather than waiting on a document that is about to disappear. The click is
        // retried on a poll in case the option list is still drawn over the button,
        // and the value is normalised because AMS pads some buttons (" OK ").
        clickWhenReady(driver, By.xpath(
                "//input[@type='submit' or @type='button']"
                + "[normalize-space(translate(@value,'OK','ok'))='ok']"
                + " | //button[normalize-space(.)='OK']"), Duration.ofSeconds(30));
        leaveClosedWindow(driver, optionsPopup, Duration.ofSeconds(30));
        waitForFormToSettle(driver);
        System.out.println("Inventory options confirmed for Excel row " + row.excelRow() + ".");
    }

    /**
     * Builds one cabinet from the row's cabinet columns and hands back the name AMS
     * generated for it, leaving the browser on the AMS home frame.
     */
    private static String createCabinet(WebDriver driver, ExcelData.DataRow row) {
        // AMS is a frameset, so the menu link has to be found inside its frames.
        openInventoryEditor(driver, "Cabinet");

        // Cabinet details form.
        fillField(driver, ExcelData.COMPLEX, row.get(ExcelData.COMPLEX));
        fillField(driver, ExcelData.SERVICE, row.get(ExcelData.SERVICE));
        fillField(driver, ExcelData.CABINET_TYPE, row.get(ExcelData.CABINET_TYPE));
        fillField(driver, ExcelData.CABINET_MODEL, row.get(ExcelData.CABINET_MODEL));
        fillField(driver, ExcelData.SERVICE_TYPE, row.get(ExcelData.SERVICE_TYPE));

        clickContinue(driver);

        // "Add New Cabinets Search Results". Columns are: [checkbox] Cabinet Name,
        // Cabinet CLLI, FIC, Serial No. The name is generated by AMS and printed as
        // plain text - only the CLLI/FIC/Serial cells are inputs, so read the name
        // rather than typing over the row.
        String cabinetName = findInAnyFrame(driver,
                By.xpath("//tr[.//input[@type='checkbox']]/td[2]"),
                Duration.ofSeconds(30)).getText().trim();
        System.out.println("Cabinet created: " + cabinetName);

        // The new cabinet row is already ticked, so the left-hand nav "Save" link
        // commits it.
        clickInAnyFrame(driver, By.xpath(
                "//a[normalize-space(.)='Save']"
                + " | //input[@type='submit' or @type='button']"
                + "[translate(@value,'SAVE','save')='save']"), Duration.ofSeconds(30));
        waitForFormToSettle(driver);
        System.out.println("Cabinet saved.");

        // Back to the AMS home frame, ready for the Equipment editor.
        returnHome(driver);
        return cabinetName;
    }

    /**
     * Where the Complex -> cabinet names are kept: beside the workbook, so a sheet that
     * is copied to another machine can bring its cabinets along.
     */
    private static Path cabinetRegistryFile(Path dataFile) {
        Path folder = dataFile.toAbsolutePath().getParent();
        return folder == null ? Path.of("cabinets.properties") : folder.resolve("cabinets.properties");
    }

    /** The cabinets built by earlier runs; empty on the very first run. */
    private static Properties loadCabinets(Path file) {
        Properties cabinets = new Properties();
        if (!Files.exists(file)) {
            return cabinets;
        }
        try (var in = Files.newInputStream(file)) {
            cabinets.load(in);
            System.out.println("Cabinets already built: " + cabinets);
        } catch (IOException e) {
            // A damaged registry must not stop a run - the worst case is one extra cabinet.
            System.out.println("Could not read " + file + " (" + e.getMessage()
                    + ") - carrying on as if no cabinets had been built yet.");
        }
        return cabinets;
    }

    /** Saves the registry straight away, so a later failure cannot lose the new name. */
    private static void rememberCabinets(Path file, Properties cabinets) {
        try (var out = Files.newOutputStream(file)) {
            cabinets.store(out, "Cabinet built in AMS for each Complex."
                    + " Delete a line to have the next run build a fresh cabinet.");
            System.out.println("Remembered in " + file);
        } catch (IOException e) {
            System.out.println("Could not write " + file + " (" + e.getMessage()
                    + ") - the next run will build another cabinet unless you put this"
                    + " name in the '" + ExcelData.CABINET_NAME + "' column.");
        }
    }

    /**
     * Works out which workbook to read. Nothing in the code has to change to point at a
     * different sheet - the first of these that is set wins:
     * <ol>
     *   <li>the first command line argument</li>
     *   <li>-Dams.data=&lt;path&gt;</li>
     *   <li>the AMS_DATA_FILE environment variable</li>
     *   <li>data\inventory-data.xlsx inside the project</li>
     * </ol>
     * A folder can be given instead of a file, in which case the newest .xlsx inside it
     * is taken - handy when the sheet sits in a OneDrive/shared drive folder and gets a
     * new name every time it is reissued.
     */
    private static Path resolveDataFile(String[] args) {
        String configured;
        String source;

        String pathArg = firstNonSwitch(args);
        if (pathArg != null) {
            configured = pathArg;
            source = "command line";
        } else if (!isBlank(System.getProperty(DATA_PROPERTY))) {
            configured = System.getProperty(DATA_PROPERTY);
            source = "-D" + DATA_PROPERTY;
        } else if (!isBlank(System.getenv(DATA_ENV))) {
            configured = System.getenv(DATA_ENV);
            source = DATA_ENV;
        } else {
            configured = DEFAULT_DATA_FILE.toString();
            source = "default";
        }

        // Paths pasted from Explorer often arrive wrapped in quotes.
        Path path = Path.of(configured.trim().replace("\"", "")).toAbsolutePath().normalize();
        if (Files.isDirectory(path)) {
            path = newestWorkbookIn(path);
        }

        System.out.println("Data file (" + source + "): " + path);
        return path;
    }

    /** Newest .xlsx in a folder, skipping the "~$name.xlsx" lock file Excel leaves open. */
    private static Path newestWorkbookIn(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xlsx"))
                    .filter(p -> !p.getFileName().toString().startsWith("~$"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .orElseThrow(() -> new IllegalStateException(
                            "No .xlsx workbook in " + folder));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list " + folder, e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** First argument that is a path rather than a switch such as --dry-run. */
    private static String firstNonSwitch(String[] args) {
        for (String arg : args) {
            if (!isBlank(arg) && !arg.startsWith("-")) {
                return arg;
            }
        }
        return null;
    }

    private static boolean isDryRun(String[] args) {
        if (System.getProperty(DRY_RUN_PROPERTY) != null) {
            return true;
        }
        for (String arg : args) {
            if ("--dry-run".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    /** Echoes what was read back out of the sheet, so a typo shows up before AMS does. */
    private static void printRows(List<ExcelData.DataRow> rows) {
        System.out.println("Dry run - nothing will be created in AMS.");
        for (ExcelData.DataRow row : rows) {
            System.out.println();
            System.out.println("Excel row " + row.excelRow() + ":");
            for (String column : ExcelData.COLUMNS) {
                System.out.println("  " + column + " = " + row.getOrDefault(column, "(blank)"));
            }
        }
    }

    /**
     * POI logs through the Log4j2 API. With no logging provider on the classpath it
     * greets every run with a one-line "could not find a logging provider" ERROR that
     * looks like a failure but is not - point it at the simple logger that ships inside
     * log4j-api instead.
     */
    private static void quietenPoiLogging() {
        System.setProperty("log4j2.loggerContextFactory",
                "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
        System.setProperty("org.apache.logging.log4j.simplelog.level", "warn");
    }

    /** The four boxes on "Add Equipment Parameters" - typed twice when a reload wipes them. */
    private static void fillEquipmentParameters(WebDriver driver, ExcelData.DataRow row,
            String cabinetName) {
        fillField(driver, ExcelData.COMPLEX, row.get(ExcelData.COMPLEX));
        fillField(driver, ExcelData.SERVICE, row.get(ExcelData.SERVICE));
        fillField(driver, "Cabinet Name", cabinetName);
        fillField(driver, ExcelData.SERVICE_TYPE, row.get(ExcelData.SERVICE_TYPE));
    }

    /**
     * Clicks the AMS "Home" link so the next screen starts from a known place. Best
     * effort: a row that failed half way may have left a window where Home is missing,
     * and that must not stop the rows still to come.
     */
    private static void returnHome(WebDriver driver) {
        try {
            clickInAnyFrame(driver, By.xpath("//a[normalize-space(.)='Home']"),
                    Duration.ofSeconds(30));
            waitForFormToSettle(driver);
        } catch (RuntimeException e) {
            System.out.println("Could not click Home (" + e.getClass().getSimpleName()
                    + ") - carrying on from the current screen.");
        }
    }

    /** The sign-in page may open in a new window/tab - switch to it if one appeared. */    private static void switchToNewWindowIfAny(WebDriver driver) {
        String currentWindow = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(currentWindow)) {
                driver.switchTo().window(handle);
                return;
            }
        }
    }

    /**
     * Fills the Microsoft login form only when it is actually shown. MFA (Authenticator
     * approval, PIN, smart card, ...) cannot be automated, so the run pauses until you
     * finish that step in the browser and press Enter.
     */
    private static void signInIfNeeded(WebDriver driver, WebDriverWait wait, Scanner scanner) {
        if (!onLoginPage(driver)) {
            System.out.println("Existing session reused - no sign-in required.");
            return;
        }

        // A remembered profile may show an account picker instead of the email box.
        clickIfPresent(driver, By.cssSelector("div[data-test-id='" + USER_EMAIL + "']"));

        // ...and it may skip the email page entirely, landing straight on "Enter password".
        if (isPresent(driver, By.name("loginfmt"))) {
            WebElement emailInput = wait.until(
                    ExpectedConditions.elementToBeClickable(By.name("loginfmt")));
            emailInput.clear();
            emailInput.sendKeys(USER_EMAIL);
            wait.until(ExpectedConditions.elementToBeClickable(By.id("idSIButton9"))).click();
            System.out.println("Entered email: " + USER_EMAIL);
        } else {
            System.out.println("Email page skipped - account already remembered.");
        }

        try {
            typePassword(driver);
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(ExpectedConditions.elementToBeClickable(By.id("idSIButton9")))
                    .click();
            System.out.println("Password submitted.");
        } catch (RuntimeException e) {
            System.out.println("Password step failed: " + e.getClass().getSimpleName()
                    + " - " + firstLine(e.getMessage()));
            System.out.println("Sign in manually in the browser window.");
        }

        // "Let's keep your account secure" / "Stay signed in?" may appear before MFA.
        dismissInterruptPages(driver, 20);

        if (!onLoginPage(driver)) {
            System.out.println("Login completed without a manual step.");
            return;
        }

        // "Verify your identity" - pick the Authenticator push, then wait for the tap.
        chooseAuthenticatorPush(driver);
        showAuthenticatorNumber(driver);

        System.out.println();
        System.out.println("==> Approve the sign-in on your Microsoft Authenticator app.");
        System.out.println("==> Waiting up to 5 minutes - no need to press anything here.");

        try {
            waitUntilSignedIn(driver, Duration.ofMinutes(5));
        } catch (RuntimeException e) {
            System.out.println("Still on the sign-in page. Finish it manually, then press Enter.");
            scanner.nextLine();
            dismissInterruptPages(driver, 5);
        }
    }

    /** Picks "Approve a request on my Microsoft Authenticator app" on the method chooser. */
    private static void chooseAuthenticatorPush(WebDriver driver) {
        clickIfPresent(driver, By.xpath(
                "//div[@data-value='PhoneAppNotification']"
                + " | //*[contains(text(),'Approve a request')]"), 10);
    }

    /** Prints the two-digit number that has to be typed into the Authenticator app. */
    private static void showAuthenticatorNumber(WebDriver driver) {
        try {
            String number = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.visibilityOfElementLocated(
                            By.id("idRichContext_DisplaySign")))
                    .getText();
            System.out.println();
            System.out.println("    Enter this number in Authenticator: " + number);
        } catch (RuntimeException e) {
            // Plain approve/deny prompt - no number to show.
        }
    }

    /**
     * Polls until the browser leaves login.microsoftonline.com, clearing the
     * "Stay signed in?" style pages that pop up right after the MFA approval.
     */
    private static void waitUntilSignedIn(WebDriver driver, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onLoginPage(driver)) {
                System.out.println("Sign-in complete.");
                return;
            }
            dismissInterruptPages(driver, 3);
        }
        throw new IllegalStateException("Sign-in did not complete within " + timeout);
    }

    /**
     * Types the password and verifies it stuck. The Entra sign-in page is a SPA: it often
     * re-renders right after the email step and silently wipes whatever was typed, which
     * leaves an empty box and a "Sign in" click that does nothing.
     */
    private static void typePassword(WebDriver driver) {
        if (USER_PASSWORD == null || USER_PASSWORD.isBlank()) {
            throw new IllegalStateException(
                    "A sign-in is needed but no password was supplied. Set it for this"
                    + " terminal and run again:\n"
                    + "    $env:ST1_PASSWORD = \"<your password>\"\n"
                    + "Or sign in once by hand in the automation Edge profile ("
                    + PROFILE_DIR + "), after which the session is reused.");
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement passwordInput = wait.until(
                    ExpectedConditions.elementToBeClickable(By.name("passwd")));
            passwordInput.click();
            passwordInput.clear();
            passwordInput.sendKeys(USER_PASSWORD);

            String value = passwordInput.getAttribute("value");
            if (value != null && value.length() == USER_PASSWORD.length()) {
                System.out.println("Password typed (attempt " + attempt + ").");
                return;
            }
            System.out.println("Password box was cleared by the page - retrying (" + attempt + "/3).");
        }
        throw new IllegalStateException("Could not keep the password in the input field");
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "(no message)";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    /** "Not now" on the "Let's keep your account secure" page - id varies, so match the text too. */
    private static final By NOT_NOW = By.xpath(
            "//*[@id='iCancel' or @id='btnAskLater' or @id='iShowSkip'"
            + " or normalize-space(text())='Not now' or normalize-space(text())='Skip setup']");

    /**
     * Clicks through the Entra ID interrupt screens that follow a password:
     *   - "Let's keep your account secure" -> "Not now"
     *   - "Stay signed in?"                -> "Yes" (keeps this profile authenticated)
     * Anything that is not on screen is simply skipped.
     */
    private static void dismissInterruptPages(WebDriver driver, int firstWaitSeconds) {
        for (int i = 0; i < 4; i++) {
            boolean clicked = false;

            // First pass gets a longer wait - the page still has to render after "Sign in".
            clicked |= clickIfPresent(driver, NOT_NOW, i == 0 ? firstWaitSeconds : 3);

            // "Stay signed in?" - tick the box, then Yes.
            if (clickIfPresent(driver, By.id("KmsiCheckboxField"), 3)) {
                clickIfPresent(driver, By.id("idSIButton9"), 3);
                clicked = true;
            }

            if (!clicked) {
                return;
            }
        }
    }

    /** True when the element shows up within a few seconds. */
    private static boolean isPresent(WebDriver driver, By locator) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Clicks the element if it shows up in time; falls back to a JS click. */
    private static boolean clickIfPresent(WebDriver driver, By locator, int seconds) {
        WebElement element;
        try {
            element = new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (RuntimeException e) {
            return false;
        }

        try {
            element.click();
        } catch (RuntimeException e) {
            // Overlay, animation or off-screen link - click it through the DOM instead.
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
        System.out.println("Clicked: " + locator);
        return true;
    }

    /** Single-argument overload kept for the account-picker tile. */
    private static boolean clickIfPresent(WebDriver driver, By locator) {
        return clickIfPresent(driver, locator, 5);
    }

    private static boolean onLoginPage(WebDriver driver) {
        String url = driver.getCurrentUrl();
        return url != null && url.contains(LOGIN_HOST);
    }

    /**
     * Clicks an element that lives somewhere in a frameset. Retries until the timeout,
     * because AMS loads its frames one after the other. The driver is left inside the
     * frame that held the element, ready for the next step.
     */
    private static void clickInAnyFrame(WebDriver driver, By locator, Duration timeout) {
        findInAnyFrame(driver, locator, timeout).click();
        System.out.println("Clicked: " + locator);
    }

    /**
     * Finds an element anywhere in the frameset and leaves the driver inside the frame
     * that holds it. Retries until the timeout, since AMS loads frames one at a time.
     */
    private static WebElement findInAnyFrame(WebDriver driver, By locator, Duration timeout) {
        try {
            // The scan restarts from the top on every poll, so a frame that reloads
            // mid-search (and the stale handles that come with it) is simply retried.
            return fluentWait(driver, timeout).until(d -> {
                d.switchTo().defaultContent();
                return findInFrameTree(d, locator, 0);
            });
        } catch (TimeoutException e) {
            throw new NoSuchElementException("Not found in any frame: " + locator, e);
        }
    }

    /**
     * Finds an element in any open window, leaving the driver on the window and inside
     * the frame that holds it. AMS spreads its screens over several windows, so the one
     * showing the menu is not always the one we were last working in.
     */
    private static WebElement findInAnyWindow(WebDriver driver, By locator, Duration timeout) {
        try {
            return fluentWait(driver, timeout).until(d -> {
                for (String handle : d.getWindowHandles()) {
                    d.switchTo().window(handle);
                    d.switchTo().defaultContent();
                    WebElement found = findInFrameTree(d, locator, 0);
                    if (found != null) {
                        return found;
                    }
                }
                return null;
            });
        } catch (TimeoutException e) {
            throw new NoSuchElementException("Not found in any window: " + locator, e);
        }
    }

    /** Clicks an element, falling back to a DOM click when the real one is refused. */
    private static void clickElement(WebDriver driver, WebElement element) {
        try {
            element.click();
        } catch (RuntimeException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    /** Depth-first search of the current frame and every frame nested below it. */
    private static WebElement findInFrameTree(WebDriver driver, By locator, int depth) {
        for (WebElement element : driver.findElements(locator)) {
            if (element.isDisplayed()) {
                return element;
            }
        }

        if (depth >= 5) {
            return null;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);
            } catch (RuntimeException e) {
                continue;
            }
            WebElement found = findInFrameTree(driver, locator, depth + 1);
            if (found != null) {
                return found;
            }
            driver.switchTo().parentFrame();
        }
        return null;
    }

    /**
     * Opens "Add/Update Equipment Inventory - Restricted Access" from the AMS menu and
     * gets the "Edit Inventory Data" screen ready: level chosen, Action set to Add,
     * Continue clicked. The menu link runs javascript:openApp1(...), which may open a
     * popup window or reuse the current one, so both cases are handled.
     */
    private static void openInventoryEditor(WebDriver driver, String level) {
        Set<String> windowsBefore = driver.getWindowHandles();
        NoSuchElementException lastFailure = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            // After Save/Home the menu can be sitting in a different window than the one
            // we are on, so look for the link everywhere rather than in this window only.
            WebElement link = findInAnyWindow(driver,
                    By.partialLinkText("Add/Update Equipment Inventory"), Duration.ofSeconds(30));
            clickElement(driver, link);
            System.out.println("Clicked Add/Update Equipment Inventory (attempt " + attempt + ")");

            try {
                // openApp1() reuses a named window, so the editor can land in the window we
                // are already on, in a brand new popup, or back in the one used last round.
                switchToWindowShowing(driver, "Edit Inventory Data", windowsBefore,
                        Duration.ofSeconds(15));
                lastFailure = null;
                break;
            } catch (NoSuchElementException e) {
                // A click that only raises the named window leaves it on the old screen -
                // clicking the link again makes it load the editor.
                lastFailure = e;
                System.out.println("Editor did not open - clicking the link again.");
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }

        // "Choose Level to Edit" dropdown.
        WebElement levelDropdown = findInAnyFrame(driver, By.tagName("select"),
                Duration.ofSeconds(30));
        selectOption(levelDropdown, level, Duration.ofSeconds(20));
        System.out.println("Selected level: " + level);

        // Action: (o) Add - matched by value, falling back to the first radio.
        WebElement addRadio = findInAnyFrame(driver, By.xpath(
                "//input[@type='radio' and translate(@value,'ADD','add')='add']"
                + " | (//input[@type='radio'])[1]"), Duration.ofSeconds(15));
        if (!addRadio.isSelected()) {
            addRadio.click();
        }
        System.out.println("Action set to Add.");

        clickContinue(driver);
    }

    /**
     * Switches to whichever window is showing the wanted screen. A brand new popup wins,
     * otherwise any window whose title matches is used - openApp1() targets a named
     * window, so the second visit often reloads an existing one instead of opening one.
     */
    private static void switchToWindowShowing(WebDriver driver, String title,
            Set<String> windowsBefore, Duration timeout) {
        try {
            fluentWait(driver, timeout).until(d -> {
                for (String handle : d.getWindowHandles()) {
                    d.switchTo().window(handle);
                    if (!windowsBefore.contains(handle) || d.getTitle().contains(title)) {
                        System.out.println("Using window: " + d.getTitle());
                        return true;
                    }
                }
                return false;
            });
        } catch (TimeoutException e) {
            throw new NoSuchElementException("No window is showing '" + title + "'", e);
        }
    }

    private static void clickContinue(WebDriver driver) {
        clickButton(driver, "Continue");
    }

    /**
     * Keeps looking for the element and keeps clicking it until one lands or the timeout
     * runs out. Unlike clickInAnyFrame - which waits for the element, then clicks once -
     * every poll re-finds it and retries, so a click that is refused (an open dropdown
     * list still covering the button, a frame reloading mid-click, an element that is
     * there but not yet live) is simply attempted again on the next tick.
     */
    private static void clickWhenReady(WebDriver driver, By locator, Duration timeout) {
        try {
            fluentWait(driver, timeout).until(d -> {
                d.switchTo().defaultContent();
                WebElement element = findInFrameTree(d, locator, 0);
                if (element == null) {
                    return false;
                }
                try {
                    element.click();
                } catch (WebDriverException e) {
                    // Covered by the still-open <select> list - go through the DOM.
                    ((JavascriptExecutor) d).executeScript("arguments[0].click();", element);
                }
                return true;
            });
            System.out.println("Clicked: " + locator);
        } catch (TimeoutException e) {
            throw new NoSuchElementException("Could not click in any frame: " + locator, e);
        }
    }

    /**
     * Waits for a popup to go away after its OK/Cancel, then parks the driver on one of
     * the windows that are left - carrying on inside a window that has just closed only
     * produces NoSuchWindowException further down.
     */
    private static void leaveClosedWindow(WebDriver driver, String popup, Duration timeout) {
        try {
            fluentWait(driver, timeout).until(d -> !d.getWindowHandles().contains(popup));
        } catch (TimeoutException e) {
            System.out.println("Popup is still open - carrying on with the main window.");
        }

        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(popup)) {
                driver.switchTo().window(handle);
                driver.switchTo().defaultContent();
                System.out.println("Back on window: " + driver.getTitle());
                return;
            }
        }
    }

    /** Clicks a form button (Continue, Search, ...) wherever it lives in the frameset. */
    private static void clickButton(WebDriver driver, String caption) {
        String lower = caption.toLowerCase();
        clickInAnyFrame(driver, By.xpath(
                "//input[@type='submit' or @type='button']"
                + "[translate(@value,'" + caption.toUpperCase() + "','" + lower + "')='" + lower + "']"
                + " | //button[normalize-space(.)='" + caption + "']"), Duration.ofSeconds(20));
        waitForFormToSettle(driver);
    }

    /**
     * Ticks a radio button by the text printed next to it. All three options sit in one
     * cell as "<input type=radio> Equipment Using Template", so a radio is matched on the
     * first piece of text that follows it - looking for a radio inside the labelling
     * element would just return the first radio of the group.
     */
    private static void clickRadioByLabel(WebDriver driver, String label) {
        String following =
                "normalize-space(translate(following::text()[normalize-space()!=''][1], '\u00A0', ' '))";

        WebElement radio;
        try {
            radio = findInAnyFrame(driver,
                    By.xpath("//input[@type='radio'][" + following + "='" + label + "']"),
                    Duration.ofSeconds(15));
        } catch (NoSuchElementException e) {
            radio = findInAnyFrame(driver,
                    By.xpath("//input[@type='radio'][contains(" + following + ", '" + label + "')]"),
                    Duration.ofSeconds(15));
        }

        if (!radio.isSelected()) {
            clickElement(driver, radio);
        }
        System.out.println("Selected radio: " + label);
        waitForFormToSettle(driver);
    }

    /** True when the box after the label holds nothing - used to spot a reload wiping it. */
    private static boolean isFieldEmpty(WebDriver driver, String label) {
        try {
            WebElement field;
            try {
                field = findInAnyFrame(driver, labelledField(label, true), Duration.ofSeconds(10));
            } catch (NoSuchElementException e) {
                field = findInAnyFrame(driver, labelledField(label, false), Duration.ofSeconds(10));
            }
            String value = field.getAttribute("value");
            return value == null || value.trim().isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Fills the field that follows a label. AMS uses table layouts rather than real
     * <label> elements, so the field is located as "first input or select after the
     * innermost element whose text is the label". Works for both text boxes and
     * dropdowns, and copes with the &nbsp; / trailing ':' / '*' the legacy pages use.
     */
    private static void fillField(WebDriver driver, String label, String value) {
        By exact = labelledField(label, true);
        By loose = labelledField(label, false);

        StaleElementReferenceException lastFailure = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                // Only the lookup may be reported as "field not found". Anything thrown
                // while setting the value carries its own message naming the entries that
                // were on offer, and that message is the whole diagnostic - rewriting it
                // as "field not found" is what made this look like a locator problem.
                WebElement field;
                try {
                    try {
                        field = findInAnyFrame(driver, exact, Duration.ofSeconds(10));
                    } catch (NoSuchElementException e) {
                        // Label rendered with extra markup/wording - fall back to contains.
                        field = findInAnyFrame(driver, loose, Duration.ofSeconds(10));
                    }
                } catch (NoSuchElementException e) {
                    dumpFormControls(driver);
                    throw new NoSuchElementException("Could not find the field for label '"
                            + label + "'. See the control dump above.", e);
                }

                String tag = field.getTagName();
                try {
                    if ("select".equalsIgnoreCase(tag)) {
                        selectOption(field, value, Duration.ofSeconds(20));
                    } else {
                        typeInto(driver, field, value);
                    }
                } catch (NoSuchElementException e) {
                    dumpFormControls(driver);
                    throw e;
                }
                System.out.println(label + " (" + tag + ") = " + value);
                waitForFormToSettle(driver);
                return;
            } catch (StaleElementReferenceException e) {
                // Picking a value reloads the form to refresh the dependent dropdowns,
                // which detaches every element we had already looked up. Look again.
                lastFailure = e;
                System.out.println("Form reloaded while setting " + label + " - retrying ("
                        + attempt + ")");
                waitForFormToSettle(driver);
            }
        }
        throw new StaleElementReferenceException(
                "Gave up setting '" + label + "': the form kept reloading", lastFailure);
    }

    /**
     * Types into a text box. Some AMS boxes (Service Type on the equipment screen) are
     * marked readonly or sit under an overlay, which makes sendKeys throw or silently
     * drop the text, so the value is checked and written through the DOM if needed.
     */
    private static void typeInto(WebDriver driver, WebElement field, String value) {
        List<String> entries = listEntries(driver, field);
        String wanted = entries.isEmpty() ? value : listEntryFor(entries, value);
        if (!wanted.equals(value)) {
            System.out.println("  (dropdown entry for \"" + value + "\" is \"" + wanted + "\")");
        }

        try {
            field.clear();
            field.sendKeys(wanted);
        } catch (StaleElementReferenceException e) {
            throw e; // the form reloaded - let fillField look the field up again
        } catch (WebDriverException e) {
            setValueThroughDom(driver, field, wanted);
            return;
        }

        if (!wanted.equals(field.getAttribute("value"))) {
            setValueThroughDom(driver, field, wanted);
            return;
        }

        // Only a box with a dropdown is tabbed out of, because only its entry feeds the
        // field below it. Plain text boxes are left with the cursor in them, exactly as
        // in the run that created a cabinet end to end: tabbing out of Service fired a
        // handler that closed the AMS window before Cabinet Type was ever reached.
        if (!entries.isEmpty()) {
            commitValue(driver, field);
        }
    }

    /**
     * Picks the entry a dropdown-backed box will accept. Cabinet Type is an
     * &lt;input&gt; backed by a list rather than a &lt;select&gt;, and AMS only populates
     * Cabinet Model when the box holds one of those entries exactly - the list holds
     * "BORDER  ELEMENT" (upper case, two spaces), so the spreadsheet's "Border element"
     * is read as free text, the lookup finds nothing and Cabinet Model stays empty.
     * Matching ignores case and runs of spaces so the sheet stays readable.
     */
    private static String listEntryFor(List<String> entries, String value) {
        String wanted = squash(value);
        for (String entry : entries) {
            if (squash(entry).equals(wanted)) {
                return entry;
            }
        }
        for (String entry : entries) {
            if (squash(entry).contains(wanted)) {
                return entry;
            }
        }

        StringBuilder available = new StringBuilder();
        for (int i = 0; i < Math.min(entries.size(), 25); i++) {
            available.append("\n  - ").append(entries.get(i));
        }
        if (entries.size() > 25) {
            available.append("\n  ... and ").append(entries.size() - 25).append(" more");
        }
        throw new NoSuchElementException("'" + value + "' is not one of the " + entries.size()
                + " entries in this dropdown. Fix the spelling in the spreadsheet."
                + " Entries:" + available);
    }

    /** The entries behind an input's dropdown arrow; empty for a plain text box. */
    private static List<String> listEntries(WebDriver driver, WebElement field) {
        List<String> entries = new ArrayList<>();
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0];"
                    + "if (!el.list) { return []; }"
                    + "return Array.prototype.map.call(el.list.options, function (o) {"
                    + "  return (o.value || o.text || '').trim();"
                    + "});",
                    field);
            if (raw instanceof List<?> values) {
                for (Object entry : values) {
                    String text = String.valueOf(entry);
                    if (!text.isBlank()) {
                        entries.add(text);
                    }
                }
            }
        } catch (WebDriverException e) {
            return List.of(); // treat it as a plain text box
        }
        return entries;
    }

    /** Case- and spacing-insensitive form, so "Border element" matches "BORDER  ELEMENT". */
    private static String squash(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /**
     * Makes the page act on what was just typed, the same way a person does: tab out of
     * the box once. These forms refresh the dependent dropdown from an onchange that
     * only fires when focus leaves, and sendKeys leaves the cursor sitting in the field.
     *
     * <p>A single TAB matters. Firing the handler twice - dispatching 'change' and also
     * calling el.onchange() - submits the form twice, and AMS answers the second submit
     * with a fresh blank form, which looks exactly like the typed value being wiped.
     */
    private static void commitValue(WebDriver driver, WebElement field) {
        try {
            field.sendKeys(Keys.TAB);
        } catch (StaleElementReferenceException e) {
            // The handler reloaded the form straight away - that is the wanted outcome.
            return;
        } catch (WebDriverException e) {
            fireChangeOnce(driver, field);
        }
        waitForFormToSettle(driver);
    }

    /** Blur path for boxes that will not accept a keystroke - fires the handler once. */
    private static void fireChangeOnce(WebDriver driver, WebElement field) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0];"
                    + "el.dispatchEvent(new Event('change', { bubbles: true }));"
                    + "el.blur();",
                    field);
        } catch (StaleElementReferenceException e) {
            // Form reloaded on the handler - expected.
        } catch (WebDriverException e) {
            System.out.println("  (could not fire the change handler - carrying on)");
        }
    }

    /** Last resort for boxes that will not take typing: set the value and fire onchange. */
    private static void setValueThroughDom(WebDriver driver, WebElement field, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');"
                + "arguments[0].removeAttribute('disabled');"
                + "arguments[0].value = arguments[1];",
                field, value);
        System.out.println("  (typed through the DOM - the box refused keystrokes)");
        fireChangeOnce(driver, field);
    }

    /** Builds the "field that follows this label" XPath, exact or contains. */
    private static By labelledField(String label, boolean exact) {
        // Legacy pages pad labels with &nbsp; and mark required fields with ':' or '*'.
        String text = "normalize-space(translate(., '\u00A0:*', '  '))";
        String match = exact ? text + "='" + label + "'" : "contains(" + text + ", '" + label + "')";
        // "not(.//*[match])" keeps only the innermost element holding the label, so the
        // surrounding <td>/<tr>/<table> wrappers do not win the document-order race.
        return By.xpath("//*[" + match + " and not(.//*[" + match + "])]"
                + "/following::*[self::input or self::select][1]");
    }

    /**
     * Finds the first editable cell sitting under a column heading and returns it.
     * Runs in the page because the results grid has no labels, no ids and no stable
     * names - the only thing tying a box to its meaning is the column it is in, so the
     * heading row is scanned for the wanted text and the same cell index is taken from
     * the data row below it. Checkboxes and hidden fields are skipped, which keeps the
     * leading "select this row" column out of the way.
     */
    private static final String FIND_GRID_CONTROL_JS =
            "var label = arguments[0];"
            + "function norm(s){return (s||'').replace(/\u00a0/g,' ')"
            + "  .replace(/\\s+/g,' ').trim().toLowerCase();}"
            + "var tables = document.getElementsByTagName('table');"
            + "for (var t = 0; t < tables.length; t++) {"
            + "  var rows = tables[t].rows;"
            + "  for (var r = 0; r < rows.length; r++) {"
            + "    var cells = rows[r].cells, idx = -1;"
            + "    for (var c = 0; c < cells.length; c++) {"
            + "      if (norm(cells[c].textContent) === label) { idx = c; break; }"
            + "    }"
            + "    if (idx < 0) continue;"
            + "    for (var d = r + 1; d < rows.length; d++) {"
            + "      var dcells = rows[d].cells;"
            + "      if (idx >= dcells.length) continue;"
            + "      var control = dcells[idx].querySelector("
            + "        'input:not([type=checkbox]):not([type=hidden]), select');"
            + "      if (control) { return control; }"
            + "    }"
            + "  }"
            + "}"
            + "return null;";

    /**
     * Fills one cell of the results grid, picked out by its column heading. Text boxes
     * are typed into, dropdowns are selected; the lookup is retried because ticking or
     * changing a cell can make the page redraw the grid underneath us.
     */
    private static void fillGridField(WebDriver driver, String column, String value) {
        StaleElementReferenceException lastFailure = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement control = findGridControl(driver, column, Duration.ofSeconds(20));
                String tag = control.getTagName();
                if ("select".equalsIgnoreCase(tag)) {
                    selectOption(control, value, Duration.ofSeconds(20));
                } else {
                    typeInto(driver, control, value);
                }
                System.out.println(column + " (" + tag + ") = " + value);
                return;
            } catch (StaleElementReferenceException e) {
                lastFailure = e;
                System.out.println("Grid redrew while setting " + column + " - retrying ("
                        + attempt + "/3)");
                waitForFormToSettle(driver);
            } catch (NoSuchElementException e) {
                dumpFormControls(driver);
                throw new NoSuchElementException("No editable cell under the column '" + column
                        + "'. See the control dump above.", e);
            }
        }
        throw new StaleElementReferenceException(
                "Gave up setting '" + column + "': the grid kept redrawing", lastFailure);
    }

    /** Runs the column lookup over every frame, newest content first, until it hits. */
    private static WebElement findGridControl(WebDriver driver, String column, Duration timeout) {
        String label = column.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim().toLowerCase();
        try {
            return fluentWait(driver, timeout).until(d -> {
                d.switchTo().defaultContent();
                return findGridControlInFrameTree(d, label, 0);
            });
        } catch (TimeoutException e) {
            throw new NoSuchElementException("No grid column named '" + column + "'", e);
        }
    }

    private static WebElement findGridControlInFrameTree(WebDriver driver, String label, int depth) {
        Object found = ((JavascriptExecutor) driver).executeScript(FIND_GRID_CONTROL_JS, label);
        if (found instanceof WebElement) {
            return (WebElement) found;
        }

        if (depth >= 5) {
            return null;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);
            } catch (RuntimeException e) {
                continue;
            }
            WebElement control = findGridControlInFrameTree(driver, label, depth + 1);
            if (control != null) {
                return control;
            }
            driver.switchTo().parentFrame();
        }
        return null;
    }

    /** Prints every visible form control so a failing locator can be pinned by name/id. */
    private static void dumpFormControls(WebDriver driver) {
        System.out.println("---- form controls on the current page ----");
        driver.switchTo().defaultContent();
        dumpFormControls(driver, 0, "top");
        System.out.println("------------------------------------------");
    }

    private static void dumpFormControls(WebDriver driver, int depth, String path) {
        for (WebElement control : driver.findElements(By.cssSelector("input, select"))) {
            try {
                if (!control.isDisplayed()) {
                    continue;
                }
                StringBuilder line = new StringBuilder(path + " | " + control.getTagName()
                        + " name=" + control.getAttribute("name")
                        + " id=" + control.getAttribute("id"));
                // The inline handlers are what populate the dependent dropdowns, so show
                // which event each control actually listens on.
                for (String event : new String[] { "onchange", "onblur", "onkeyup", "onclick" }) {
                    String handler = control.getAttribute(event);
                    if (handler != null && !handler.isBlank()) {
                        line.append("\n      ").append(event).append("=").append(firstLine(handler));
                    }
                }
                if ("select".equalsIgnoreCase(control.getTagName())) {
                    List<WebElement> options = new Select(control).getOptions();
                    line.append(" options=").append(options.size());
                    for (int i = 0; i < Math.min(options.size(), 8); i++) {
                        line.append("\n      * ").append(options.get(i).getText().trim());
                    }
                } else {
                    // An input can carry a dropdown of its own; its entries are the exact
                    // spellings AMS will accept, so they belong in the dump too.
                    List<String> entries = listEntries(driver, control);
                    if (!entries.isEmpty()) {
                        line.append(" list=").append(entries.size());
                        for (int i = 0; i < Math.min(entries.size(), 8); i++) {
                            line.append("\n      * ").append(entries.get(i));
                        }
                    }
                }
                System.out.println("  " + line);
            } catch (RuntimeException ignored) {
                // Control vanished while dumping - not worth failing the diagnostic for.
            }
        }

        if (depth >= 5) {
            return;
        }
        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);
            } catch (RuntimeException e) {
                continue;
            }
            dumpFormControls(driver, depth + 1, path + "/frame[" + i + "]");
            driver.switchTo().parentFrame();
        }
    }

    /**
     * Waits for the reload that a selection can trigger, without guessing how long it
     * takes: the document must report readyState "complete" and hand back the same DOM
     * size on three polls in a row before the form counts as settled.
     */
    private static void waitForFormToSettle(WebDriver driver) {
        String[] previous = { "" };
        int[] stableCount = { 0 };
        try {
            fluentWait(driver, Duration.ofSeconds(20)).until(d -> {
                String snapshot = documentSnapshot(d);
                stableCount[0] = snapshot.equals(previous[0]) ? stableCount[0] + 1 : 0;
                previous[0] = snapshot;
                return snapshot.startsWith("complete") && stableCount[0] >= 2;
            });
        } catch (TimeoutException e) {
            System.out.println("Page was still changing after 20s - carrying on.");
        }
    }

    /** readyState plus DOM size, so a reload in progress is visible as a changing value. */
    private static String documentSnapshot(WebDriver driver) {
        String script = "return document.readyState + ':' + document.documentElement.innerHTML.length;";
        try {
            return String.valueOf(((JavascriptExecutor) driver).executeScript(script));
        } catch (WebDriverException e) {
            // The frame we were in was replaced by the reload - measure the top document.
            driver.switchTo().defaultContent();
            return String.valueOf(((JavascriptExecutor) driver).executeScript(script));
        }
    }

    /**
     * Explicit wait tuned for this frameset: polls often and rides out the stale/missing
     * element churn a frame reload throws while the condition is being evaluated.
     */
    private static Wait<WebDriver> fluentWait(WebDriver driver, Duration timeout) {
        return new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(Duration.ofMillis(250))
                .ignoring(WebDriverException.class);
    }

    /**
     * Selects an option by exact text first, then by a case-insensitive partial match.
     * Dependent dropdowns (Cabinet Model, Service Type) are filled by an onchange on the
     * dropdown above them, so the wanted option can be missing for a moment - keep
     * re-reading the option list until it shows up.
     */
    private static void selectOption(WebElement dropdown, String value, Duration timeout) {
        Wait<WebElement> wait = new FluentWait<>(dropdown)
                .withTimeout(timeout)
                .pollingEvery(Duration.ofMillis(250))
                .ignoring(StaleElementReferenceException.class);
        try {
            wait.until(element -> trySelectOption(element, value));
        } catch (TimeoutException e) {
            StringBuilder available = new StringBuilder();
            for (WebElement option : new Select(dropdown).getOptions()) {
                available.append("\n  - ").append(option.getText().trim());
            }
            throw new NoSuchElementException("No option matching '" + value
                    + "'. Available options:" + available, e);
        }
    }

    /** One attempt at picking the option; false means "not in the list yet". */
    private static boolean trySelectOption(WebElement dropdown, String value) {
        Select select = new Select(dropdown);
        String wanted = squash(value);
        List<WebElement> selected = select.getAllSelectedOptions();
        if (!selected.isEmpty() && squash(selected.get(0).getText()).equals(wanted)) {
            return true; // already set - re-selecting would fire another reload
        }

        // squash(), not equalsIgnoreCase(): these lists are upper case and padded with
        // runs of spaces, so "Generic Border Element Cabnet" has to match
        // "GENERIC BORDER  ELEMENT CABNET" without the sheet having to copy the padding.
        List<WebElement> options = select.getOptions();
        for (WebElement option : options) {
            if (squash(option.getText()).equals(wanted)) {
                chooseOption(select, dropdown, option);
                return true;
            }
        }
        for (WebElement option : options) {
            if (squash(option.getText()).contains(wanted)) {
                chooseOption(select, dropdown, option);
                return true;
            }
        }
        return false;
    }

    /**
     * Picks the option without ever clicking the &lt;select&gt; itself. Clicking the box
     * opens the native, browser-drawn option list, and while that list is up Edge stops
     * answering WebDriver commands - the run then hangs on the very next call with no
     * error (the "VAN Release #" popup froze exactly this way). Clicking the option
     * element alone still fires onchange, which is what the dependent dropdowns need.
     */
    private static void chooseOption(Select select, WebElement dropdown, WebElement option) {
        String wanted = option.getText().trim();
        try {
            option.click();
        } catch (StaleElementReferenceException e) {
            throw e; // the list was rebuilt - let selectOption look it up again
        } catch (WebDriverException e) {
            select.selectByVisibleText(wanted);
        }

        // Some renderings accept the click but never run the handler - confirm, and set
        // it through the DOM (with an explicit change event) when it did not take.
        List<WebElement> selected = select.getAllSelectedOptions();
        if (selected.isEmpty() || !wanted.equalsIgnoreCase(selected.get(0).getText().trim())) {
            WebDriver driver = ((WrapsDriver) dropdown).getWrappedDriver();
            ((JavascriptExecutor) driver).executeScript(
                    "var select = arguments[0], wanted = arguments[1];"
                    + "for (var i = 0; i < select.options.length; i++) {"
                    + "  if (select.options[i].text.trim() === wanted) { select.selectedIndex = i; break; }"
                    + "}"
                    + "select.dispatchEvent(new Event('change', { bubbles: true }));",
                    dropdown, wanted);
            System.out.println("  (option set through the DOM - the click did not take)");
        }
    }
}
