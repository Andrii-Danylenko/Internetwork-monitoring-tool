import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SshConnectionUI extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField hostnameField;
    private JTextArea commandArea;
    private JTextArea logArea;
    private JTextArea outputArea;
    private JTextArea historyArea;
    private SshConnectionManager sshConnectionManager;

    public SshConnectionUI() {
        setTitle("Internetwork monitoring tool");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel connectionPanel = new JPanel(new GridLayout(3, 2));
        connectionPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        connectionPanel.add(usernameField);

        connectionPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        connectionPanel.add(passwordField);

        connectionPanel.add(new JLabel("Hostname:"));
        hostnameField = new JTextField();
        connectionPanel.add(hostnameField);

        commandArea = new JTextArea(5, 20);
        historyArea = new JTextArea(5, 20);
        historyArea.setEditable(false);

        // Добавляем заголовки для текстовых областей
        JScrollPane commandScrollPane = new JScrollPane(commandArea);
        commandScrollPane.setBorder(BorderFactory.createTitledBorder("Command Input"));
        JScrollPane historyScrollPane = new JScrollPane(historyArea);
        historyScrollPane.setBorder(BorderFactory.createTitledBorder("History"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, commandScrollPane, historyScrollPane);
        splitPane.setResizeWeight(0.5);

        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        outputArea = new JTextArea(20, 35);
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createTitledBorder("Output"));

        JButton connectButton = new JButton("Connect");
        JButton sendCommandButton = new JButton("Send Commands");
        JButton disconnectButton = new JButton("Disconnect");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(sendCommandButton);
        buttonPanel.add(disconnectButton);

        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.add(connectionPanel, BorderLayout.NORTH);
        container.add(splitPane, BorderLayout.WEST);
        container.add(buttonPanel, BorderLayout.SOUTH);
        container.add(logScrollPane, BorderLayout.EAST);
        container.add(outputScrollPane, BorderLayout.CENTER);

        connectButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String hostname = hostnameField.getText();
            sshConnectionManager = new SshConnectionManager(username, password, hostname);
            logArea.append("Connecting to " + hostname + "...\n");
            try {
                SshConnectionManager.executeCommands(List.of("echo 'alive'"), sshConnectionManager);
                logArea.append("Successfully connected to " + hostname + "!\n");
            } catch (IOException ex) {
                logArea.append(hostname.isBlank() || hostname.isEmpty() ? "Error while connecting server via SSH! Hostname is empty!\n" :
                        "Error while connecting to " + hostname + "! Please, check your settings and try again.\n");
            }
        });

        sendCommandButton.addActionListener(e -> {
            if (sshConnectionManager != null) {
                String commandText = commandArea.getText();
                String[] commands = commandText.split("\n");
                try {
                    List<String> commandsList = Arrays.asList(commands);
                    String response = SshConnectionManager.executeCommands(commandsList, sshConnectionManager);
                    logArea.append("Commands sent!\n");
                    outputArea.append(response);
                    commandsList.forEach(command -> historyArea.append(command + "\n"));
                    commandArea.setText("");
                } catch (IOException ex) {
                    logArea.append("Error while sending commands!\n");
                }
            } else {
                logArea.append("Not connected!\n");
            }
        });

        disconnectButton.addActionListener(e -> {
            if (sshConnectionManager != null) {
                sshConnectionManager.close();
                logArea.append("Disconnected!\n");
            } else {
                logArea.append("Not connected!\n");
            }
        });
    }
}
