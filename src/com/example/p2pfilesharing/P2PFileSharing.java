// The main package for our P2P application
package com.example.p2pfilesharing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream; // Added for Singleton pattern
import java.io.IOException; // Keep this import
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet; // Import necessary SQL classes
import java.sql.SQLException; // For timestamp
import java.sql.Statement; // For formatting timestamp
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator; // Added for Iterator pattern
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException; // Added for Iterator pattern
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities; // Import SwingUtilities

// --- Design Principle: Interface Segregation (Conceptual) ---
// Interfaces for commands define specific actions.
interface Command {
    void execute();
}

/**
 * Main class - Entry point
 */
public class P2PFileSharing {
    public static void main(String[] args) {
        // --- Design Pattern: Singleton ---
        // Get the single instance of PeerController - GUI will use this
        // PeerController controller = PeerController.getInstance();
        // controller.start(); // Don't start the console loop

        // Launch the GUI instead
        SwingUtilities.invokeLater(() -> {
            P2PGui gui = new P2PGui();
            gui.setVisible(true);
        });
    }
}

// --- Simple Record for Download History ---
class DownloadRecord {
    final String fileName;
    final String peerIp;
    final int peerPort;
    final long fileSize;
    final String status;
    final LocalDateTime timestamp;

    DownloadRecord(String fileName, String peerIp, int peerPort, long fileSize, String status, LocalDateTime timestamp) {
        this.fileName = fileName;
        this.peerIp = peerIp;
        this.peerPort = peerPort;
        this.fileSize = fileSize;
        this.status = status;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] %s (%d bytes) from %s:%d - %s",
                timestamp.format(formatter), fileName, fileSize, peerIp, peerPort, status);
    }
}

// --- Design Principle: Single Responsibility Principle (SRP) ---
// ConsoleView is responsible *only* for interacting with the console.
class ConsoleView {
    private final Scanner scanner = new Scanner(System.in); // Made final

    public void displayWelcome() {
        System.out.println("P2P File Sharing Application");
        System.out.println("============================");
    }

    public int getPort() {
        System.out.print("Enter your port number: ");
        int port = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return port;
    }

    public String getSharedDirectory() {
        System.out.print("Enter your shared directory path: ");
        return scanner.nextLine();
    }

