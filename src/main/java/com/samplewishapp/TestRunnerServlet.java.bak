package com.samplewishapp;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import org.apache.poi.xssf.usermodel.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


@WebServlet("/TestRunnerServlet")
@MultipartConfig(maxFileSize = 10485760, maxRequestSize = 10485760)
public class TestRunnerServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Part filePart = request.getPart("testDataFile");
        String uploadDir = getServletContext().getRealPath("/uploads/");
        String outputDir = getServletContext().getRealPath("/outputs/");

        new File(uploadDir).mkdirs();
        new File(outputDir).mkdirs();

        String inputPath = uploadDir + "TestData.xlsx";
        try (InputStream is = filePart.getInputStream();
             FileOutputStream fos = new FileOutputStream(inputPath)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) fos.write(buf, 0, len);
        }

        String outputPath = outputDir + "TestResults.xlsx";

        try {
            List<Map<String, String>> testCases = readExcel(inputPath);
            List<Map<String, String>> results    = executeTests(testCases);
            writeResults(outputPath, results);

            request.setAttribute("message",
                "\u2705 " + results.size() + " test(s) executed successfully!");
            request.setAttribute("downloadLink",
                request.getContextPath() + "/DownloadServlet");

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
        }

        request.getRequestDispatcher("/Upload.jsp").forward(request, response);
    }

    // ── Read Excel ───────────────────────────────────────────
    private List<Map<String, String>> readExcel(String path) throws Exception {
        List<Map<String, String>> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(path);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFRow   hRow  = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < hRow.getLastCellNum(); i++)
                headers.add(hRow.getCell(i).getStringCellValue().trim());

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                XSSFRow row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    XSSFCell cell = row.getCell(c);
                    map.put(headers.get(c), cell != null ? cell.toString() : "");
                }
                data.add(map);
            }
        }
        return data;
    }

    // ── Execute Selenium Tests ───────────────────────────────
    private List<Map<String, String>> executeTests(List<Map<String, String>> cases) {
        List<Map<String, String>> results = new ArrayList<>();
        WebDriver driver = null;

        java.util.logging.Logger log = java.util.logging.Logger
            .getLogger(TestRunnerServlet.class.getName());

        log.info("=== TestRunnerServlet: executeTests() START ===");
        log.info("Total test cases received: " + cases.size());

        try {
            // ── Step 1: Configure ChromeOptions ───────────────
            ChromeOptions opts = new ChromeOptions();
            opts.addArguments(
                "--incognito",                  // ✅ ADDED: fresh session, no cookies
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--remote-allow-origins=*"
            );
            log.info("ChromeOptions configured: incognito + headless");

            // ── Step 2: Auto-detect Environment ───────────────
            String chromeDriverPath = System.getenv("CHROMEDRIVER_PATH");
            if (chromeDriverPath != null && !chromeDriverPath.isEmpty()) {
                log.info("Environment: PIPELINE/DOCKER");
                log.info("ChromeDriver path from env: " + chromeDriverPath);
                System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            } else {
                String localDriver =
                    "C:\\Workspace\\SampleWishApp\\drivers\\chromedriver.exe";
                log.info("Environment: LOCAL WINDOWS");
                log.info("ChromeDriver path (local): " + localDriver);
                System.setProperty("webdriver.chrome.driver", localDriver);
            }

            // ── Step 3: Auto-detect App URL ───────────────────
            String appUrl = System.getenv("APP_URL");
            if (appUrl == null || appUrl.isEmpty()) {
                appUrl = "http://localhost:6060/SampleWishApp/login.jsp";
                log.info("APP_URL not set → using local default: " + appUrl);
            } else {
                if (!appUrl.endsWith("login.jsp")) {
                    appUrl = appUrl.endsWith("/") ? appUrl + "login.jsp" : appUrl + "/login.jsp";
                }
                log.info("APP_URL from env: " + appUrl);
            }

            // ── Step 4: Launch ChromeDriver ───────────────────
            log.info("Launching ChromeDriver...");
            driver = new ChromeDriver(opts);
            log.info("ChromeDriver launched successfully.");

            // ── Step 5: Loop through each test case ───────────
            for (Map<String, String> tc : cases) {
                Map<String, String> res = new LinkedHashMap<>(tc);
                String ts = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                res.put("Timestamp", ts);

                String testCase = tc.getOrDefault("TestCase", "Unknown");
                log.info("--- Running TestCase: " + testCase + " ---");

                try {
                    String username = tc.getOrDefault("Username", "");
                    String password = tc.getOrDefault("Password", "");
                    String wishItem = tc.getOrDefault("WishItem", "");

                    // ✅ FIX 1: Clear cookies before EACH test case
                    driver.manage().deleteAllCookies();
                    log.info("[" + testCase + "] Cookies cleared.");

                    // ✅ FIX 2: Navigate to login.jsp fresh
                    log.info("[" + testCase + "] Step 1: Navigating to " + appUrl);
                    WishAppPage page = new WishAppPage(driver);
                    page.navigateTo(appUrl);

                    log.info("[" + testCase + "] Step 2: Logging in as: " + username);
                    page.login(username, password);

                    log.info("[" + testCase + "] Step 3: Adding wish: " + wishItem);
                    page.addWish(wishItem);

                    log.info("[" + testCase + "] Step 4: Verifying wish added...");
                    boolean verified = page.verifyWish(wishItem);

                    log.info("[" + testCase + "] Step 5: Logging out...");
                    page.logout();

                    String status = verified ? "PASS" : "FAIL";
                    res.put("Status", status);
                    res.put("URL", appUrl);
                    res.put("Action", "login,addWish,verify");
                    res.put("ErrorMessage", verified ? "" :
                        "Wish '" + wishItem + "' not found after adding");

                    log.info("[" + testCase + "] Result: " + status);

                } catch (Exception e) {
                    res.put("Status", "FAIL");
                    res.put("URL", appUrl);
                    res.put("Action", "login,addWish,verify");
                    res.put("ErrorMessage", e.getMessage());
                    log.severe("[" + testCase + "] EXCEPTION: " + e.getMessage());
                    log.severe("[" + testCase + "] Cause: "
                        + (e.getCause() != null ? e.getCause().toString() : "N/A"));
                }
                results.add(res);
            }

        } catch (Exception e) {
            log.severe("FATAL ERROR in executeTests: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("ChromeDriver quit successfully.");
            }
            log.info("=== TestRunnerServlet: executeTests() END ===");
        }
        return results;
    }

    private WebElement getElement(WebDriver d, String type, String val) {
        switch (type.toLowerCase()) {
            case "id":          return d.findElement(By.id(val));
            case "name":        return d.findElement(By.name(val));
            case "xpath":       return d.findElement(By.xpath(val));
            case "css":         return d.findElement(By.cssSelector(val));
            case "linktext":    return d.findElement(By.linkText(val));
            case "classname":   return d.findElement(By.className(val));
            default: throw new IllegalArgumentException("Unknown locator: " + type);
        }
    }

    // ── Write Output Excel ───────────────────────────────────
    private void writeResults(String path, List<Map<String, String>> results) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Results");
            String[] cols = {"TestCase","URL","Action","Status","Timestamp","ErrorMessage"};

            XSSFRow hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++)
                hRow.createCell(i).setCellValue(cols[i]);

            int rn = 1;
            for (Map<String, String> r : results) {
                XSSFRow row = sheet.createRow(rn++);
                row.createCell(0).setCellValue(r.getOrDefault("TestCase",""));
                row.createCell(1).setCellValue(r.getOrDefault("URL",""));
                row.createCell(2).setCellValue(r.getOrDefault("Action",""));
                row.createCell(3).setCellValue(r.getOrDefault("Status",""));
                row.createCell(4).setCellValue(r.getOrDefault("Timestamp",""));
                row.createCell(5).setCellValue(r.getOrDefault("ErrorMessage",""));
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(path)) {
                wb.write(fos);
            }
        }
    }
}
