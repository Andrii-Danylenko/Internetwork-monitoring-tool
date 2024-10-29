import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SshConnectionUI extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField hostnameField;
    private JTextArea commandArea;
    private JTextArea logArea;
    private JTextArea outputArea;
    private JTextArea historyArea;
    private JComboBox<String> configComboBox;
    private Map<String, ConnectionConfig> configs;
    private SshConnectionManager sshConnectionManager;
    private JTabbedPane tabbedPane;
    private boolean isInCommandInput = false;

    public SshConnectionUI() {
        setTitle("Internetwork Monitoring Tool");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        configs = loadConfigs();
        JPanel connectionPanel = createConnectionPanel();
        JPanel commandPanel = createCommandPanel();
        JPanel logPanel = createLogPanel();
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Connection", connectionPanel);
        tabbedPane.addTab("Commands", commandPanel);
        tabbedPane.addTab("Log", logPanel);
        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createConnectionPanel() {
        JPanel connectionPanel = new JPanel();
        connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.Y_AXIS));
        JPanel sshSettingsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        sshSettingsPanel.setBorder(BorderFactory.createTitledBorder("SSH Connection Settings"));
        sshSettingsPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        sshSettingsPanel.add(usernameField);
        sshSettingsPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        sshSettingsPanel.add(passwordField);
        sshSettingsPanel.add(new JLabel("Hostname:"));
        hostnameField = new JTextField();
        sshSettingsPanel.add(hostnameField);
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBorder(BorderFactory.createTitledBorder("Export/import configuration"));
        configComboBox = new JComboBox<>(configs.keySet().toArray(new String[0]));
        configComboBox.addActionListener(e -> loadSelectedConfig());
        configPanel.add(configComboBox);
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JButton connectButton = new JButton("Connect");
        JButton disconnectButton = new JButton("Disconnect");
        JButton saveConfigButton = new JButton("Save Config");
        JButton deleteConfigButton = new JButton("Delete Config");
        JButton loadConfigButton = new JButton("Load Config");

        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());
        saveConfigButton.addActionListener(e -> saveConfig());
        deleteConfigButton.addActionListener(e -> deleteConfig());
        loadConfigButton.addActionListener(e -> loadSelectedConfig());

        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        buttonPanel.add(saveConfigButton);
        buttonPanel.add(deleteConfigButton);
        buttonPanel.add(loadConfigButton);

        connectionPanel.add(sshSettingsPanel);
        connectionPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        connectionPanel.add(configPanel);
        connectionPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        connectionPanel.add(buttonPanel);

        return connectionPanel;
    }

    private JPanel createLogPanel() {
        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        JPanel panel = new JPanel(new BorderLayout());
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        panel.add(logScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCommandPanel() {
        commandArea = new JTextArea(5, 20);
        historyArea = new JTextArea(5, 20);
        historyArea.setEditable(false);

        JScrollPane commandScrollPane = new JScrollPane(commandArea);
        commandScrollPane.setBorder(BorderFactory.createTitledBorder("Command Input"));
        JScrollPane historyScrollPane = new JScrollPane(historyArea);
        historyScrollPane.setBorder(BorderFactory.createTitledBorder("History"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, commandScrollPane, historyScrollPane);
        splitPane.setResizeWeight(0.5);

        outputArea = new JTextArea(20, 35);
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createTitledBorder("Output"));

        JButton sendCommandButton = new JButton("Send Commands");
        // TODO: запись в файл
        JButton dumpToJson = new JButton("Dump output to file");
        sendCommandButton.addActionListener(e -> sendCommands());
        commandArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                System.out.println("focusGained");
                isInCommandInput = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
                System.out.println("focusLost");
                isInCommandInput = false;
            }
        });
        sendCommandButton.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                System.out.println("keyTyped");
            }
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                System.out.println("keyPressed");
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER &&
                    tabbedPane.getSelectedComponent() == tabbedPane.getComponentAt(1) &&
                    !isInCommandInput) {
                    sendCommands();
                }
            }
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                System.out.println("keyReleased");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendCommandButton);

        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPanel.add(splitPane, BorderLayout.WEST);
        commandPanel.add(buttonPanel, BorderLayout.SOUTH);
        commandPanel.add(outputScrollPane, BorderLayout.CENTER);
        return commandPanel;
    }
    private void connectToServer() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String hostname = hostnameField.getText();
        sshConnectionManager = new SshConnectionManager(username, password, hostname);
        try {
            logArea.append("Connecting to " + hostname + "...\n");
            sshConnectionManager.setConnected(true);
            SshConnectionManager.executeCommands(List.of("echo 'alive'"), sshConnectionManager);
            logArea.append("Successfully connected to " + hostname + "!\n");
        } catch (IOException ex) {
            logArea.append("Error while connecting to " + hostname + "! Please check your settings and try again.\n");
        }
    }

    private void disconnectFromServer() {
        try {
            sshConnectionManager.close();
            logArea.append("Disconnected!\n");
        } catch (NullPointerException ex) {
            logArea.append("Not connected!\n");
        }
    }

    private void sendCommands() {
        if (sshConnectionManager != null) {
            String commandText = commandArea.getText();
            String[] commands = commandText.split("\n");
            try {
                java.util.List<String> commandsList = Arrays.stream(commands).filter(str -> !str.isEmpty() && !str.isBlank()).toList();
                String response = SshConnectionManager.executeCommands(commandsList, sshConnectionManager);
                logArea.append("Commands %s sent successfully!\n".formatted(commandsList));
                outputArea.append(response);
                commandsList.forEach(command -> historyArea.append(command + "\n"));
                commandArea.setText("");
            } catch (IOException ex) {
                logArea.append("Error while sending commands!\n");
            }
        } else {
            logArea.append("Not connected!\n");
        }
    }

    private void saveConfig() {
        String configName = JOptionPane.showInputDialog(this, "Enter config name:");
        if (configName != null && !configName.isBlank()) {
            ConnectionConfig config = new ConnectionConfig(usernameField.getText(), new String(passwordField.getPassword()), hostnameField.getText());
            configs.put(configName, config);
            configComboBox.addItem(configName);
            saveConfigs();
            logArea.append("Configuration saved as " + configName + "\n");
        }
    }

    private void deleteConfig() {
        String selectedConfig = (String) configComboBox.getSelectedItem();
        if (selectedConfig != null && configs.containsKey(selectedConfig)) {
            configs.remove(selectedConfig);
            configComboBox.removeItem(selectedConfig);
            saveConfigs();
            logArea.append("Configuration " + selectedConfig + " deleted\n");
        }
    }

    private void loadSelectedConfig() {
        String selectedConfig = (String) configComboBox.getSelectedItem();
        if (selectedConfig != null && configs.containsKey(selectedConfig)) {
            ConnectionConfig config = configs.get(selectedConfig);
            usernameField.setText(config.username());
            passwordField.setText(config.password());
            hostnameField.setText(config.hostname());
            logArea.append("Configuration " + selectedConfig + " loaded\n");
        }
    }

    private Map<String, ConnectionConfig> loadConfigs() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("configs.ser"))) {
            return (Map<String, ConnectionConfig>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new HashMap<>();
        }
    }

    private void saveConfigs() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("configs.ser"))) {
            oos.writeObject(configs);
        } catch (IOException e) {
            logArea.append("Error saving configurations\n");
        }
    }
}
