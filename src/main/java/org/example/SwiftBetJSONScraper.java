package org.example;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v139.network.Network;
import org.openqa.selenium.devtools.v139.network.model.RequestId;
import org.openqa.selenium.devtools.v139.network.model.Response;
import org.openqa.selenium.devtools.v139.network.model.ResponseReceived;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.time.Duration.ofSeconds;
import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

public class SwiftBetJSONScraper {

    static void main() throws InterruptedException {
        System.setProperty("webdriver.edge.driver", "/Users/seancarey/Documents/edgedriver/msedgedriver");
        EdgeOptions options = new EdgeOptions();
        EdgeDriver driver = new EdgeDriver(options);

        try {
            DevTools devTools = driver.getDevTools();
            devTools.createSession();
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

            Map<RequestId, Response> responses = new HashMap<>();

            devTools.addListener(Network.responseReceived(), (ResponseReceived resp) -> {
                responses.put(resp.getRequestId(), resp.getResponse());
                System.out.println(devTools.send(Network.getResponseBody(resp.getRequestId())));
            });

            driver.get("https://www.swiftbet.com.au/racing");
            selectAusNzlMeetings(new WebDriverWait(driver, ofSeconds(3)));
            Thread.sleep(10000);

            System.out.println(responses);

        } finally {
            driver.quit();
        }
    }

    private static void selectAusNzlMeetings(WebDriverWait wait) {
        final WebElement ausButton = wait.until(elementToBeClickable(xpath("//button[contains(., 'AUS/NZL')]")));
        ausButton.click();
    }
}