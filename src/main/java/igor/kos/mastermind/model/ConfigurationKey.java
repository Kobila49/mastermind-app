package igor.kos.mastermind.model;

import lombok.Getter;

@Getter
public enum ConfigurationKey {

    RMI_HOST("rmi.host"), RMI_PORT("rmi.port");

    private final String key;

    ConfigurationKey(String key) {
        this.key = key;
    }

}
