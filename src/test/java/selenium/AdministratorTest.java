package selenium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import static selenium.TimeCounter.startTime;
import static selenium.TimeCounter.stopTime;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static selenium.Constants.*;

@ExtendWith(SeleniumExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@Order(1)
public class AdministratorTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseURL;

    private final String testID;

    public AdministratorTest() {
        this.testID = "";
    }

    public AdministratorTest(final String testID) {
        this.testID = testID;
    }

    @BeforeEach
    public void setUp(WebDriver driver, WebDriverWait wait, String baseURL) {
        this.driver = driver;
        this.wait = wait;
        this.baseURL = baseURL;
    }

    @Test
    public void T010_login() {
        startTime(testID);
        driver.get(baseURL);
        driver.findElement(By.id("l-login")).click();
        stopTime(testID, "T010 home");
        startTime(testID);
        driver.findElement(By.id("f-login:it-username")).click();
        stopTime(testID, "T010 login");
        startTime(testID);
        driver.findElement(By.id("f-login:it-username")).sendKeys(ADMIN_USERNAME);
        driver.findElement(By.id("f-login:it-password")).sendKeys(ADMIN_PASSWORD);
        driver.findElement(By.id("f-login:cb-login")).click();

        Select languageDropdown = new Select(driver.findElement(By.id("f-language:s-language")));
        assertAll(
                () -> assertEquals("en", languageDropdown.getFirstSelectedOption().getAttribute("value")),
                () -> assertDoesNotThrow(() -> driver.findElement(By.id("p-avatar-thumbnail")))
        );

        stopTime(testID, "T010 home2");
    }

    @Test
    public void T015_administration() {
        startTime(testID);
        new Actions(driver).moveToElement(driver.findElement(By.id("p-avatar-thumbnail"))).perform();
        driver.findElement(By.id("l-configuration")).click();

        assertTrue(driver.getTitle().contains(ADMIN_TITLE));
        stopTime(testID, "T015 admin");
    }

    @Test
    public void T020_create_user() {
        startTime(testID);
        driver.findElement(By.id("l-create")).click();
        driver.findElement(By.id("f-profile-edit:it-username")).sendKeys(ALF_USERNAME + testID);
        stopTime(testID, "T020 profile");
        startTime(testID);
        driver.findElement(By.id("f-profile-edit:it-first-name")).sendKeys(ALF_FIRST_NAME);
        driver.findElement(By.id("f-profile-edit:it-last-name")).sendKeys(ALF_LAST_NAME);
        driver.findElement(By.id("f-profile-edit:it-email")).sendKeys(ALF_EMAIL_USER + testID + EMAIL_HOST);
        driver.findElement(By.id("f-profile-edit:i-password-new")).sendKeys(ALF_PASSWORD);
        driver.findElement(By.id("f-profile-edit:i-password-new-repeat")).sendKeys(ALF_PASSWORD);
        driver.findElement(By.id("f-profile-edit:cb-apply")).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("f-change-user:i-password-change")));
        driver.findElement(By.id("f-change-user:i-password-change")).sendKeys(ADMIN_PASSWORD);
        stopTime(testID, "T020 profile2");
        startTime(testID);
        driver.findElement(By.id("f-change-user:cb-really-change")).click();

        assertAll(
                () -> assertTrue(driver.getTitle().contains(ALF_PROFILE_TITLE + testID)),
                () -> assertEquals(ALF_USERNAME + testID, driver.findElement(By.id("f-profile:ot-username")).getText()),
                () -> assertEquals(ALF_FIRST_NAME, driver.findElement(By.id("f-profile:ot-first-name")).getText()),
                () -> assertEquals(ALF_LAST_NAME, driver.findElement(By.id("f-profile:ot-last-name")).getText()),
                () -> assertEquals(ALF_EMAIL_USER + testID + EMAIL_HOST,
                        driver.findElement(By.id("f-profile:ot-email")).getText())
        );
        stopTime(testID, "T020 profile3");
    }

    @Test
    public void T030_change_voting_weight() {
        startTime(testID);
        driver.findElement(By.id("f-profile:l-edit")).click();

        driver.findElement(By.id("f-profile-edit:it-overwrite-vote")).sendKeys(ALF_VOTING_WEIGHT);
        stopTime(testID, "T030 profile");
        startTime(testID);
        driver.findElement(By.id("f-profile-edit:cb-apply")).click();

        driver.findElement(By.id("f-change-user:i-password-change")).sendKeys(ADMIN_PASSWORD);
        stopTime(testID, "T030 profile2");
        startTime(testID);
        driver.findElement(By.id("f-change-user:cb-really-change")).click();

        assertAll(
                () -> assertTrue(driver.getTitle().contains(ALF_PROFILE_TITLE + testID)),
                () -> assertEquals(ALF_VOTING_WEIGHT, driver.findElement(By.id("f-profile:ot-weight")).getText()),
                () -> assertDoesNotThrow(() -> driver.findElement(By.className("alert-success")))
        );
        stopTime(testID, "T030 profile3");
    }

    @Test
    public void T040_create_topic() {
        startTime(testID);
        driver.findElement(By.id("l-logo")).click();
        driver.findElement(By.id("l-create")).click();
        stopTime(testID, "T040 home");
        startTime(testID);
        driver.findElement(By.id("f-topic-edit:it-title")).sendKeys(TOPIC_FEEDBACK + testID);
        stopTime(testID, "T040 topic-edit");
        startTime(testID);
        driver.findElement(By.id("f-topic-edit:it-description")).sendKeys(TOPIC_FEEDBACK_DESCRIPTION);
        driver.findElement(By.id("f-topic-edit:cb-save")).click();

        assertAll(
                () -> assertTrue(driver.getTitle().contains(TOPIC_FEEDBACK + testID)),
                () -> assertEquals(TOPIC_FEEDBACK_DESCRIPTION,
                                   driver.findElement(By.id("f-topic:ot-description")).getText())
        );

        stopTime(testID, "T040 topic");
    }

    @Test
    public void T050_add_moderator() {
        startTime(testID);
        driver.findElement(By.id("f-moderator-status:cb-image-promote")).click();
        driver.findElement(By.id("f-promote-mod:it-username")).sendKeys(ALF_USERNAME + testID);
        stopTime(testID, "T050 topic");
        startTime(testID);
        driver.findElement(By.id("f-promote-mod:cb-promote")).click();

        assertDoesNotThrow(() -> driver.findElement(By.linkText(ALF_LINK_TEXT + testID)));
        stopTime(testID, "T050 topic2");
    }

    @Test
    public void T060_create_topic_with_same_title() {
        startTime(testID);
        driver.findElement(By.id("l-logo")).click();
        driver.findElement(By.id("l-create")).click();
        stopTime(testID, "T060 home");
        startTime(testID);
        driver.findElement(By.id("f-topic-edit:it-title")).sendKeys(TOPIC_FEEDBACK + testID);
        stopTime(testID, "T060 topic-edit");
        startTime(testID);
        driver.findElement(By.id("f-topic-edit:it-description")).sendKeys(TOPIC_FEEDBACK_DESCRIPTION);
        driver.findElement(By.id("f-topic-edit:cb-save")).click();

        assertDoesNotThrow(() -> driver.findElement(By.className("alert-danger")));
        stopTime(testID, "T060 topic-edit2");
    }

    @Test
    public void T070_change_topic_title() {
        startTime(testID);
        driver.findElement(By.id("f-topic-edit:it-title")).clear();
        driver.findElement(By.id("f-topic-edit:it-title")).sendKeys(TOPIC_GUI + testID);
        driver.findElement(By.id("f-topic-edit:cb-save")).click();
        driver.findElement(By.id("f-moderator-status:cb-image-promote")).click();
        stopTime(testID, "T070 topic");
        startTime(testID);
        driver.findElement(By.id("f-promote-mod:it-username")).sendKeys(ALF_USERNAME + testID);
        stopTime(testID, "T070 topic2");
        startTime(testID);
        driver.findElement(By.id("f-promote-mod:cb-promote")).click();

        assertAll(
                () -> assertTrue(driver.findElement(By.id("title")).getText().contains(TOPIC_GUI + testID)),
                () -> assertEquals(TOPIC_FEEDBACK_DESCRIPTION,
                                   driver.findElement(By.id("f-topic:ot-description")).getText()),
                () -> assertDoesNotThrow(() -> driver.findElement(By.linkText(ALF_LINK_TEXT + testID)))
        );
        stopTime(testID, "T070 topic3");
    }

    @Test
    public void T080_create_report_with_invalid_attachment() {
        startTime(testID);
        driver.findElement(By.id("f-topic:l-create-report")).click();
        new Select(driver.findElement(By.id("f-create-report:s-type"))).selectByValue(TYPE_HINT_OPTION);
        stopTime(testID, "T080 create-report");
        startTime(testID);
        driver.findElement(By.id("f-create-report:it-title")).sendKeys(testID + REPORT_NO_TRANSLATION);
        driver.findElement(By.id("f-create-report:it-post-content")).sendKeys(POST_NO_TRANSLATION);

        String file = absolutePathOf(EVIL_FILE);
        driver.findElement(By.id("f-create-report:it-attachment")).sendKeys(file);
        driver.findElement(By.id("f-create-report:cb-add-attachment")).click();

        assertDoesNotThrow(() -> driver.findElement(By.className("alert-danger")));
        stopTime(testID, "T080 create-report2");
    }

    @Test
    public void T090_create_report_with_valid_attachment() {
        startTime(testID);
        String file = absolutePathOf(FRIENDLY_FILE);
        driver.findElement(By.id("f-create-report:it-attachment")).sendKeys(file);
        driver.findElement(By.id("f-create-report:cb-create")).click();

        assertAll(
                () -> assertTrue(driver.findElement(By.id("title")).getText().contains(testID + REPORT_NO_TRANSLATION)),
                () -> assertEquals(TYPE_HINT_TEXT, driver.findElement(By.id("ot-type")).getText()),
                () -> assertEquals(SEVERITY_MINOR_TEXT, driver.findElement(By.id("ot-severity")).getText())
        );
        stopTime(testID, "T090 report");
    }

    @Test
    public void T100_try_demote_admin() {
        startTime(testID);
        new Actions(driver).moveToElement(driver.findElement(By.id("p-avatar-thumbnail"))).perform();
        driver.findElement(By.id("l-profile")).click();

        driver.findElement(By.id("f-profile:cb-rem-admin")).click();
        stopTime(testID, "T100 profile");
        startTime(testID);
        driver.findElement(By.id("f-change-status:i-user-password")).sendKeys(ADMIN_PASSWORD);
        stopTime(testID, "T100 profile2");
        startTime(testID);
        driver.findElement(By.id("f-change-status:cb-change-status")).click();

        assertDoesNotThrow(() -> driver.findElement(By.className("alert-danger")));
        stopTime(testID, "T100 profile3");
    }

    private String absolutePathOf(String path) {
        try {
            return Path.of(ClassLoader.getSystemResource(path).toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new AssertionError("Could not determine absolute path of " + path, e);
        }
    }

}
