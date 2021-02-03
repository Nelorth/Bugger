package selenium;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import tech.bugger.persistence.util.PropertiesReader;

import java.util.concurrent.TimeUnit;

public class SeleniumExtension implements BeforeAllCallback, AfterAllCallback {

    private static String driverType;
    private static String driverPath;
    private static String baseURL;
    private static String os;
    private static WebDriver driver;
    private static WebDriverWait waiter;
    private static PropertiesReader propertiesReader;

    public SeleniumExtension() {
        try {
            propertiesReader = new PropertiesReader(ClassLoader.getSystemResourceAsStream("selenium.properties"));
            driverType = propertiesReader.getString("driver.type");
            driverPath = propertiesReader.getString("driver.path");
            baseURL = propertiesReader.getString("url");
            os = propertiesReader.getString("os");
        } catch (Exception e) {
            throw new InternalError("Could not load properties for selenium tests.", e);
        }

    }

    @Override
    public void beforeAll(ExtensionContext context) {
        setDriverType(driverType);
        if (driverType.equals("firefox")) {
            driver = new FirefoxDriver();
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS); // time to make screenshots ;)
            waiter = new WebDriverWait(driver, 5);
        } else {
            throw new IllegalArgumentException("The configured driver type is not supported!");
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        driver.quit();
    }

    public static void setDriverType(String type) {
        if (type.equals("firefox")) {
            switch (os) {
            case "Windows" -> driverPath += "geckodriver.exe";
            case "Linux" -> driverPath += "geckodriver";
            case "Mac" -> driverPath += "geckodriverMac";
            default -> throw new IllegalArgumentException("The configured OS is invalid!");
            }
            System.setProperty("webdriver.gecko.driver", driverPath);
        } else {
            throw new IllegalArgumentException("The configured driver type is not supported!");
        }
    }

    public static WebDriver getDriver() {
        return driver;
    }

    public static WebElement scrollTo(WebElement element) {
        System.out.println(">>>> begin scrolling to " + element.getAttribute("id"));
        String scrollElementIntoMiddle = ""
                + "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'nearest'})";
        ((JavascriptExecutor) driver).executeScript(scrollElementIntoMiddle, element);
        waiter.until(ExpectedConditions.elementToBeClickable(element));
        try {
            Thread.sleep(150);
        } catch (Exception e) {}

        System.out.println(">>>> end scrolling");
        return element;
    }

    public static String getBaseURL() {
        if (baseURL == null || baseURL.trim().isBlank()) {
            throw new IllegalArgumentException("The configured url must not be null or blank!");
        } else {
            return baseURL;
        }
    }

}