    public int getMenuChoice() {
        System.out.println("\nMenu:");
        System.out.println("1. Connect to a peer");
        System.out.println("2. List available files from connected peers");
        System.out.println("3. Download a file");
        System.out.println("4. List local shared files");
        System.out.println("5. List known peers (from DB)"); // Updated label
        System.out.println("6. View Download History"); // New option
        System.out.println("7. Exit"); // Adjusted number
        System.out.print("Select an option: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return choice;
    }

    public String getPeerIp() {
        System.out.print("Enter peer IP address: ");
        return scanner.nextLine();
    }

    public int getPeerPort() {
        System.out.print("Enter peer port: ");
        int port = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return port;
    }

    public int getPeerIndex() {
        System.out.print("Enter peer index: ");
        int index = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return index;
    }

    public String getFileName() {
        System.out.print("Enter file name to download: ");
        return scanner.nextLine();
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showPeers(List<PeerConnection> peers) {
        if (peers.isEmpty()) {
            showMessage("No peers connected.");
            return;
        }
        for (int i = 0; i < peers.size(); i++) {
            PeerConnection peer = peers.get(i);
            showMessage("Peer " + i + " (" + peer.getSocket().getInetAddress().getHostAddress() + ":" + peer.getSocket().getPort() + ")");
            List<String> files = peer.getFiles();
            if (files == null || files.isEmpty()) {
                showMessage("  No files available (or list not retrieved yet)");
            } else {
                for (String file : files) {
                    showMessage("  " + file);
                }
            }
        }
    }

     public void showKnownPeers(List<String> knownPeers) {
        if (knownPeers.isEmpty()) {
            showMessage("No known peers found in database."); // Updated message
            return;
        }
        showMessage("Known Peers (from Database):"); // Updated message
        for (String peerInfo : knownPeers) {
            showMessage("  " + peerInfo);
        }
    }

    public void showLocalFiles(File[] files) {
        if (files == null || files.length == 0) {
            showMessage("No files in shared directory.");
            return;
        }
        showMessage("Local shared files:");
        for (File file : files) {
            if (file.isFile()) {
                showMessage("  " + file.getName() + " (" + file.length() + " bytes)");
            }
        }
    }

    public void showDownloadProgress(long current, long total) {
        int progress = (int) ((current * 100) / total);
        System.out.print("\rDownloading: " + progress + "% complete");
    }

    public void showDownloadComplete(String fileName) {
        System.out.println("\nDownload complete: " + fileName);
    }

    public void showDownloadHistory(List<DownloadRecord> history) {
        if (history.isEmpty()) {
            showMessage("No download history found.");
            return;
        }
        showMessage("Download History:");
        for (DownloadRecord record : history) {
            showMessage("  " + record.toString());
        }
    }
}

// --- Design Principle: Single Responsibility Principle (SRP) ---
// PeerController handles application flow, user commands, and coordinates View and Peer.
// --- Design Pattern: Singleton ---
// Ensures only one instance of the controller exists.
// --- Design Principle: Dependency Inversion Principle (DIP) ---
// Depends on abstractions (Command, ConsoleView, PersistenceService) rather than concrete classes directly.
// --- Design Principle: Open/Closed Principle (OCP) ---
// New commands can be added without modifying this class's main loop,
// just by adding a new Command implementation and mapping it.
class PeerController {
    private static volatile PeerController instance; // Volatile for thread safety
    private Peer peer; // Cannot be final, initialized later in start()
    private final ConsoleView view; // Made final
    private Map<Integer, Command> commands; // Cannot be final, initialized later in start()
    private final PersistenceService persistenceService; // Made final

    // Private constructor for Singleton
    private PeerController() {
        this.view = new ConsoleView();
        // Use the DB-based PersistenceService
        this.persistenceService = new PersistenceService();
    }

    // Double-checked locking for thread-safe Singleton initialization
    public static PeerController getInstance() {
        if (instance == null) {
            synchronized (PeerController.class) {
                if (instance == null) {
                    instance = new PeerController();
                }
            }
        }
        return instance;
    }

    public void start() {
        view.displayWelcome();
        int port = view.getPort();
        String sharedDir = view.getSharedDirectory();

        // Initialize the Peer model
        this.peer = new Peer(port, sharedDir, view, persistenceService); // Pass view for progress updates
        new Thread(peer::startServer).start(); // Start listening in background

        // --- Design Pattern: Command ---
        // Initialize commands map
        commands = new HashMap<>();
        commands.put(1, new ConnectCommand(peer, view));
        commands.put(2, new ListRemoteFilesCommand(peer, view));
        commands.put(3, new DownloadFileCommand(peer, view));
        commands.put(4, new ListLocalFilesCommand(peer, view));
        commands.put(5, new ListKnownPeersCommand(persistenceService, view));
        commands.put(6, new ViewDownloadHistoryCommand(persistenceService, view)); // New command
        commands.put(7, new ExitCommand()); // Adjusted number

        // Main application loop
        while (true) {
            int choice = view.getMenuChoice();
            Command command = commands.get(choice);
            if (command != null) {
                command.execute();
            } else {
                view.showMessage("Invalid option. Please try again.");
            }
        }
    }
}

// --- Command Pattern Implementations ---

class ConnectCommand implements Command {
    private final Peer peer; // Made final
    private final ConsoleView view; // Made final

    public ConnectCommand(Peer peer, ConsoleView view) {
        this.peer = peer;
        this.view = view;
    }

    @Override
    public void execute() {
        String ip = view.getPeerIp();
        int port = view.getPeerPort();
        peer.connectToPeer(ip, port);
    }
}

class ListRemoteFilesCommand implements Command {
    private final Peer peer; // Made final
    // private final ConsoleView view; // Removed - Peer handles display

    public ListRemoteFilesCommand(Peer peer, ConsoleView view) {
        this.peer = peer;
        // this.view = view; // Removed
    }

    @Override
    public void execute() {
        peer.listAvailableFiles(); // Peer now uses view to display
    }
}

class DownloadFileCommand implements Command {
    private final Peer peer; // Made final
    private final ConsoleView view; // Made final

    public DownloadFileCommand(Peer peer, ConsoleView view) {
        this.peer = peer;
        this.view = view;
    }

    @Override
    public void execute() {
        int index = view.getPeerIndex();
        String file = view.getFileName();
        peer.downloadFile(index, file);
    }
}

class ListLocalFilesCommand implements Command {
     private final Peer peer; // Made final
    // private final ConsoleView view; // Removed - Peer handles display

    public ListLocalFilesCommand(Peer peer, ConsoleView view) {
        this.peer = peer;
        // this.view = view; // Removed
    }
    @Override
    public void execute() {
        peer.listLocalFiles(); // Peer now uses view to display
    }
}

class ListKnownPeersCommand implements Command {
    private final PersistenceService persistenceService; // Made final
    private final ConsoleView view; // Made final

    public ListKnownPeersCommand(PersistenceService persistenceService, ConsoleView view) {
        this.persistenceService = persistenceService;
        this.view = view;
    }

    @Override
    public void execute() {
        List<String> knownPeers = persistenceService.loadPeers();
        view.showKnownPeers(knownPeers);
    }
}

// New Command for viewing history
class ViewDownloadHistoryCommand implements Command {
    private final PersistenceService persistenceService;
    private final ConsoleView view;

    public ViewDownloadHistoryCommand(PersistenceService persistenceService, ConsoleView view) {
        this.persistenceService = persistenceService;
        this.view = view;
    }

    @Override
    public void execute() {
        List<DownloadRecord> history = persistenceService.getDownloadHistory();
        view.showDownloadHistory(history);
    }
}

class ExitCommand implements Command {
    @Override
    public void execute() {
        System.out.println("Exiting application...");
        System.exit(0);
    }
}

// --- Basic Persistence Service ---
// --- Design Principle: Single Responsibility Principle (SRP) ---
// Handles loading/saving peer data and download history.
// --- Design Pattern: Repository (Conceptual) ---
// Mediates between the domain (PeerController, Commands) and data mapping layers (JDBC/SQLite).
// It provides a collection-like interface for accessing domain data (known peers, download history)
// while abstracting the underlying data storage mechanism (SQLite database).
class PersistenceService {
    private static final String DB_URL = "jdbc:sqlite:p2p_data.db"; // Database file

    public PersistenceService() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createKnownPeersSQL = "CREATE TABLE IF NOT EXISTS known_peers (" +
                                   "ip_address TEXT NOT NULL, " +
                                   "port INTEGER NOT NULL, " +
                                   "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                   "PRIMARY KEY (ip_address, port)" +
                                   ");";
        // New table for download history
        String createDownloadHistorySQL = "CREATE TABLE IF NOT EXISTS download_history (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "file_name TEXT NOT NULL, " +
                                        "peer_ip TEXT NOT NULL, " +
                                        "peer_port INTEGER NOT NULL, " +
                                        "file_size INTEGER, " +
                                        "status TEXT, " + // e.g., 'Completed', 'Failed'
                                        "download_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                        ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createKnownPeersSQL);
            stmt.execute(createDownloadHistorySQL); // Execute creation for the new table
            System.out.println("Database initialized/checked."); // Use view in real app
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public List<String> loadPeers() {
        List<String> peers = new ArrayList<>();
        String sql = "SELECT ip_address, port FROM known_peers";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String ip = rs.getString("ip_address");
                int port = rs.getInt("port");
                peers.add(ip + ":" + port);
            }
        } catch (SQLException e) {
            System.err.println("Error loading known peers from DB: " + e.getMessage());
        }
        return peers;
    }

