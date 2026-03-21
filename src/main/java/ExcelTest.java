import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import java.io.FileInputStream;

public class ExcelTest {
    public static void main(String[] args) throws Exception {

        // Launch Browser
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get("https://www.google.com");
        System.out.println("Page Title: " + driver.getTitle());

        // Read Excel
        FileInputStream fis = new FileInputStream(
            "C:\\Users\\boraw\\Desktop\\TestData.xlsx");
        XSSFWorkbook wb = new XSSFWorkbook(fis);
        XSSFSheet sheet = wb.getSheetAt(0);
        XSSFRow row = sheet.getRow(0);
        System.out.println("Excel Data: " + row.getCell(0));

        wb.close();
        driver.quit();
    }
}
