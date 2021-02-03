package selenium;

import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@SelectClasses({TestDBCleaner.class, AdministratorTest.class, TestDBCleaner.class})
@SuiteDisplayName("Bugger System Test")
public class TestSuite {

    @Test
    public void test() {
        System.out.println("AM I EXECUTED?");
    }

}