import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SshConnectionUI extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField hostnameField;
    private JTextArea commandArea;
    private JTextArea logArea;
    private JTextArea outputArea;
    private SshConnectionManager sshConnectionManager;

    public SshConnectionUI() {
        setTitle("Internetwork monitoring tool");
        setSize(600, 400);
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
        JScrollPane commandScrollPane = new JScrollPane(commandArea);
        logArea = new JTextArea(10, 20);
        logArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(logArea);

        outputArea = new JTextArea(20, 35);
        outputArea.setEditable(false);
        JScrollPane outputScrollPane1 = new JScrollPane(outputArea);
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
        container.add(commandScrollPane, BorderLayout.WEST);
        container.add(buttonPanel, BorderLayout.SOUTH);
        container.add(outputScrollPane, BorderLayout.EAST);
        container.add(outputScrollPane1, BorderLayout.CENTER);
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
                    String response = SshConnectionManager.executeCommands(new ArrayList<>(java.util.Arrays.asList(commands)), sshConnectionManager);
                    logArea.append("Commands sent!\n");
                    outputArea.append(response);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SshConnectionUI().setVisible(true));
    }
}
