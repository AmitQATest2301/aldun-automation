package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public final class ScreenshotUtil {

    private ScreenshotUtil() {
    }

    /**
     * Saves a PNG to the configured screenshot folder.
     * @return absolute path of the saved file
     */
    public static String capture(WebDriver driver, String name) throws IOException {
        Path dir = Paths.get(ConfigReader.get("screenshotDir"));
        Files.createDirectories(dir);
        String safeName = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path target = dir.resolve(safeName + "_" + DateUtils.fileTimestamp() + ".png");
        Path source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }
}