    public void savePeer(String ip, int port) {
        String sql = "INSERT OR IGNORE INTO known_peers(ip_address, port, last_seen) VALUES(?, ?, CURRENT_TIMESTAMP)";
        // Update last_seen if already exists (SQLite specific)
        String updateSql = "UPDATE known_peers SET last_seen = CURRENT_TIMESTAMP WHERE ip_address = ? AND port = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement insertStmt = conn.prepareStatement(sql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            insertStmt.setString(1, ip);
            insertStmt.setInt(2, port);
            int rowsAffected = insertStmt.executeUpdate();

            if (rowsAffected == 0) { // If insert didn't happen (peer exists), update last_seen
                updateStmt.setString(1, ip);
                updateStmt.setInt(2, port);
                updateStmt.executeUpdate();
            }

        } catch (SQLException e) {
             System.err.println("Error saving/updating known peer to DB: " + e.getMessage());
        }
    }

    // New method to record download attempt
    public void recordDownload(String fileName, String peerIp, int peerPort, long fileSize, String status) {
        String sql = "INSERT INTO download_history(file_name, peer_ip, peer_port, file_size, status, download_timestamp) VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            pstmt.setString(2, peerIp);
            pstmt.setInt(3, peerPort);
            pstmt.setLong(4, fileSize);
            pstmt.setString(5, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
             System.err.println("Error recording download history to DB: " + e.getMessage());
        }
    }

    // New method to get download history
    public List<DownloadRecord> getDownloadHistory() {
        List<DownloadRecord> history = new ArrayList<>();
        // Order by most recent first
        String sql = "SELECT file_name, peer_ip, peer_port, file_size, status, download_timestamp FROM download_history ORDER BY download_timestamp DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String fileName = rs.getString("file_name");
                String peerIp = rs.getString("peer_ip");
                int peerPort = rs.getInt("peer_port");
                long fileSize = rs.getLong("file_size");
                String status = rs.getString("status");
                // Retrieve timestamp and convert to LocalDateTime
                Timestamp timestamp = rs.getTimestamp("download_timestamp");
                LocalDateTime localDateTime = (timestamp != null) ? timestamp.toLocalDateTime() : null;

                history.add(new DownloadRecord(fileName, peerIp, peerPort, fileSize, status, localDateTime));
            }
        } catch (SQLException e) {
            System.err.println("Error loading download history from DB: " + e.getMessage());
        }
        return history;
    }
}


