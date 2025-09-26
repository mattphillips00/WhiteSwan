package org.example;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static io.github.bonigarcia.wdm.WebDriverManager.chromedriver;
import static java.lang.String.join;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static org.openqa.selenium.By.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.slf4j.LoggerFactory.getLogger;

public class SwiftBetScraper {
    private static final String SWIFT_BET_TODAY_URL = "https://www.swiftbet.com.au/racing";
    private static final String SWIFT_BET_TOMORROW_URL = "https://www.swiftbet.com.au/racing/tomorrow";
    private static final String GRID_SELECTOR = "div.md\\:grid";
    private static final String MEETING_NAME_SELECTOR = "span.text-text-primary.font-semibold";
    private static final String RACE_LINK_SELECTOR = "a[href*='/racing/meeting/']";
    private static final String AUS_NZL_MEETINGS_BUTTON = "//button[contains(., 'AUS/NZL')]";
    private static final String THOROUGHBRED_MEETINGS_BUTTON = "svg[data-testid='icon_thoroughbred']";
    private static final String OUTPUT_FILE = "df_races_data.csv";

    private static final Logger LOG = getLogger(SwiftBetScraper.class);
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final List<Race> allRaces;

    public SwiftBetScraper() {
        chromedriver().setup();
        this.driver = new ChromeDriver();
        this.wait = new WebDriverWait(driver, ofSeconds(20));
        this.allRaces = new ArrayList<>();
    }

    public static void main(String[] args) {
        final SwiftBetScraper scraper = new SwiftBetScraper();
        scraper.run();
    }

    public void run() {
        try {
            LOG.info("Beginning scraping of {}", SWIFT_BET_TODAY_URL);

            processRaces(SWIFT_BET_TODAY_URL);
            processRaces(SWIFT_BET_TOMORROW_URL);

            populateMeetingNames();

            saveToCsv();

            LOG.info("Scraping complete. Data saved to {}", OUTPUT_FILE);
        } finally {
            driver.quit();
        }
    }

    private void processRaces(String url) {
        driver.get(url);
        selectAusNzlMeetings();
        selectThoroughbredMeetings();

        WebElement meetings = wait.until(presenceOfElementLocated(cssSelector(GRID_SELECTOR)));
        processUpcomingHorseRaces(meetings);
    }

    private void selectAusNzlMeetings() {
        final WebElement button = wait.until(elementToBeClickable(xpath(AUS_NZL_MEETINGS_BUTTON)));
        button.click();
    }

    private void selectThoroughbredMeetings() {
        final WebElement button = wait.until(elementToBeClickable(cssSelector(THOROUGHBRED_MEETINGS_BUTTON)));
        button.click();
    }

    private void processUpcomingHorseRaces(WebElement meetings) {
        try {
            final Map<String, List<Race>> meetingToRaceTimes = mapMeetingsToRaceTimes(meetings);
            addRaceNumbers(meetingToRaceTimes);
        } catch (Exception e) {
            LOG.error("Failed to process upcoming races", e);
        }
    }

    private Map<String, List<Race>> mapMeetingsToRaceTimes(WebElement type) {
        Map<String, List<Race>> meetingToRaceTimes = new HashMap<>();

        final List<WebElement> links = type.findElements(cssSelector(RACE_LINK_SELECTOR));
        for (WebElement link : links) {
            final String href = link.getAttribute("href");
            if (href == null || !href.contains("/race/")) {
                continue;
            }

            final String raceTime = link.findElement(tagName("span")).getText().trim();
            final String meetingId = href.split("/")[5];

            meetingToRaceTimes.computeIfAbsent(meetingId, k -> new ArrayList<>())
                .add(new Race(meetingId, raceTime, href));
        }

        return meetingToRaceTimes;
    }

    private void addRaceNumbers(Map<String, List<Race>> meetingToRaceTimes) {
        for (Entry<String, List<Race>> entry : meetingToRaceTimes.entrySet()) {
            final List<Race> races = entry.getValue();
            for (int i = 0; i < races.size(); i++) {
                races.get(i).setRaceNumber(valueOf(i + 1));
            }
            allRaces.addAll(races);
        }
    }

    private void populateMeetingNames() {
        List<String> meetingNames = new ArrayList<>();
        List<WebElement> elements = driver.findElements(cssSelector(MEETING_NAME_SELECTOR));
        for (WebElement e : elements) {
            meetingNames.add(e.getText().trim());
        }

        String currMeetingId = allRaces.get(0).getMeetingId();
        int meetingNameIdx = 0;
        String currMeetingName = meetingNames.get(meetingNameIdx);

        for (Race race : allRaces) {
            if (!race.getMeetingId().equals(currMeetingId)) {
                currMeetingId = race.getMeetingId();
                meetingNameIdx++;
                currMeetingName = meetingNames.get(meetingNameIdx);
            }

            race.setMeetingName(currMeetingName);
        }
    }

    private void saveToCsv() {
        try (FileWriter writer = new FileWriter(OUTPUT_FILE)) {
            writer.write("Track Name,Race Number,Race Time,Race URL\n");
            for (Race r : allRaces) {
                String[] row = {r.getMeetingName(), r.getRaceNumber(), r.getRaceTime(), r.getUrl()};
                writer.write(join(",", row) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
