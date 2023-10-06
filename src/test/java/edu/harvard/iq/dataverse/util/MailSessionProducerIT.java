package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.util.testing.Tags;
import io.restassured.RestAssured;
import jakarta.mail.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An integration test using a fake SMTP MTA to check for outgoing mails.
 * LIMITATION: This test cannot possibly check if the production and injection of the session via CDI
 *             works, as it is not running within a servlet container. This would require usage of Arquillian
 *             or and end-to-end API test with a deployed application.
 */

@Tag(Tags.INTEGRATION_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(MockitoExtension.class)
class MailSessionProducerIT {
    
    private static final Integer PORT_SMTP = 1025;
    private static final Integer PORT_HTTP = 1080;
    
    static SettingsServiceBean settingsServiceBean = Mockito.mock(SettingsServiceBean.class);;
    static DataverseServiceBean dataverseServiceBean = Mockito.mock(DataverseServiceBean.class);;
    
    @BeforeAll
    static void setUp() {
        // Setup mocks behavior, inject as deps
        BrandingUtil.injectServices(dataverseServiceBean, settingsServiceBean);
    }
    
    @Nested
    @LocalJvmSettings
    @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = "test@test.com")
    @JvmSetting(key = JvmSettings.MAIL_MTA_HOST, method = "tcSmtpHost")
    @JvmSetting(key = JvmSettings.MAIL_MTA_SETTING, method = "tcSmtpPort", varArgs = "port")
    class WithoutAuthentication {
        @Container
        static GenericContainer<?> maildev = new GenericContainer<>("maildev/maildev:2.1.0")
            .withExposedPorts(PORT_HTTP, PORT_SMTP)
            .waitingFor(Wait.forHttp("/"));
        
        static String tcSmtpHost() {
            return maildev.getHost();
        }
        
        static String tcSmtpPort() {
            return maildev.getMappedPort(PORT_SMTP).toString();
        }
        
        @BeforeAll
        static void setup() {
            RestAssured.baseURI = "http://" + tcSmtpHost();
            RestAssured.port = maildev.getMappedPort(PORT_HTTP);
        }
        
        @Test
        void createSession() {
            given().when().get("/email")
                .then()
                .statusCode(200)
                .body("size()", is(0));
            
            // given
            Session session = new MailSessionProducer().getSession();
            MailServiceBean mailer = new MailServiceBean(session, settingsServiceBean);
            
            // when
            boolean sent = mailer.sendSystemEmail("test@example.org", "Test", "Test", false);
            
            // then
            assertTrue(sent);
            //RestAssured.get("/email").body().prettyPrint();
            given().when().get("/email")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].subject", equalTo("Test"));
        }
        
    }
    
    static final String username = "testuser";
    static final String password = "supersecret";
    
    @Nested
    @LocalJvmSettings
    @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = "test@test.com")
    @JvmSetting(key = JvmSettings.MAIL_MTA_HOST, method = "tcSmtpHost")
    @JvmSetting(key = JvmSettings.MAIL_MTA_SETTING, method = "tcSmtpPort", varArgs = "port")
    @JvmSetting(key = JvmSettings.MAIL_MTA_AUTH, value = "yes")
    @JvmSetting(key = JvmSettings.MAIL_MTA_USER, value = username)
    @JvmSetting(key = JvmSettings.MAIL_MTA_PASSWORD, value = password)
    class WithAuthentication {
        @Container
        static GenericContainer<?> maildev = new GenericContainer<>("maildev/maildev:2.1.0")
            .withExposedPorts(PORT_HTTP, PORT_SMTP)
            .withEnv(Map.of(
                "MAILDEV_INCOMING_USER", username,
                "MAILDEV_INCOMING_PASS", password
            ))
            .waitingFor(Wait.forHttp("/"));
        
        static String tcSmtpHost() {
            return maildev.getHost();
        }
        
        static String tcSmtpPort() {
            return maildev.getMappedPort(PORT_SMTP).toString();
        }
        
        @BeforeAll
        static void setup() {
            RestAssured.baseURI = "http://" + tcSmtpHost();
            RestAssured.port = maildev.getMappedPort(PORT_HTTP);
        }
        
        @Test
        void createSession() {
            given().when().get("/email")
                .then()
                .statusCode(200)
                .body("size()", is(0));
            
            // given
            Session session = new MailSessionProducer().getSession();
            MailServiceBean mailer = new MailServiceBean(session, settingsServiceBean);
            
            // when
            boolean sent = mailer.sendSystemEmail("test@example.org", "Test", "Test", false);
            
            // then
            assertTrue(sent);
            //RestAssured.get("/email").body().prettyPrint();
            given().when().get("/email")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].subject", equalTo("Test"));
        }
        
    }
    
}