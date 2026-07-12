package utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    // Stores configuration values loaded from config.properties.
    private static final Properties properties = new Properties();

    // Loads sensitive environment variables from the .env file.
    // Missing .env file is ignored to prevent immediate failure.
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();


    // Static block runs once when the class is loaded.
    // It initializes the configuration before any method uses it.
    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config/config.properties")) {

            // Ensures that the configuration file exists.
            if (input == null) {
                throw new RuntimeException("config.properties not found on classpath");
            }

            // Loads key-value pairs from config.properties into memory.
            properties.load(input);

        } catch (IOException e) {

            // Stops execution if configuration cannot be loaded.
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }


    // Private constructor prevents creating ConfigReader objects.
    // This class only provides static utility methods.
    private ConfigReader() {
    }


    // Returns the application URL from configuration.
    public static String getBaseUrl() {
        return properties.getProperty("baseUrl");
    }


    // Returns the selected browser type from configuration.
    public static String getBrowser() {
        return properties.getProperty("browser");
    }


    // Returns the explicit wait duration used by Selenium waits.
    public static int getExplicitWaitSeconds() {
        return Integer.parseInt(
                properties.getProperty("explicitWaitSeconds")
        );
    }


    // Returns the Trendyol email stored in the .env file.
    // Credentials are kept outside the source code for security.
    public static String getEmail() {
        return dotenv.get("TRENDYOL_EMAIL");
    }


    // Returns the Trendyol password stored in the .env file.
    // Passwords are not hard-coded or committed to the repository.
    public static String getPassword() {
        return dotenv.get("TRENDYOL_PASSWORD");
    }
}