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
					"✅ Test execution completed. Total result row(s): " + results.size());
			request.setAttribute("downloadLink",
					request.getContextPath() + "/DownloadServlet");

		} catch (Exception e) {
			request.setAttribute("error", e.getMessage());
		}

		request.getRequestDispatcher("/Upload.jsp").forward(request, response);
	}

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

	private List<Map<String, String>> executeTests(List<Map<String, String>> cases) {
		List<Map<String, String>> results = new ArrayList<>();
		WebDriver driver = null;

		java.util.logging.Logger log = java.util.logging.Logger
				.getLogger(TestRunnerServlet.class.getName());

		log.info("=== TestRunnerServlet: executeTests() START ===");
		log.info("Total test cases received: " + cases.size());
		for (Map<String, String> tc : cases) {
			log.info("Parsed row: " + tc.toString());
		}

		try {
			ChromeOptions opts = new ChromeOptions();
			opts.addArguments("--incognito");
			opts.addArguments("--headless=new");
			opts.addArguments("--no-sandbox");
			opts.addArguments("--disable-dev-shm-usage");
			opts.addArguments("--disable-gpu");
			opts.addArguments("--disable-software-rasterizer");
			opts.addArguments("--window-size=1920,1080");
			opts.addArguments("--disable-extensions");
			opts.addArguments("--remote-allow-origins=*");

			String chromeBinary = System.getenv("CHROME_BIN");
			if (chromeBinary != null && !chromeBinary.trim().isEmpty() && new File(chromeBinary.trim()).exists()) {
				opts.setBinary(chromeBinary.trim());
				log.info("Using Chrome binary from CHROME_BIN: " + chromeBinary.trim());
			}

			log.info("ChromeOptions configured for headless Linux/Windows execution");

			String envDriver = System.getenv("CHROMEDRIVER_PATH");
			if (envDriver != null && !envDriver.trim().isEmpty() && new File(envDriver.trim()).exists()) {
				System.setProperty("webdriver.chrome.driver", envDriver.trim());
				log.info("Environment driver found: " + envDriver.trim());
			} else {
				log.info("No valid explicit ChromeDriver path found. Using Selenium Manager fallback.");
			}

			String appUrl = System.getenv("APP_URL");
			if (appUrl == null || appUrl.trim().isEmpty()) {
				appUrl = "http://localhost:6060/SampleWishApp/login.jsp";
				log.info("APP_URL not set -> using local default: " + appUrl);
			} else {
				appUrl = appUrl.trim();
				if (!appUrl.endsWith("login.jsp")) {
					appUrl = appUrl.endsWith("/") ? appUrl + "login.jsp" : appUrl + "/login.jsp";
				}
				log.info("APP_URL resolved as: " + appUrl);
			}

			log.info("Launching ChromeDriver...");
			driver = new ChromeDriver(opts);
			log.info("ChromeDriver launched successfully.");

			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));

			for (Map<String, String> tc : cases) {
				Map<String, String> result = new LinkedHashMap<>(tc);
				String testCase = tc.getOrDefault("TestCase", "Unknown");
				String ts = LocalDateTime.now()
						.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

				result.put("Timestamp", ts);
				result.put("URL", appUrl);
				result.put("Action", "login,addWish,verify");

				try {
					String username = tc.getOrDefault("Username", "");
					String password = tc.getOrDefault("Password", "");
					String wishItem = tc.getOrDefault("WishItem", "");

					log.info("--- Running TestCase: " + testCase + " ---");
					log.info("[" + testCase + "] Username: " + username);
					log.info("[" + testCase + "] WishItem: " + wishItem);

					driver.manage().deleteAllCookies();
					log.info("[" + testCase + "] Cookies cleared.");

					WishAppPage page = new WishAppPage(driver);

					log.info("[" + testCase + "] Step 1: Navigate to " + appUrl);
					page.navigateTo(appUrl);

					log.info("[" + testCase + "] Step 2: Login");
					page.login(username, password);

					log.info("[" + testCase + "] Step 3: Add wish");
					page.addWish(wishItem);

					log.info("[" + testCase + "] Step 4: Verify wish");
					boolean verified = page.verifyWish(wishItem);

					log.info("[" + testCase + "] Step 5: Logout");
					page.logout();

					result.put("Status", verified ? "PASS" : "FAIL");
					result.put("ErrorMessage",
							verified ? "" : "Wish '" + wishItem + "' not found after adding");

					log.info("[" + testCase + "] Result: " + result.get("Status"));

				} catch (Exception testEx) {
					result.put("Status", "FAIL");
					result.put("ErrorMessage", safeMessage(testEx));
					log.severe("[" + testCase + "] EXCEPTION: " + safeMessage(testEx));
					log.severe("[" + testCase + "] Cause: "
							+ (testEx.getCause() != null ? testEx.getCause().toString() : "N/A"));
				}

				results.add(result);
			}

		} catch (Exception e) {
			String fatalMsg = safeMessage(e);
			log.severe("FATAL ERROR in executeTests: " + fatalMsg);

			for (Map<String, String> tc : cases) {
				Map<String, String> result = new LinkedHashMap<>(tc);
				String ts = LocalDateTime.now()
						.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				result.put("Timestamp", ts);
				result.put("URL", "");
				result.put("Action", "login,addWish,verify");
				result.put("Status", "FAIL");
				result.put("ErrorMessage", "FATAL: " + fatalMsg);
				results.add(result);
			}

		} finally {
			if (driver != null) {
				try {
					driver.quit();
					log.info("Browser closed.");
				} catch (Exception quitEx) {
					log.warning("Error while closing browser: " + quitEx.getMessage());
				}
			}
			log.info("=== TestRunnerServlet: executeTests() END ===");
		}

		return results;
	}

	private String safeMessage(Throwable t) {
		if (t == null) return "Unknown error";
		String msg = t.getMessage();
		if (msg == null || msg.trim().isEmpty()) {
			msg = t.toString();
		}
		msg = msg.replace("\r", " ").replace("\n", " ").trim();
		return msg;
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