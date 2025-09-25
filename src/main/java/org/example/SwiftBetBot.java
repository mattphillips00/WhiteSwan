package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

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

public class SwiftBetBot {
    static void main() {
        List<String[]> races = readRacesFromCSV();
        if (races.isEmpty()) {
            System.out.println("No races found");
            return;
        }

        List<String[]> raceData = races.subList(1, races.size());
        if (raceData.isEmpty()) {
            System.out.println("No race data found: only headers present");
        }

        Random random = new Random();
        String[] selectedRace = raceData.get(random.nextInt(raceData.size()));

        String meetingId = "790702ea-843d-53f4-9b0e-4910d9631c13";

        chromedriver().setup();
        WebDriver driver = new ChromeDriver();

        try {
            driver.get("https://www.swiftbet.com.au/racing");

            WebDriverWait wait = new WebDriverWait(driver, ofSeconds(10));
            wait.until(presenceOfElementLocated(cssSelector("a[href*='/racing/meeting/']")));

            // Get all anchors that lead to races
            List<WebElement> raceLinks = driver.findElements(By.cssSelector("a[href*='/racing/meeting/']"));

            for (WebElement link : raceLinks) {
                String href = link.getAttribute("href");
                if (href.contains(meetingId)) {
                    System.out.println("Found race link: " + href);
                    link.click();
                    break;
                }
            }

            scrapeHorsesData(driver);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

    }

    private static void scrapeHorsesData(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, ofSeconds(10));

        try {
            wait.until(presenceOfElementLocated(cssSelector("div.md\\:last\\:border-none")));
            
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Horse Name", "Odds"});

            final List<WebElement> runners = driver.findElements(cssSelector("div.md\\:last\\:border-none"));
            for (WebElement runner : runners) {
                String name = runner.findElement(cssSelector("span[class*='font-semibold")).getText().trim();
                String odds = runner.findElement(cssSelector("button div span")).getText().trim();
                data.add(new String[]{name, odds});
            }

            saveToCSV(data);
        } catch (Exception e) {
            System.out.println("Error scraping horses data" + e.getMessage());
        }
    }

    private static List<String[]> readRacesFromCSV() {
        List<String[]> races = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("swiftbet_races_data.csv"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                races.add(row);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }

        return races;
    }

    private static void saveToCSV(List<String[]> data) {
        try (FileWriter writer = new FileWriter("df_performed_bets.csv")) {
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
            System.out.println("Successfully saved to df_performed_bets.csv");
        } catch (IOException e) {
            System.err.println("Error writing to df_performed_bets.csv: " + e.getMessage());
        }
    }
}
