// filepath: /Users/samarth/Desktop/p2p-file-sharing/src/com/example/p2pfilesharing/P2PGui.java
package com.example.p2pfilesharing;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.*;

public class P2PGui extends JFrame {

    private PeerController controller;
    private JTextArea outputArea;
    private JTextField portField;
    private JTextField sharedDirField;
    private JTextField peerIpField;
    private JTextField peerPortField;
    private JTextField peerIndexField;
    private JTextField fileNameField;
    private JButton startButton;
    private JButton connectButton;
    private JButton listRemoteButton;
    private JButton downloadButton;
    private JButton listLocalButton;
    private JButton listKnownButton;
    private JButton historyButton;

    public P2PGui() {
        super("P2P File Sharing Client");
        initComponents();
        redirectSystemStreams();
        // Get the controller instance, but don't start it automatically here
        // The user will click "Start Server"
        controller = PeerController.getInstance();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // --- Input Panel ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Local Server Config
        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("Your Port:"), gbc);
        portField = new JTextField("6881", 5); // Default port
        gbc.gridx = 1; gbc.gridy = 0; inputPanel.add(portField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; inputPanel.add(new JLabel("Shared Dir:"), gbc);
        sharedDirField = new JTextField("./shared", 15); // Default dir
        gbc.gridx = 3; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        inputPanel.add(sharedDirField, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; // Reset

        startButton = new JButton("Start Server");
        gbc.gridx = 4; gbc.gridy = 0; inputPanel.add(startButton, gbc);

        // Row 1: Peer Connection
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Peer IP:"), gbc);
        peerIpField = new JTextField(15);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; inputPanel.add(peerIpField, gbc);
        gbc.gridwidth = 1; // Reset

        gbc.gridx = 3; gbc.gridy = 1; inputPanel.add(new JLabel("Peer Port:"), gbc);
        peerPortField = new JTextField(5);
        gbc.gridx = 4; gbc.gridy = 1; inputPanel.add(peerPortField, gbc);

        connectButton = new JButton("Connect");
        connectButton.setEnabled(false); // Enable after starting server
        gbc.gridx = 5; gbc.gridy = 1; inputPanel.add(connectButton, gbc);

        // Row 2: Download
        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(new JLabel("Peer Index:"), gbc);
        peerIndexField = new JTextField(3);
        gbc.gridx = 1; gbc.gridy = 2; inputPanel.add(peerIndexField, gbc);

        gbc.gridx = 2; gbc.gridy = 2; inputPanel.add(new JLabel("File Name:"), gbc);
        fileNameField = new JTextField(15);
        gbc.gridx = 3; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        inputPanel.add(fileNameField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; // Reset

        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false); // Enable after starting server
        gbc.gridx = 5; gbc.gridy = 2; inputPanel.add(downloadButton, gbc);

        // Row 3: Other Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        listRemoteButton = new JButton("List Remote Files");
        listLocalButton = new JButton("List Local Files");
        listKnownButton = new JButton("List Known Peers (DB)");
        historyButton = new JButton("View History");
        listRemoteButton.setEnabled(false);
        listLocalButton.setEnabled(false);
        listKnownButton.setEnabled(false);
        historyButton.setEnabled(false);
        actionPanel.add(listRemoteButton);
        actionPanel.add(listLocalButton);
        actionPanel.add(listKnownButton);
        actionPanel.add(historyButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 6; gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(actionPanel, gbc);


        // --- Output Area ---
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // --- Layout Panels ---
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // --- Action Listeners ---
        setupActionListeners();
    }

    private void setupActionListeners() {
        startButton.addActionListener(e -> startPeerServer());
        connectButton.addActionListener(e -> connectToPeer());
        listRemoteButton.addActionListener(e -> listRemoteFiles());
        downloadButton.addActionListener(e -> downloadFile());
        listLocalButton.addActionListener(e -> listLocalFiles());
        listKnownButton.addActionListener(e -> listKnownPeers());
        historyButton.addActionListener(e -> viewHistory());
    }

    private void runInBackground(Runnable task) {
        // Run backend tasks in a separate thread to avoid blocking the GUI
        new Thread(task).start();
    }

    private void startPeerServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            String sharedDir = sharedDirField.getText().trim();

            // Initialize Peer within the controller (this needs modification in PeerController)
            // For now, we assume PeerController.start() can be adapted or we manually init
            // This is a simplification - ideally PeerController would take config differently
            System.out.println("Attempting to start server on port " + port + " with dir " + sharedDir);
            System.out.println("NOTE: GUI uses PeerController singleton. Ensure it's not started elsewhere.");

            // We need a way to initialize the Peer instance within the singleton controller
            // This is a limitation of the current structure when driven by GUI
            // A possible workaround (hacky):
            try {
                java.lang.reflect.Field peerField = PeerController.class.getDeclaredField("peer");
                peerField.setAccessible(true);
                if (peerField.get(controller) == null) {
                     java.lang.reflect.Field viewField = PeerController.class.getDeclaredField("view");
                     viewField.setAccessible(true);
                     Object consoleView = viewField.get(controller); // Keep the original view for now

                     java.lang.reflect.Field persistenceField = PeerController.class.getDeclaredField("persistenceService");
                     persistenceField.setAccessible(true);
                     Object persistenceService = persistenceField.get(controller);

                     // Create the Peer instance manually - requires ConsoleView and PersistenceService
                     // This highlights the need for better dependency injection or factory
                     Peer peer = new Peer(port, sharedDir, (ConsoleView)consoleView, (PersistenceService)persistenceService);
                     peerField.set(controller, peer);

                     // Start server in background
                     runInBackground(peer::startServer);

                     // Initialize commands (also needs reflection or refactoring)
                     // This part is complex to replicate via reflection.
                     // For this basic GUI, we'll call peer methods directly where possible.
                     System.out.println("Server thread started. Commands might not be fully initialized in controller.");

                     // Enable buttons
                     startButton.setEnabled(false);
                     portField.setEnabled(false);
                     sharedDirField.setEnabled(false);
                     connectButton.setEnabled(true);
                     listRemoteButton.setEnabled(true);
                     downloadButton.setEnabled(true);
                     listLocalButton.setEnabled(true);
                     listKnownButton.setEnabled(true);
                     historyButton.setEnabled(true);

                } else {
                     System.out.println("Server already seems to be initialized.");
                }

            } catch (Exception ex) {
                 System.err.println("Error initializing Peer via reflection: " + ex.getMessage());
                 ex.printStackTrace(System.err);
            }


        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid port number.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
             JOptionPane.showMessageDialog(this, "Error starting server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void connectToPeer() {
        try {
            String ip = peerIpField.getText().trim();
            int port = Integer.parseInt(peerPortField.getText().trim());
            if (ip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Peer IP cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Use the Peer instance from the controller
            Peer peer = getPeerInstance();
            if (peer != null) {
                runInBackground(() -> peer.connectToPeer(ip, port));
            } else {
                 System.err.println("Peer instance not available in controller.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid peer port number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listRemoteFiles() {
         Peer peer = getPeerInstance();
         if (peer != null) {
             runInBackground(peer::listAvailableFiles); // Assumes listAvailableFiles prints to console/view
         } else {
              System.err.println("Peer instance not available in controller.");
         }
    }

    private void downloadFile() {
         try {
            int index = Integer.parseInt(peerIndexField.getText().trim());
            String fileName = fileNameField.getText().trim();
             if (fileName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "File name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Peer peer = getPeerInstance();
            if (peer != null) {
                runInBackground(() -> peer.downloadFile(index, fileName));
            } else {
                 System.err.println("Peer instance not available in controller.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid peer index.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listLocalFiles() {
         Peer peer = getPeerInstance();
         if (peer != null) {
             runInBackground(peer::listLocalFiles); // Assumes listLocalFiles prints to console/view
         } else {
              System.err.println("Peer instance not available in controller.");
         }
    }

    private void listKnownPeers() {
        // This uses the PersistenceService directly, which the controller holds
        PersistenceService ps = getPersistenceServiceInstance();
         if (ps != null) {
             runInBackground(() -> {
                 java.util.List<String> peers = ps.loadPeers();
                 // Need a view to display this properly. Redirecting ConsoleView output for now.
                 ConsoleView tempView = new ConsoleView(); // Temporary view to format output
                 tempView.showKnownPeers(peers);
             });
         } else {
              System.err.println("PersistenceService instance not available in controller.");
         }
    }

     private void viewHistory() {
        PersistenceService ps = getPersistenceServiceInstance();
         if (ps != null) {
             runInBackground(() -> {
                 java.util.List<DownloadRecord> history = ps.getDownloadHistory();
                 // Need a view to display this properly. Redirecting ConsoleView output for now.
                  ConsoleView tempView = new ConsoleView(); // Temporary view to format output
                  tempView.showDownloadHistory(history);
             });
         } else {
              System.err.println("PersistenceService instance not available in controller.");
         }
    }

    // Helper to get Peer instance via reflection (use with caution)
    private Peer getPeerInstance() {
        try {
            java.lang.reflect.Field peerField = PeerController.class.getDeclaredField("peer");
            peerField.setAccessible(true);
            return (Peer) peerField.get(controller);
        } catch (Exception e) {
            System.err.println("Could not get Peer instance via reflection: " + e.getMessage());
            return null;
        }
    }

    // Helper to get PersistenceService instance via reflection (use with caution)
     private PersistenceService getPersistenceServiceInstance() {
        try {
            java.lang.reflect.Field persistenceField = PeerController.class.getDeclaredField("persistenceService");
            persistenceField.setAccessible(true);
            return (PersistenceService) persistenceField.get(controller);
        } catch (Exception e) {
            System.err.println("Could not get PersistenceService instance via reflection: " + e.getMessage());
            return null;
        }
    }


    // --- Stream Redirection ---
    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text);
            // Optional: Auto-scroll
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true)); // Redirect error stream too
        System.out.println("System output redirected to GUI.");
    }

    public static void main(String[] args) {
        // Ensure GUI creation is done on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            P2PGui gui = new P2PGui();
            gui.setVisible(true);
        });
        // Do NOT start the PeerController here anymore.
        // The GUI's "Start Server" button will handle initialization.
    }
}