/**
 * Represents a peer in the P2P network
 * --- Design Principle: Single Responsibility Principle (SRP) ---
 * Peer is now more focused on core P2P logic (server, connections, file ops),
 * delegating I/O to ConsoleView and persistence to PersistenceService.
 * --- Design Pattern: Facade ---
 * Provides a simplified interface (connectToPeer, listAvailableFiles, downloadFile, etc.)
 * to the more complex underlying P2P operations (socket management, threading, protocol handling).
 * --- Design Principle: Dependency Inversion Principle (DIP) ---
 * Depends on abstractions (ConsoleView, PersistenceService) injected via the constructor.
 * --- Design Pattern: Iterator ---
 * Implements Iterable<PeerConnection> to allow iterating over connected peers using the Iterator pattern.
 */
// Add Iterable<PeerConnection> to the class definition
class Peer implements Iterable<PeerConnection> {
    private final int port; // Made final
    private final String sharedDirectory; // Made final
    private final List<PeerConnection> connectedPeers; // Made final and used by Iterator
    private final ConsoleView view; // Made final
    private final PersistenceService persistenceService; // Made final

    // Updated constructor to accept dependencies
    public Peer(int port, String sharedDirectory, ConsoleView view, PersistenceService persistenceService) {
        this.port = port;
        this.sharedDirectory = sharedDirectory;
        this.view = view; // Inject ConsoleView
        this.persistenceService = persistenceService; // Inject PersistenceService
        this.connectedPeers = new CopyOnWriteArrayList<>(); // Use thread-safe list

        // Create shared directory if it doesn't exist
        File dir = new File(sharedDirectory);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                 view.showMessage("Warning: Could not create shared directory: " + sharedDirectory);
            }
        }
    }

    // --- Iterator Pattern Implementation ---
    @Override
    public Iterator<PeerConnection> iterator() {
        return new PeerConnectionIterator(connectedPeers);
    }

    // Inner class implementing the Iterator interface for PeerConnection
    private static class PeerConnectionIterator implements Iterator<PeerConnection> {
        private final List<PeerConnection> peers;
        private int currentIndex = 0;

        public PeerConnectionIterator(List<PeerConnection> peers) {
            // Create a defensive copy or use the thread-safe list directly
            // Using the direct list is okay here as CopyOnWriteArrayList handles concurrent modifications during iteration.
            this.peers = peers;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < peers.size();
        }

        @Override
        public PeerConnection next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more peer connections available.");
            }
            return peers.get(currentIndex++);
        }

        // Optional: Implement remove() if needed, otherwise leave as default (UnsupportedOperationException)
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove operation is not supported for this iterator.");
        }
    }
    // --- End of Iterator Pattern Implementation ---


    /**
     * Start the server to listen for incoming connections
     */
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            view.showMessage("Server started on port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    view.showMessage("New connection from: " + clientSocket.getInetAddress().getHostAddress());

                    // Create a new thread to handle this connection
                    PeerHandler handler = new PeerHandler(clientSocket, this); // Pass Peer instance
                    new Thread(handler).start();
                } catch (IOException e) {
                    view.showMessage("Error accepting connection: " + e.getMessage());
                    // Continue listening for other connections
                }
            }
        } catch (IOException e) {
            view.showMessage("Server error: Could not bind to port " + port + ". " + e.getMessage());
            // Consider exiting or prompting user if server can't start
             System.exit(1); // Exit if server fails critically
        }
    }

    /**
     * Connect to another peer
     */
    public synchronized void connectToPeer(String ip, int port) { // Added synchronized
        // Avoid connecting to self
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            if (ip.equals(localAddress.getHostAddress()) || ip.equals("127.0.0.1") || ip.equals("localhost")) {
                if (port == this.port) {
                    view.showMessage("Cannot connect to yourself.");
                    return;
                }
            }
        } catch (UnknownHostException e) {
             view.showMessage("Warning: Could not determine local host address for self-connection check.");
        }


        // Avoid duplicate connections - using the iterator pattern here for demonstration
        // for (PeerConnection existingConn : connectedPeers) { // Old way
        for (PeerConnection existingConn : this) { // Using the iterator implicitly via enhanced for-loop
            Socket existingSocket = existingConn.getSocket();
            if (existingSocket.getInetAddress().getHostAddress().equals(ip) && existingSocket.getPort() == port) {
                view.showMessage("Already connected to " + ip + ":" + port);
                return;
            }
        }


        try {
            Socket socket = new Socket(ip, port);
            PeerConnection connection = new PeerConnection(socket);
            connectedPeers.add(connection);

            // Request file list from the peer
            connection.sendMessage("LIST");
            String response = connection.receiveMessage(); // Potential blocking call
            if (response != null) {
                 connection.setFiles(parseFileList(response));
                 view.showMessage("Connected to peer at " + ip + ":" + port + " and retrieved file list.");
                 persistenceService.savePeer(ip, port); // Save successful connection
            } else {
                 view.showMessage("Connected to peer at " + ip + ":" + port + " but failed to retrieve file list (peer might have disconnected).");
                 connectedPeers.remove(connection); // Remove potentially problematic connection
                 socket.close();
            }

        } catch (IOException e) {
            view.showMessage("Failed to connect to " + ip + ":" + port + ". " + e.getMessage());
        }
    }

    /**
     * List all files available from connected peers (uses view now)
     */
    public void listAvailableFiles() {
        // Pass the underlying list to the view method, or the view could use the iterator too
        view.showPeers(connectedPeers);
        /* Example of using the iterator explicitly:
        if (!connectedPeers.isEmpty()) {
            view.showMessage("Available files from connected peers:");
            int i = 0;
            for (PeerConnection peerConn : this) { // Using the iterator
                view.showMessage("Peer " + i + " (" + peerConn.getSocket().getInetAddress().getHostAddress() + ":" + peerConn.getSocket().getPort() + ")");
                List<String> files = peerConn.getFiles();
                 if (files == null || files.isEmpty()) {
                    view.showMessage("  No files available (or list not retrieved yet)");
                } else {
                    for (String file : files) {
                        view.showMessage("  " + file);
                    }
                }
                i++;
            }
        } else {
            view.showMessage("No peers connected.");
        }
        */
    }

    /**
     * List files in the local shared directory (uses view now)
     */
    public void listLocalFiles() {
        File dir = new File(sharedDirectory);
        File[] files = dir.listFiles();
        view.showLocalFiles(files); // Delegate display to view
    }

    /**
     * Download a file from a connected peer
     */
    public synchronized void downloadFile(int peerIndex, String fileName) { // Added synchronized
        if (peerIndex < 0 || peerIndex >= connectedPeers.size()) {
            view.showMessage("Invalid peer index.");
            return;
        }

        PeerConnection peerConnection = connectedPeers.get(peerIndex); // Direct access by index is still needed here
        Socket socket = peerConnection.getSocket();
        String peerIp = socket.getInetAddress().getHostAddress();
        int peerPort = socket.getPort();
        long fileSize = -1; // Initialize fileSize - KEEP THIS ONE
        String status = "Failed"; // Default status
        Path filePath = Paths.get(sharedDirectory, fileName); // KEEP THIS ONE

        // Check if the peer actually has the file listed
        List<String> peerFiles = peerConnection.getFiles();
        if (peerFiles == null || !peerFiles.contains(fileName)) {
             view.showMessage("Peer " + peerIndex + " does not list the file: " + fileName);
             // Record failed attempt (optional, but good for history)
             // persistenceService.recordDownload(fileName, peerIp, peerPort, -1, "Failed - Not Listed");
             return;
        }

        FileOutputStream fos = null; // Declare outside try
        try {
            // Request the file
            peerConnection.sendMessage("GET " + fileName);

            // Use the connection's streams
            String response = peerConnection.receiveMessage(); // Read size or error

            if (response == null) {
                 view.showMessage("Peer disconnected before sending file size.");
                 status = "Failed - Disconnected";
                 // persistenceService.recordDownload(fileName, peerIp, peerPort, -1, status);
                 return;
            }
            if (response.equals("FILE_NOT_FOUND")) {
                view.showMessage("File not found on peer (peer reported).");
                status = "Failed - Not Found";
                // persistenceService.recordDownload(fileName, peerIp, peerPort, -1, status);
                return;
            }

            try {
                 fileSize = Long.parseLong(response);
            } catch (NumberFormatException e) {
                 view.showMessage("Invalid file size received from peer: " + response);
                 status = "Failed - Invalid Size";
                 // persistenceService.recordDownload(fileName, peerIp, peerPort, -1, status);
                 return;
            }

            view.showMessage("Downloading file " + fileName + " (" + fileSize + " bytes)");

            // Create file output stream
            fos = new FileOutputStream(filePath.toFile());

            // Get input stream from the connection's socket
            InputStream in = socket.getInputStream(); // Use socket's stream directly for binary data

            // Buffer for reading data
            byte[] buffer = new byte[8192]; // Increased buffer size
            int bytesRead;
            long totalBytesRead = 0;

            // Read file data
            while (totalBytesRead < fileSize && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Update progress using the view
                view.showDownloadProgress(totalBytesRead, fileSize);
            }
            fos.flush(); // Ensure all data is written

            if (totalBytesRead == fileSize) {
                 view.showDownloadComplete(fileName); // Use view for completion message
                 status = "Completed"; // Update status on success
            } else {
                 view.showMessage("\nDownload incomplete. Expected " + fileSize + " bytes, received " + totalBytesRead + " bytes.");
                 status = "Failed - Incomplete";
                 // Optionally delete the partial file
                 Files.deleteIfExists(filePath);
            }

            // No need to refresh file list here, downloading doesn't change remote list

        } catch (IOException e) {
            view.showMessage("\nDownload failed: " + e.getMessage());
            status = "Failed - IO Error";
            // Clean up partial file if it exists
             try {
                 Files.deleteIfExists(filePath);
             } catch (IOException ex) {
                 view.showMessage("Could not delete partial file: " + ex.getMessage());
             }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    view.showMessage("Error closing file output stream: " + e.getMessage());
                }
            }
            // Record the download attempt regardless of success/failure
            persistenceService.recordDownload(fileName, peerIp, peerPort, fileSize, status);
            // Important: Do NOT close the main socket input stream here,
            // as it's needed for subsequent commands (like LIST).
            // The PeerConnection or PeerHandler should manage the socket lifecycle.
        }
    }


    /**
     * Parse a comma-separated list of files
     */
    private List<String> parseFileList(String fileList) {
        if (fileList == null || fileList.isEmpty()) {
            return new ArrayList<>();
        }
        // Trim whitespace around commas and filenames
        String[] files = fileList.split("\\s*,\\s*");
        return new ArrayList<>(Arrays.asList(files)); // Return mutable list
    }

    /**
     * Get a list of files in the shared directory
     */
    public String getFileList() {
        File dir = new File(sharedDirectory);
        // Filter only files, ignore directories
        File[] files = dir.listFiles((d, name) -> new File(d, name).isFile());

        if (files == null || files.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.length; i++) {
            sb.append(files[i].getName());
            if (i < files.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Get the path to the shared directory (used by PeerHandler)
     */
    public String getSharedDirectory() {
        return sharedDirectory;
    }

     /**
     * Get the ConsoleView (used by PeerHandler for error messages)
     */
    public ConsoleView getView() {
        return view;
    }

    // Method to remove a disconnected peer - potentially called by PeerHandler or PeerConnection
    public synchronized void removePeerConnection(PeerConnection connection) {
        if (connection != null) {
            boolean removed = connectedPeers.remove(connection); // Use the list's remove method
            if (removed) {
                view.showMessage("Peer disconnected: " + connection.getSocket().getInetAddress().getHostAddress());
                try {
                    connection.close(); // Ensure resources are released
                } catch (IOException e) {
                    view.showMessage("Error closing disconnected peer socket: " + e.getMessage());
                }
            }
        }
    }

    // Added getter for the list if needed externally, though iterator is preferred
    public List<PeerConnection> getConnectedPeers() {
        return connectedPeers;
    }
}

/**
 * Handles communication with a single connected peer (incoming requests)
 * --- Design Principle: Single Responsibility Principle (SRP) ---
 * Focused on handling requests from one specific peer connection.
 */
class PeerHandler implements Runnable {
    private final Socket socket; // Made final
    private final Peer peer; // Made final
    private final ConsoleView view; // Made final
    private BufferedReader in;
    private PrintWriter out;
    private OutputStream socketOutputStream; // For sending binary file data

    public PeerHandler(Socket socket, Peer peer) {
        this.socket = socket;
        this.peer = peer;
        this.view = peer.getView(); // Get view from Peer

        try {
            // Use specific charset for consistency
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); // Auto-flush enabled
            this.socketOutputStream = socket.getOutputStream(); // Get raw stream for file transfer
        } catch (IOException e) {
            view.showMessage("Error setting up handler for " + socket.getInetAddress().getHostAddress() + ": " + e.getMessage());
            // Close socket if setup fails
            try {
                socket.close();
            } catch (IOException ex) { /* Ignore */ }
        }
    }

    @Override
    public void run() {
         if (in == null || out == null || socketOutputStream == null) {
             view.showMessage("Handler not initialized properly for " + socket.getInetAddress().getHostAddress() + ". Thread exiting.");
             return; // Exit if streams aren't set up
         }

        try {
            String request;
            // Keep reading lines until null (connection closed) or an error occurs
            while ((request = in.readLine()) != null) {
                processRequest(request);
            }
        } catch (SocketException e) {
             // Common when client disconnects abruptly
             view.showMessage("Peer " + socket.getInetAddress().getHostAddress() + " disconnected (SocketException).");
        } catch (IOException e) {
            // Other potential I/O errors
            view.showMessage("Error handling peer " + socket.getInetAddress().getHostAddress() + ": " + e.getMessage());
        } finally {
            // Cleanup resources
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socketOutputStream != null) socketOutputStream.close(); // Close raw stream too
                if (socket != null && !socket.isClosed()) socket.close();
                view.showMessage("Closed connection handler for " + (socket != null ? socket.getInetAddress().getHostAddress() : "unknown peer"));
            } catch (IOException e) {
                view.showMessage("Error closing handler resources: " + e.getMessage());
            }
            // Optionally, notify the main Peer instance to remove this connection if it was tracked
            // This part is tricky as PeerHandler doesn't directly map to a PeerConnection object easily
        }
    }


    /**
     * Process a request from a peer
     */
    private void processRequest(String request) {
        view.showMessage("Received request from " + socket.getInetAddress().getHostAddress() + ": " + request);
        if (request.equals("LIST")) {
            // Send list of files
            String fileList = peer.getFileList();
            out.println(fileList); // Send the comma-separated list
            view.showMessage("Sent file list to " + socket.getInetAddress().getHostAddress());
        } else if (request.startsWith("GET ")) {
            // Handle file download request
            String fileName = request.substring(4).trim();
            Path filePath = Paths.get(peer.getSharedDirectory(), fileName); // Use Paths
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                out.println("FILE_NOT_FOUND"); // Send specific error message
                view.showMessage("File not found for GET request: " + fileName);
                return;
            }

            FileInputStream fis = null; // Declare outside try
            try {
                // 1. Send file size (as a string on its own line)
                out.println(file.length());
                view.showMessage("Sent file size (" + file.length() + ") for: " + fileName);

                // 2. Send file data (binary)
                fis = new FileInputStream(file);

                byte[] buffer = new byte[8192]; // Increased buffer size
                int bytesRead;

                // Use the raw output stream for binary data
                while ((bytesRead = fis.read(buffer)) != -1) {
                    socketOutputStream.write(buffer, 0, bytesRead);
                }
                socketOutputStream.flush(); // Ensure all data is sent

                view.showMessage("Finished sending file: " + fileName + " to " + socket.getInetAddress().getHostAddress());

            } catch (IOException e) {
                view.showMessage("Error sending file " + fileName + ": " + e.getMessage());
                // Client might have disconnected during transfer
            } finally {
                 if (fis != null) {
                     try {
                         fis.close();
                     } catch (IOException e) {
                          view.showMessage("Error closing file input stream for " + fileName + ": " + e.getMessage());
                     }
                 }
                 // Do NOT close socketOutputStream here, it's managed by the main finally block
            }
        } else {
             view.showMessage("Received unknown request: " + request);
             // Optionally send an error response
             // out.println("UNKNOWN_COMMAND");
        }
    }
}


