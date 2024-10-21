import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

public class SshConnectionManager {

    private Session session;
    private ChannelShell channel;
    private final String username;
    private final String password;
    private final String hostname;
    // Тайм-аут соединения
    // TODO: сделать его динамическим (user-defined)
    private static final long TIMEOUT = 1000L;

    public SshConnectionManager(String username, String password, String hostname) {
        this.username = username;
        this.password = password;
        this.hostname = hostname;
    }

    private Session getSession() throws IOException {
        if (session == null || !session.isConnected()) {
            try {
                session = connect(this, hostname, username, password);
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
        }
        return session;
    }

    private Channel getChannel() throws IOException {
        if (channel == null || !channel.isConnected()) {
            try {
                channel = (ChannelShell) getSession().openChannel("shell");
                channel.connect();
            } catch (Exception e) {
                System.out.println("Error while opening channel: " + e);
                throw new IOException(e);
            }
        }
        return channel;
    }

    private static Session connect(SshConnectionManager connectionManager, String hostname, String username, String password) {

        JSch jSch = new JSch();
        Session session = null;
        try {
            session = jSch.getSession(username, hostname, 22);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(password);
            connectionManager.session = session;
            System.out.println("Connecting SSH to " + hostname + " - Please wait for few seconds... ");
            session.connect();
            System.out.println("Connected!");
        } catch (Exception e) {
            System.out.println("An error occurred while connecting to " + hostname + ": " + e);
        }
        return session;

    }

    public static String executeCommands(List<String> commands, SshConnectionManager sshConnectionManager) throws IOException {
        Channel channel = sshConnectionManager.getChannel();
        System.out.println("Sending commands...");
        sendCommands(channel, commands);
        System.out.println("Finished sending commands!");
        return readChannelOutput(channel);
    }

    private static void sendCommands(Channel channel, List<String> commands) {
        try {
            PrintStream out = new PrintStream(channel.getOutputStream());
            ;
            for (String command : commands) {
                out.println(command);
            }
            out.flush();
        } catch (Exception e) {
            System.out.println("Error while sending commands: " + e);
        }
    }

    // TODO: ПОЧИНИТЬ ХОЛОСТОЙ ЦИКЛ!!!
    private static String readChannelOutput(Channel channel) {
        byte[] buffer = new byte[1024];
        StringBuilder output = new StringBuilder();
        try {
            InputStream in = channel.getInputStream();
            String line = "";
            long startTime = System.currentTimeMillis();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    line = new String(buffer, 0, i);
                    System.out.println(line);
                    output.append(line);
                    startTime = System.currentTimeMillis();
                }

                if (line.contains("logout")) {
                    break;
                }
                if (Math.abs(startTime - System.currentTimeMillis()) > TIMEOUT) {
                    System.out.println("Timed out!");
                    break;
                }
                if (channel.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
        } catch (Exception e) {
            System.out.println("Error while reading channel output: " + e);
        }
        return output.toString();
    }

    public void close() {
        channel.disconnect();
        session.disconnect();
        this.channel = null;
        this.session = null;
        System.out.println("Disconnected channel and session");
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHostname() {
        return hostname;
    }
}