package org.example;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static io.github.bonigarcia.wdm.WebDriverManager.chromedriver;
import static java.time.Duration.ofSeconds;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.slf4j.LoggerFactory.getLogger;

public class SwiftBetBot {
    private static final String SWIFT_BET_URL = "https://www.swiftbet.com.au/racing";
    private static final String RACE_LINK_SELECTOR = "a[href*='/racing/meeting/']";
    private static final String RUNNERS_SELECTOR = "div.md\\:last\\:border-none";
    private static final String HORSE_NAME_SELECTOR = "span[class*='font-semibold']";
    private static final String ODDS_SELECTOR = "button div span";
    private static final String INPUT_FILE = "swiftbet_races_data.csv";
    private static final String OUTPUT_FILE = "df_performed_bets.csv";

    private static final Logger LOG = getLogger(SwiftBetBot.class);
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final List<String[]> data;

    public SwiftBetBot() {
        chromedriver().setup();
        this.driver = new ChromeDriver();
        this.wait = new WebDriverWait(driver, ofSeconds(20));
        this.data = new ArrayList<>();
        data.add(new String[]{"Horse Name", "Odds"});
    }

    public static void main(String[] args) {
        SwiftBetBot bot = new SwiftBetBot();
        bot.run();
    }

    public void run() {
        List<String[]> races = readRacesFromCSV();
        if (races.isEmpty()) {
            LOG.error("No races found");
            return;
        }

        List<String[]> raceData = races.subList(1, races.size());
        if (raceData.isEmpty()) {
            LOG.error("No race data found: only headers present");
        }

        try {
            visitRandomRace(raceData);
            scrapeHorsesData();
        } finally {
            driver.quit();
        }

    }

    private void visitRandomRace(List<String[]> raceData) {
        driver.get(SWIFT_BET_URL);

        wait.until(presenceOfElementLocated(cssSelector(RACE_LINK_SELECTOR)));
        List<WebElement> raceLinks = driver.findElements(cssSelector(RACE_LINK_SELECTOR));

        Random random = new Random();
        String selectedRace = raceData.get(random.nextInt(raceData.size()))[0];

        for (WebElement link : raceLinks) {
            String href = link.getAttribute("href");
            if (href != null && href.contains(selectedRace)) {
                link.click();
                break;
            }
        }
    }

    private void scrapeHorsesData() {
        try {
            wait.until(presenceOfElementLocated(cssSelector(RUNNERS_SELECTOR)));

            List<WebElement> runners = driver.findElements(cssSelector(RUNNERS_SELECTOR));
            for (WebElement runner : runners) {
                String name = runner.findElement(cssSelector(HORSE_NAME_SELECTOR)).getText().trim();
                String odds = runner.findElement(cssSelector(ODDS_SELECTOR)).getText().trim();
                data.add(new String[]{name, odds});
            }

            saveToCSV();
        } catch (Exception e) {
            LOG.error("Error scraping horses data", e);
        }
    }

    private static List<String[]> readRacesFromCSV() {
        List<String[]> races = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                races.add(row);
            }
        } catch (IOException e) {
            LOG.error("Error reading CSV file", e);
        }

        return races;
    }

    private void saveToCSV() {
        try (FileWriter writer = new FileWriter(OUTPUT_FILE)) {
            for (String[] row : data) {
                String[] escapedRow = new String[row.length];
                for (int i = 0; i < row.length; i++) {
                    if (row[i].contains(",")) {
                        escapedRow[i] = "\"" + row[i] + "\"";
                    } else {
                        escapedRow[i] = row[i];
                    }
                }
                writer.write(String.join(",", escapedRow) + "\n");
            }
            LOG.info("Successfully saved to {}", OUTPUT_FILE);
        } catch (IOException e) {
            LOG.error("Error writing to {}", OUTPUT_FILE, e);
        }
    }
}