/**
 * Represents a connection to a peer (outgoing perspective)
 * --- Design Principle: Single Responsibility Principle (SRP) ---
 * Manages the state and communication for a single outgoing connection.
 */
class PeerConnection {
    private final Socket socket; // Made final
    private final BufferedReader in; // Made final
    private final PrintWriter out; // Made final
    private List<String> files; // Cannot be final, updated with setFiles

    public PeerConnection(Socket socket) throws IOException {
        this.socket = socket;
        // Use specific charset for consistency
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); // Auto-flush
        this.files = new ArrayList<>(); // Initialize empty list
    }

    /**
     * Send a message (command) to the peer
     */
    public void sendMessage(String message) {
        if (socket.isClosed() || !socket.isConnected() || out.checkError()) {
             System.err.println("Cannot send message, socket is closed or has error.");
             // Optionally throw an exception or handle reconnection logic
             return;
        }
        out.println(message);
    }

    /**
     * Receive a message (response) from the peer.
     * Note: This is a blocking call.
     */
    public String receiveMessage() throws IOException {
         if (socket.isClosed() || !socket.isConnected()) {
             throw new SocketException("Socket is closed or not connected.");
         }
        try {
            return in.readLine(); // Can return null if stream ends
        } catch (SocketException e) {
             System.err.println("SocketException while receiving message: " + e.getMessage() + " (Peer likely disconnected)");
             close(); // Close connection on error
             throw e; // Re-throw
        }
    }

    /**
     * Get the socket connection
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Get the list of available files from this peer
     */
    public synchronized List<String> getFiles() { // Added synchronized
        return files;
    }

    /**
     * Set the list of available files from this peer
     */
    public synchronized void setFiles(List<String> files) { // Added synchronized
        this.files = files;
    }

     /**
     * Close the connection and its streams
     */
    public void close() throws IOException {
        // Close streams first, then socket
        if (in != null) try { in.close(); } catch (IOException e) { /* ignore */ }
        if (out != null) try { out.close(); } catch (Exception e) { /* ignore */ } // PrintWriter close doesn't throw IOException
        if (socket != null && !socket.isClosed()) try { socket.close(); } catch (IOException e) { /* ignore */ }
        System.out.println("PeerConnection closed for " + (socket != null ? socket.getInetAddress().getHostAddress() : "unknown peer"));
    }
}