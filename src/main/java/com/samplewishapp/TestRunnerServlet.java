package com.samplewishapp;

import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
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
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        }

        String outputPath = outputDir + "TestResults.xlsx";

        try {
            List<Map<String, String>> testCases = readExcel(inputPath);
            List<Map<String, String>> results = executeTests(testCases);
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
            if (sheet == null) return data;

            XSSFRow hRow = sheet.getRow(0);
            if (hRow == null) return data;

            DataFormatter formatter = new DataFormatter();
            List<String> headers = new ArrayList<>();

            for (int i = 0; i < hRow.getLastCellNum(); i++) {
                String header = formatter.formatCellValue(hRow.getCell(i)).trim();
                headers.add(header);
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                XSSFRow row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> map = new LinkedHashMap<>();
                boolean allBlank = true;

                for (int c = 0; c < headers.size(); c++) {
                    String value = formatter.formatCellValue(row.getCell(c)).trim();
                    if (!value.isEmpty()) {
                        allBlank = false;
                    }
                    map.put(headers.get(c), value);
                }

                if (!allBlank) {
                    data.add(map);
                }
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
        log.info("Total parsed test cases: " + cases.size());
        for (Map<String, String> tc : cases) {
            log.info("Parsed row: " + tc.toString());
        }

        try {
            ChromeOptions opts = new ChromeOptions();
            opts.addArguments(
                "--incognito",
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--remote-allow-origins=*"
            );
            log.info("ChromeOptions configured: incognito + headless");

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

            log.info("Launching ChromeDriver...");
            driver = new ChromeDriver(opts);
            log.info("ChromeDriver launched successfully.");

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

                    driver.manage().deleteAllCookies();
                    log.info("[" + testCase + "] Cookies cleared.");

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
            case "id":        return d.findElement(By.id(val));
            case "name":      return d.findElement(By.name(val));
            case "xpath":     return d.findElement(By.xpath(val));
            case "css":       return d.findElement(By.cssSelector(val));
            case "linktext":  return d.findElement(By.linkText(val));
            case "classname": return d.findElement(By.className(val));
            default: throw new IllegalArgumentException("Unknown locator: " + type);
        }
    }

    // ── Write Output Excel ───────────────────────────────────
    private void writeResults(String path, List<Map<String, String>> results) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Results");
            String[] cols = {"TestCase", "URL", "Action", "Status", "Timestamp", "ErrorMessage"};

            XSSFRow hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                hRow.createCell(i).setCellValue(cols[i]);
            }

            int rn = 1;
            for (Map<String, String> r : results) {
                XSSFRow row = sheet.createRow(rn++);
                row.createCell(0).setCellValue(r.getOrDefault("TestCase", ""));
                row.createCell(1).setCellValue(r.getOrDefault("URL", ""));
                row.createCell(2).setCellValue(r.getOrDefault("Action", ""));
                row.createCell(3).setCellValue(r.getOrDefault("Status", ""));
                row.createCell(4).setCellValue(r.getOrDefault("Timestamp", ""));
                row.createCell(5).setCellValue(r.getOrDefault("ErrorMessage", ""));
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(path)) {
                wb.write(fos);
            }
        }
    }
}