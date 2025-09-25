package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.time.Duration.ofSeconds;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

public class SwiftBetScraper {
    private static final String SWIFT_BET_TODAY_URL = "https://www.swiftbet.com.au/racing";
    private static final String SWIFT_BET_TOMORROW_URL = "https://www.swiftbet.com.au/racing/tomorrow";

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final List<Race> allRaces;
//    private final Map<String, String> meetingIdToName;

    public SwiftBetScraper() {
        this.driver = new ChromeDriver();
        this.wait = new WebDriverWait(driver, ofSeconds(20));
        this.allRaces = new ArrayList<>();
//        this.meetingIdToName = new HashMap<>();
    }

    static void main() {
        final SwiftBetScraper scraper = new SwiftBetScraper();
        scraper.run();
    }

    public void run() {
        try {
            System.out.printf("Beginning scraping of %s%n", SWIFT_BET_TODAY_URL);

            processRaces(SWIFT_BET_TODAY_URL);
            processRaces(SWIFT_BET_TOMORROW_URL);

            saveToCsv(collectData());
            System.out.println("Scraping complete. Data saved to df_races_data.csv");
        } finally {
            driver.quit();
        }
    }

    private void processRaces(String url) {
        driver.get(url);
        selectAusNzlMeetings();
        selectThoroughbredMeetings();

        WebElement meetings = wait.until(presenceOfElementLocated(cssSelector("div.md\\:grid")));
        processUpcomingHorseRaces(meetings);
    }

    private void selectAusNzlMeetings() {
        final WebElement ausButton = wait.until(elementToBeClickable(xpath("//button[contains(., 'AUS/NZL')]")));
        ausButton.click();
    }

    private void selectThoroughbredMeetings() {
        WebElement horsesButton = wait.until(elementToBeClickable(cssSelector("button[data-testid='icon_thoroughbred']")));
        horsesButton.click();
    }

    private void processUpcomingHorseRaces(WebElement meetings) {
        try {
            final Map<String, List<Race>> meetingToRaceTimes = mapMeetingsToRaceTimes(meetings);
            addRaceNumbers(meetingToRaceTimes);
//            populateMeetingNames(meetingToRaceTimes);
        } catch (Exception ignored) {
        }
    }

    private Map<String, List<Race>> mapMeetingsToRaceTimes(WebElement type) {
        Map<String, List<Race>> meetingToRaceTimes = new HashMap<>();

        final List<WebElement> links = type.findElements(cssSelector("a[href*='/racing/meeting/']"));
        for (WebElement link : links) {
            final String href = link.getAttribute("href");
            if (!href.contains("/race/")) {
                continue;
            }

            final String raceTime = link.findElement(By.tagName("span")).getText().trim();
            final String meetingId = href.split("/")[5];

            meetingToRaceTimes.computeIfAbsent(meetingId, _ -> new ArrayList<>())
                    .add(new Race(meetingId, raceTime, href));
        }

        return meetingToRaceTimes;
    }

    private void addRaceNumbers(Map<String, List<Race>> meetingToRaceTimes) {
        for (Entry<String, List<Race>> entry : meetingToRaceTimes.entrySet()) {
            final List<Race> races = entry.getValue();
            for (int i = 0; i < races.size(); i++) {
                races.get(i).setRaceNumber(String.valueOf(i + 1));
            }
            allRaces.addAll(races);
        }
    }

    /*private void populateMeetingNames(Map<String, List<Race>> meetingToRaceTimes) {
        for (String meetingId : meetingToRaceTimes.keySet()) {
            try {
                final Race anyRace = meetingToRaceTimes.get(meetingId).getFirst();
                driver.navigate().to(anyRace.getUrl());

                final WebElement element = wait.until(presenceOfElementLocated(cssSelector("button.text-text-button-racenav-dropdown h1")));
                final String meetingName = element.getText().trim();
                meetingIdToName.put(meetingId, meetingName);

                driver.navigate().back();
                wait.until(elementToBeClickable(xpath("//button[contains(., 'AUS/NZ')]")));
                selectAusNzlMeetings();

            } catch (Exception e) {
                System.out.println("Failed to get name for meetingId: " + meetingId);
            }
        }
    }*/

    private List<String[]> collectData() {
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"Track Name", "Race Number", "Race Time", "Race URL"});
        for (Race r : allRaces) {
            data.add(new String[]{
                    r.getMeetingId(),
                    r.getRaceNumber(),
                    r.getRaceTime(),
                    r.getUrl()
            });
        }
        return data;
    }

    private void saveToCsv(List<String[]> data) {
        try (FileWriter writer = new FileWriter("df_races_data.csv")) {
            for (String[] row : data) {
                writer.write(String.join(",", row) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
