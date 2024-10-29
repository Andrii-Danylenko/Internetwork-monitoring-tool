import java.io.Serializable;

record ConnectionConfig(String username, String password, String hostname) implements Serializable {}