package com.bootstrapium.tests.internal;

import static com.bootstrapium.config.DriverType.determineEffectiveDriverType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.SessionNotFoundException;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import com.bootstrapium.config.DriverType;
import com.bootstrapium.config.WebDriverWrapper;
import com.bootstrapium.listeners.MethodInterceptor;
import com.bootstrapium.listeners.SauceLabsListener;
import com.bootstrapium.listeners.ScreenshotListener;
import com.bootstrapium.listeners.TestListener;
import com.bootstrapium.reporting.AllureProperties;
import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.testng.SauceOnDemandAuthenticationProvider;

@Listeners({ ScreenshotListener.class, MethodInterceptor.class,
        SauceLabsListener.class, TestListener.class })
public abstract class BaseTest implements SauceOnDemandSessionIdProvider,
        SauceOnDemandAuthenticationProvider {

    private static List<WebDriverWrapper> webDriverPool = Collections
            .synchronizedList(new ArrayList<WebDriverWrapper>());
    private static ThreadLocal<WebDriverWrapper> driverThread;

    private static DriverType desiredDriver = determineEffectiveDriverType();

    protected static final Logger logger = LogManager.getLogger();
    
    private static ThreadLocal<Boolean> requiresReset;
    
    @BeforeSuite(alwaysRun = true)
    public static void instantiateDriverObject() {
        driverThread = new ThreadLocal<WebDriverWrapper>() {
            @Override
            protected WebDriverWrapper initialValue() {
                final WebDriverWrapper webDriver = desiredDriver.instantiate();
                webDriverPool.add(webDriver);
                return webDriver;
            }
        };
        requiresReset = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };
    }

    public static WebDriverWrapper getDriver() {
        return driverThread.get();
    }

    @BeforeMethod(alwaysRun = true)
    public static void clearSession() {
        if(requiresReset.get()) {
        try {
            if (DriverType.isNative()) {
                getDriver().getWrappedAppiumDriver().resetApp();
            } else {
                getDriver().manage().deleteAllCookies();
            }
        } catch (SessionNotFoundException e) {
            logger.error("Session quit unexpectedly.");
        }
        } else {
            requiresReset.set(Boolean.TRUE);
        }
    }

    @AfterSuite(alwaysRun = true)
    public static void closeDriverObject() {
        for (WebDriver driver : webDriverPool) {
            try {
                driver.quit();
            } catch (Exception e) {
                logger.error("Session quit unexpectedly.");
            }
        }
    }

    @AfterSuite(alwaysRun = true)
    public static void createAllureProperties() {
        AllureProperties.create();
    }

    /**
     *
     * @return the Sauce Job id for the current thread
     */
    @Override
    public String getSessionId() {
        SessionId sessionId = getDriver().getWrappedRemoteWebDriver().getSessionId();
        return (sessionId == null) ? null : sessionId.toString();
    }

    /**
     *
     * @return the {@link SauceOnDemandAuthentication} instance containing the
     *         Sauce username/access key
     */
    @Override
    public SauceOnDemandAuthentication getAuthentication() {
        return new SauceOnDemandAuthentication();
    }
}