package igor.kos.mastermind.jndi;

import igor.kos.mastermind.exception.ConfigurationReaderException;
import igor.kos.mastermind.model.ConfigurationKey;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Properties;

public class ConfigurationReader {
    private static final String DEFAULT_PROVIDER_URL = "file:D:/";
    private static final String CONFIG_FILE_NAME = "conf.properties";
    private static final String PROVIDER_URL_ENV = "PROVIDER_URL";

    private ConfigurationReader() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getValue(ConfigurationKey key) {
        try (InitialDirContextCloseable context = new InitialDirContextCloseable(configureEnvironment())) {
            Object object = context.lookup(CONFIG_FILE_NAME);
            Path configFilePath = Paths.get(object.toString());

            Properties props = new Properties();
            try (var reader = Files.newBufferedReader(configFilePath)) {
                props.load(reader);
            }

            return props.getProperty(key.getKey());
        } catch (NamingException | IOException e) {
            throw new ConfigurationReaderException("Failed to read configuration value for key: " + key.getKey(), e);
        }
    }

    private static Hashtable<String, String> configureEnvironment() {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");
        env.put(Context.PROVIDER_URL, getProviderUrl());
        return env;
    }

    private static String getProviderUrl() {
        String providerUrl = System.getenv(PROVIDER_URL_ENV);
        return (providerUrl != null && !providerUrl.isBlank()) ? providerUrl : DEFAULT_PROVIDER_URL;
    }
}
