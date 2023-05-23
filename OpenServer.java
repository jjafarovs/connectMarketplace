import java.io.IOException;
import java.util.Scanner;

/**
 * {@link OpenServer}<br />
 * Contains a main method that opens the server side of this messaging program
 *
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 * @version 12/12/2022
 */
public abstract class OpenServer {
    /**
     * Default server port
     */
    public static final int SERVER_PORT = 1200;
    private static final boolean PRINT_DEBUG;
    private static Server server = null;
    private static volatile boolean isOpen;
    
    static {
        PRINT_DEBUG = false;
    }
    
    /**
     * Main method that opens a ServerSocket0 instance and accepts new clients.
     *
     * @param args CLI args; not used
     */
    public static void main(String[] args) {
        // This may not be safe (it has a tendency of erasing data)
        // Adds a shutdown hook to try to close the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            server = null;
        }));
        
        // Catch scanner input to exit
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner sc = new Scanner(System.in);
                
                while (true) {
                    String s = sc.nextLine();
                    if (s.equals("exit")) {
                        if (sc.nextLine().equals("admin")) {
                            close();
                            sc.close();
                            return;
                        } else {
                            System.err.println("Incorrect password.");
                        }
                    }
                }
            }
        }).start();
        
        try {
            // Setup here
            server = new Server(SERVER_PORT, PRINT_DEBUG);
            isOpen = true;
            if (PRINT_DEBUG) {
                System.out.println("\u001B[3;32m\u001B[1;35m" +
                        "To close server safely, type \"exit\" then \"admin\" in that order.\r\n" +
                        "If you force close the program, it may cause memory errors." + "\u001B[0;0m");
                System.out.println("Waiting for a connection.");
            }
            
            while (isOpen) {
                try {
                    server.accept();
                    
                    if (PRINT_DEBUG) {
                        System.out.println("New client accepted");
                    }
                } catch (IOException ioe) {
                    if (PRINT_DEBUG) {
                        System.err.println("Closing");
                    }
                    if (server != null && server.isClosed()) {
                        close();
                        return;
                    }
                    if (server == null) {
                        return;
                    }
                }
            }
            
            server.close();
            server = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Returns whether the ServerSocket is open
     *
     * @return whether the ServerSocket is open
     */
    public static boolean isOpen() {
        return isOpen;
    }
    
    /**
     * Closes the server and terminates this process
     */
    public static void close() {
        if (isOpen) {
            isOpen = false;
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            server = null;
        }
    }
    
}
