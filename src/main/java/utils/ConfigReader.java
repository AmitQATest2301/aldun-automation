package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads config.properties from the classpath once.
 * Any key can be overridden on the command line, e.g. mvn test -Dheadless=true
 */
public final class ConfigReader {

    private static final String CONFIG_FILE = "config.properties";
    private static final String LOCAL_SECRETS_FILE = "config.local.properties";
    private static final Properties PROPERTIES = load();

    private ConfigReader() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(CONFIG_FILE + " not found on the classpath (expected in src/test/resources)");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + CONFIG_FILE, e);
        }
        return props;
    }

    public static String get(String key) {
        String value = System.getProperty(key, PROPERTIES.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing config key: " + key);
        }
        return value.trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Reads a secret, first from the environment variable, then from the local
     * file config.local.properties in the project root (git-ignored, never committed).
     * Returns null when neither has it.
     */
    public static String getSecret(String envName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = LOCAL_SECRETS.getProperty(envName);
        }
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static final Properties LOCAL_SECRETS = loadLocalSecrets();

    private static Properties loadLocalSecrets() {
        Properties props = new Properties();
        Path file = Paths.get(LOCAL_SECRETS_FILE);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + file.toAbsolutePath(), e);
            }
        }
        return props;
    }
}
