import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link Server}<br />
 * This class extends and acts as a {@link ServerSocket} with some helper methods for communicating in between message
 * clients and the server.
 *
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 * @version 12/12/2022
 */
public class Server extends ServerSocket {
    /**
     * The delimiter used to separate data types when saving files
     */
    public static final String DELIMITER;
    public static final String DELIMITER_REPLACEMENT;
    public static final String FILE_LOC;
    private final static String inputCountErrorMessage;
    private static ArrayList<String> saveFileComments;
    /**
     * A (probably temporary) boolean used to ensure that no more than one {@link Server} is created
     */
    private static boolean hasBeenCreated;
    
    
    static {
        DELIMITER = ";;";
        DELIMITER_REPLACEMENT = "\\\\;\\\\;\\\\";
        FILE_LOC = "src/data/";
        
        inputCountErrorMessage = "[%d] Operation `%s` expected %d Objects but got %d instead";
        hasBeenCreated = false;
    }
    
    ///// IMPORTED FROM MAIN /////
    private final ArrayList<Socket> clients;
    private final ArrayList<Thread> threads;
    private final boolean printDebug;
    private int threadCountCreated;
    
    
    /**
     * A {@link Server} constructor
     *
     * @param desiredPort the port to bind this server to
     * @throws IOException from ServerSocket(int port)
     */
    public Server(int desiredPort) throws IOException {
        this(desiredPort, false);
    }
    
    /**
     * A {@link Server} constructor
     *
     * @param desiredPort the port to bind this server to
     * @param printDebug  whether to print debug info
     * @throws IOException from ServerSocket(int port)
     */
    public Server(int desiredPort, boolean printDebug) throws IOException {
        super(desiredPort);
        
        this.clients = new ArrayList<>();
        this.threads = new ArrayList<>();
        threadCountCreated = 0;
        
        if (hasBeenCreated) {
            System.err.println("MULTIPLE SERVERS CREATED -- WARNING: NOT INTENDED USE!!!");
        } else {
            loadFromMemory();
        }
        
        this.printDebug = printDebug;
        
        hasBeenCreated = true;
    }
    
    /**
     * Reads the previous program data from memory. <em>Run only once</em>
     */
    private synchronized static void loadFromMemory() {
        saveFileComments = new ArrayList<>();
        // load all users (first)
        // load all conversations (ids first then grab each)
        try {
            File usersFile = new File(FILE_LOC + "users.ssv");
            if (!usersFile.exists()) {
                // This is fine, just break out of the try
                throw new InstantiationException("catch me wheeeeeee");
            }
            BufferedReader usersReader = new BufferedReader(new FileReader(usersFile));
            
            String userLine;
            while ((userLine = usersReader.readLine()) != null) {
                if (!userLine.equals("")) {
                    if (userLine.startsWith(";")) {
                        saveFileComments.add(userLine);
                        continue;
                    }
                    // Automatically puts it into User.USERS, so we don't need to catch it for anything.
                    try {
                        User.rebuildUser(userLine);
                    } catch (IllegalArgumentException iae) {
                        System.err.println("error rebuilding line: \"" + userLine + "\"");
                    }
                }
            }
            
            usersReader.close();
            
            File conversationListFile = new File(FILE_LOC + "conversation_list.ssv");
            BufferedReader conversationListReader = new BufferedReader(new FileReader(conversationListFile));
            
            HashSet<String> conversationIds = new HashSet<>();
            String conversationListLine;
            while ((conversationListLine = conversationListReader.readLine()) != null) {
                if (!conversationListLine.equals("")) {
                    conversationIds.add(conversationListLine);
                }
            }
            
            conversationListReader.close();
            
            for (String conversationId : conversationIds) {
                try {
                    File conversationFile = new File(FILE_LOC + conversationId + ".ssv");
                    BufferedReader conversationReader = new BufferedReader(new FileReader(conversationFile));
                    
                    String conversationLine = conversationReader.readLine();
                    if (conversationLine == null) {
                        continue;
                    }
                    String[] metaSplit = conversationLine.split(DELIMITER);
                    
                    // seller, store, customer, disappearing
                    // id, storeName, id, boolean
                    if (metaSplit.length != 4) {
                        System.err.println("Error parsing metadata for file " + conversationId + ".ssv");
                        continue;
                    }
                    String sellerId = metaSplit[0];
                    Seller seller = (Seller) User.getUser(sellerId);
                    String storeName = parse(metaSplit[1]);
                    if (storeName.equals("")) {
                        storeName = null;
                    }
                    String customerId = parse(metaSplit[2]);
                    Customer customer = (Customer) User.getUser(customerId);
                    boolean isDisappearing = Boolean.parseBoolean(parse(metaSplit[3]));
                    
                    // causes errors I don't have time to deal with.
                    // If being loaded for a second time, skip
                    Conversation c;
                    try {
                        c = new Conversation(seller, storeName, customer, isDisappearing);
                    } catch (ClassCastException cce) {
                        System.err.println("ClassCastException caught; should have worked???");
                        cce.printStackTrace();
                        continue;
                    }
                    
                    while ((conversationLine = conversationReader.readLine()) != null) {
                        String[] dataSplit = conversationLine.split(DELIMITER);
                        // Format:
                        // timeSent;senderType{CUSTOMER,SELLER};canSenderView;canReceiverView;messageContent
                        if (dataSplit.length != 5) {
                            System.err.println("Error parsing line \"" + conversationLine + "\".");
                            continue;
                        }
                        long timeSent = Long.parseLong(dataSplit[0]);
                        boolean canSenderView = Boolean.parseBoolean(dataSplit[2]);
                        boolean canReceiverView = Boolean.parseBoolean(dataSplit[3]);
                        String messageContent = dataSplit[4];
                        
                        // timeSent, senderId, receiverId, canSenderView, canReceiverView, messageContent
                        
                        // Represents who the SENDER is
                        if (dataSplit[1].equals("CUSTOMER")) {
                            c.addMessage(new Message(customerId, sellerId, canSenderView, canReceiverView,
                                    messageContent, timeSent));
                        } else if (dataSplit[1].equals("SELLER")) {
                            c.addMessage(new Message(sellerId, customerId, canSenderView, canReceiverView,
                                    messageContent, timeSent));
                        } else {
                            System.err.println("What is type \"" + dataSplit[1] +
                                    "\"? (Error in parsing message line.)");
                        }
                    }
                    
                    conversationReader.close();
                } catch (FileNotFoundException fnfe) {
                    System.err.printf("Conversation %s not found%n", conversationId);
                } catch (NullPointerException npe) {
                    System.err.printf("Conversation %s could not load all resources (check users.ssv intact).%n",
                            conversationId);
                }
            }
        } catch (IOException ioe) {
            System.err.println("Error reading data!");
            ioe.printStackTrace();
        } catch (InstantiationException ie) {
            // ignored
        }
    }
    
    /**
     * Saves the current program data to memory.
     */
    private synchronized static void saveToMemory() {
        if (saveFileComments == null) {
            saveFileComments = new ArrayList<>();
        }
        
        File dir = new File("src/data/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // save all users
        // save all conversations
        // I know .ssv isn't really a thing. Too bad.
        try {
            File usersFile = new File(FILE_LOC + "users.ssv");
            BufferedWriter usersWriter = new BufferedWriter(new FileWriter(usersFile, false));
            usersWriter.write("");
            for (String comment : saveFileComments) {
                usersWriter.append(comment).append(System.lineSeparator());
            }
            Set<String> emails = User.getUserEmails();
            for (String email : emails) {
                usersWriter.append(User.getUser(email).toFileLine()).append(System.lineSeparator());
            }
            usersWriter.flush();
            usersWriter.close();
            
            HashSet<String> usedConversationNames = new HashSet<>();
            for (Conversation c : MainListOfConversations.getMainListOfConversations()) {
                String name = hashStrings(c.getCustomer().getEmail(), c.getSeller().getEmail(), c.getStore());
                if (usedConversationNames.contains(name)) {
                    // duplicate has found
                    continue;
                }
                // If User deleted, save the file but don't save let it be loaded to prevent errors and dead-reading
                if (User.getUser(c.getCustomer().getEmail()) != null &&
                        User.getUser(c.getSeller().getEmail()) != null) {
                    usedConversationNames.add(name);
                }
                
                File conversationFile = new File(FILE_LOC + name + ".ssv");
                BufferedWriter conversationFileWriter = new BufferedWriter(
                        new FileWriter(conversationFile, false));
                
                // seller, store, customer, disappearing
                conversationFileWriter.write(
                        clean(c.getSeller().getEmail()) + DELIMITER
                                + clean(c.getStore()) + DELIMITER
                                + clean(c.getCustomer().getEmail()) + DELIMITER
                                + c.isDisappearing() + System.lineSeparator()
                );
                
                for (Message m : c.getMessages()) {
                    // write a line here ;_;
                    // timeSent, senderId, receiverId, canSenderView, canReceiverView, messageContent
                    // timeSent;senderType{CUSTOMER,SELLER};canSenderView;canReceiverView;messageContent
                    User sender = (c.getCustomer().getEmail().equals(m.getSenderEmail())
                            ? c.getCustomer() : c.getSeller());
                    conversationFileWriter.append(String.valueOf(m.getTimeSent())).append(DELIMITER)
                            .append(sender instanceof Customer ? "CUSTOMER" : "SELLER").append(DELIMITER)
                            .append(String.valueOf(m.canSenderView())).append(DELIMITER)
                            .append(String.valueOf(m.canReceiverView())).append(DELIMITER)
                            .append(clean(m.getMessageContent())).append(System.lineSeparator());
                }
                conversationFileWriter.flush();
                conversationFileWriter.close();
            }
            
            File conversationListFile = new File(FILE_LOC + "conversation_list.ssv");
            BufferedWriter conversationListWriter =
                    new BufferedWriter(new FileWriter(conversationListFile, false));
            
            for (String conversationName : usedConversationNames) {
                conversationListWriter.append(conversationName).append(System.lineSeparator());
            }
            
            conversationListWriter.flush();
            conversationListWriter.close();
        } catch (IOException e) {
            System.err.println("Error saving data! Details: ");
            e.printStackTrace();
        }
    }
    
    /**
     * Cleans the given String for saving to a file
     *
     * @param toClean the String to clean
     * @return the cleaned String
     */
    private static String clean(String toClean) {
        return toClean == null ? "" : toClean.replaceAll(DELIMITER, DELIMITER_REPLACEMENT)
                .replaceAll("[\r\n]+", "\\\\n");
    }
    
    /**
     * Parses the given String from the file format
     *
     * @param toParse the String to parse
     * @return the parsed String
     */
    private static String parse(String toParse) {
        return toParse.replaceAll(DELIMITER_REPLACEMENT, DELIMITER)
                .replaceAll("\\\\n", System.lineSeparator());
    }
    
    /**
     * Hashes two Strings together
     *
     * @param string1 the first String
     * @param string2 the second String
     * @return the hashed value
     */
    private static String hashStrings(String string1, String string2, String string3) {
        if (string1 == null) {
            string1 = "";
        }
        if (string2 == null) {
            string2 = "";
        }
        if (string3 == null) {
            string3 = "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(
                    (string1 + "_" + string2 + "_" + string3).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            // Shouldn't ever happen
            nsae.printStackTrace();
            return null;
        }
    }
    
    /**
     * Overrides the ServerSocket method to accept a connection and spawn a Thread controlling it
     *
     * @return the Socket connected
     * @throws IOException from ServerSocket.accept();
     */
    @Override
    public Socket accept() throws IOException {
        // Accept the connection as normal, then process it afterwards. Store this new client to the socket
        Socket socket = super.accept();
        
        // Add this socket to the list
        clients.add(socket);
        
        // Increase total thread count and store it to threadNum to pass to the Thread
        final int threadNum = ++threadCountCreated;
        
        // Create and store a new Thread to keep this client relationship alive.
        // Using Runnable instead of lambda for backwards compatibility.
        threads.add(new Thread(new Runnable() {
            @Override
            public void run() {
                // Surrounding to try to catch some issues (just throws them back for now)
                try {
                    if (printDebug) {
                        // sout an update, can remove later
                        System.out.printf("[%d] In thread %1$d (client #%1$d) on port %d%n",
                                threadNum, socket.getPort());
                    }
                    // Make a DataBundle to store all the data and easily pass it around (and prevent duplicate OOS's)
                    final DataBundle dataBundle = new DataBundle(threadCountCreated, socket);
                    
                    // Keep repeating these operations until the socket closes or crashes
                    while (socket.isConnected()) {
                        if (printDebug) {
                            System.out.printf("[%d] Waiting for next request from client #%1$d%n",
                                    dataBundle.threadNum);
                        }
                        // Store the new version of the data and print an update (or remove updates later)
                        int operationOrdinal = dataBundle.is.read();
                        // Socket closed, terminate this thread
                        if (operationOrdinal == -1) {
                            throw new SocketException("Catch this!");
                        }
                        dataBundle.operation = Operation.values()[operationOrdinal];
                        dataBundle.numObjects = dataBundle.is.read();
                        if (printDebug) {
                            System.out.printf("[%d] Received operation %s with %d objects passed.%n",
                                    dataBundle.threadNum, dataBundle.operation.toString(), dataBundle.numObjects);
                        }
                        
                        // Depending on the operation, call the appropriate method
                        switch (dataBundle.operation) {
                            case Message -> doMessage(dataBundle);
                            case GetUser -> doGetUser(dataBundle);
                            case UserExists -> doUserExists(dataBundle);
                            case AllStoresAsString -> doGetAllStoresAsString(dataBundle);
                            case GetAllSellers -> doGetAllSellers(dataBundle);
                            case GetSellerFromStore -> doGetSellerFromStore(dataBundle);
                            case ListCustomers -> doListCustomers(dataBundle);
                            case GetAllCustomers -> doGetAllCustomers(dataBundle);
                            case GetConversationsWithUser -> doGetConversationsWithUser(dataBundle);
                            case SetMessageContent -> doSetMessageContent(dataBundle);
                            case AddMessageToConversation -> doAddMessageToConversation(dataBundle);
                            case UserBlocksUser -> doUserBlocksUser(dataBundle);
                            case UserInvisibleToUser -> doUserInvisibleToUser(dataBundle);
                            case GetConversationWithUsers -> doGetConversationWithUsers(dataBundle);
                            case CreateCustomer -> doCreateCustomer(dataBundle);
                            case CreateSeller -> doCreateSeller(dataBundle);
                            case CreateMessage -> doCreateMessage(dataBundle);
                            case CreateConversation -> doCreateConversation(dataBundle);
                            case SellerAddStore -> doSellerAddStore(dataBundle);
                            case GetConversationWithUsersWithStore -> doGetConversationWithUsersWithStore(dataBundle);
                            case SendMessageFromFile -> doSendMessageFromFile(dataBundle);
                            case SetUserName -> doSetUserName(dataBundle);
                            case SetUserPass -> doSetUserPass(dataBundle);
                            case DeleteUserAccount -> doDeleteUserAccount(dataBundle);
                            case Disconnect -> {
                                if (printDebug) {
                                    System.out.printf("[%d] Client #%1$d disconnected%n", threadNum);
                                }
                                return;
                            }
                            case Exit -> doExit(dataBundle);
                            default -> {
                                // If the Operation is not recognised, clean the inputs, and print an error
                                for (int i = 0; i < dataBundle.numObjects; i++) {
                                    dataBundle.ois.readObject();
                                }
                                if (printDebug) {
                                    System.err.printf(
                                            "[%d] Operation %s not recognised/implemented (passed %d Objects)%n",
                                            dataBundle.threadNum, dataBundle.operation.toString(),
                                            dataBundle.numObjects);
                                }
                            }
                        }
                    }
                } catch (SocketException se) {
                    // Almost certainly an issue with the socket disconnecting. Try to close it just in case it wasn't.
                    try {
                        socket.close();
                        saveToMemory();
                        return;
                    } catch (IOException e) {
                        // Ignored.
                    }
                    if (printDebug) {
                        System.err.printf("[%d] Client #%1$d disconnected unexpectedly. Thread %1$d terminated.%n",
                                threadNum);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
        
        // Start the new Thread
        threads.get(threads.size() - 1).start();
        
        // Return the socket connected to this client back
        return socket;
    }
    
    /**
     * Prints a message sent to the server
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doMessage(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 1) {
            if (data.numObjects == 0) {
                System.err.printf("[%d] Client #%1$d messaged nothing.%n", data.threadNum);
                return;
            }
        }
        
        try {
            System.out.printf("\u001B[34m" + "[%d] Client #%1$d messaged \"%s\".%n" + "\u001B[0m",
                    data.threadNum, data.ois.readObject());
        } catch (IllegalArgumentException iae) {
            System.out.printf("[%d] Client #%1$d messaged \"%s\".%n", data.threadNum, data.ois.readObject());
        }
        
        dumpOISStream(data, 1);
    }
    
    /**
     * Tries to find the {@link User} by the passed email and writes it to data.oos.
     * Returns null if no such {@link User} is found
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doGetUser(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 1) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 1, data.numObjects);
            if (data.numObjects == 0) {
                data.oos.writeObject(null);
                data.oos.flush();
                return;
            }
        }
        
        User u = User.getUser(data.ois.readObject().toString());
        data.oos.writeObject(u);
        data.oos.flush();
        
        dumpOISStream(data, 1);
    }
    
    /**
     * Closes the server.
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doExit(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 0) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 1, data.numObjects);
            // Don't need to dump stream because it's being closed anyhow
        }
        
        data.socket.close();
        saveToMemory();
        this.close();
    }
    
    /**
     * Finds if a {@link User} with the given ({@link String}) email exists and writes this result as a boolean to
     * data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doUserExists(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 1) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 1, data.numObjects);
            if (data.numObjects == 0) {
                data.oos.writeObject(null);
                data.oos.flush();
                return;
            }
        }
        
        data.oos.writeObject(User.userExists((String) data.ois.readObject()));
        data.oos.flush();
        
        dumpOISStream(data, 1);
    }
    
    /**
     * Gets all stores as a {@link String} and writes this result to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doGetAllStoresAsString(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 0) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 0, data.numObjects);
        }
        
        data.oos.writeObject(Seller.getAllStoresAsString());
        data.oos.flush();
        
        dumpOISStream(data, 0);
    }
    
    /**
     * Gets all {@link Seller}s and writes this result to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doGetAllSellers(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 0) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 0, data.numObjects);
        }
        
        data.oos.writeObject(Seller.getAllSellers());
        data.oos.flush();
        
        dumpOISStream(data, 0);
    }
    
    /**
     * Gets the {@link Seller} from the given ({@link String}) store and writes this result to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doGetSellerFromStore(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 1) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 1, data.numObjects);
            if (data.numObjects == 0) {
                data.oos.writeObject(null);
                data.oos.flush();
                return;
            }
        }
        
        data.oos.writeObject(Seller.getSellerFromStore((String) data.ois.readObject()));
        data.oos.flush();
        
        dumpOISStream(data, 1);
    }
    
    /**
     * Lists all {@link Customer}s as a {@link String} and writes this result to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doListCustomers(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 0) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 0, data.numObjects);
        }
        
        data.oos.writeObject(User.listCustomers());
        data.oos.flush();
        
        dumpOISStream(data, 0);
    }
    
    /**
     * Gets all {@link Customer}s in an {@link ArrayList} and and writes this result to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doGetAllCustomers(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 0) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 0, data.numObjects);
        }
        
        data.oos.writeObject(User.getAllCustomers());
        data.oos.flush();
        
        dumpOISStream(data, 0);
    }
    
    /**
     * Gets all {@link Conversation}s as an {@link ArrayList} and writes this result to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doGetConversationsWithUser(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 1) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 1, data.numObjects);
            if (data.numObjects == 0) {
                data.oos.writeObject(null);
                data.oos.flush();
                return;
            }
        }
        
        data.oos.writeObject(MainListOfConversations.getConversationsWithUser((User) data.ois.readObject()));
        data.oos.flush();
        
        dumpOISStream(data, 1);
    }
    
    /**
     * Sets the {@link Message} content to a {@link String} passed and writes the new version of the passed
     * {@link Message} to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doSetMessageContent(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 2) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 2, data.numObjects);
            if (data.numObjects < 2) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        Message messageToEdit = (Message) data.ois.readObject();
        String contentToSet = (String) data.ois.readObject();
        
        Conversation toFind = messageToEdit.getParent();
        Conversation found = MainListOfConversations.getConversation(toFind);
        
        Message actualMessage = found.getMessageByTimestamp(messageToEdit.getTimeSent());
        
        if (actualMessage == null) {
            System.err.printf("[%d] Failed in doSetMessageContent; actualMessage is null, found: %s%n",
                    data.threadNum, found);
        } else {
            messageToEdit.setMessageContent(contentToSet);
        }
        
        data.oos.writeObject(actualMessage);
        data.oos.flush();
        
        dumpOISStream(data, 2);
    }
    
    /**
     * Adds a {@link Message} to a {@link Conversation} and writes the modified {@link Conversation} to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doAddMessageToConversation(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 2) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 2, data.numObjects);
            if (data.numObjects < 2) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        Conversation conversationToEdit = (Conversation) data.ois.readObject();
        Message messageToAdd = (Message) data.ois.readObject();
        
        Conversation found = MainListOfConversations.getConversation(conversationToEdit);
        
        found.addMessage(messageToAdd);
        
        data.oos.writeObject(found);
        data.oos.flush();
        dumpOISStream(data, 2);
    }
    
    /**
     * Adds a blocked {@link User} to the first {@link User} passed (blocks the second {@link User} passed).
     * Writes the updated {@link User} to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doUserBlocksUser(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 2) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 2, data.numObjects);
            if (data.numObjects < 2) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        User userToEdit = (User) data.ois.readObject();
        User userToBlock = (User) data.ois.readObject();
        User toEdit = User.getUser(userToEdit.getEmail());
        
        if (!toEdit.getBlockedEmails().contains(userToBlock.getEmail())) {
            toEdit.blockUser(userToBlock.getEmail());
        }
        
        data.oos.writeObject(toEdit);
        data.oos.flush();
        
        dumpOISStream(data, 2);
    }
    
    /**
     * Makes the first passed {@link User} become invisible to the second {@link User} passed.
     * Writes the updated {@link User} to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doUserInvisibleToUser(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 2) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 2, data.numObjects);
            if (data.numObjects < 2) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        User userToEdit = (User) data.ois.readObject();
        User userToHideFrom = (User) data.ois.readObject();
        User toEdit = User.getUser(userToEdit.getEmail());
        
        if (!userToEdit.getInvisEmails().contains(userToHideFrom.getEmail())) {
            toEdit.becomeInvisibleToUser(userToHideFrom.getEmail());
        }
        
        data.oos.writeObject(toEdit);
        data.oos.flush();
        
        dumpOISStream(data, 2);
    }
    
    /**
     * Gets a {@link Conversation} with the two given {@link User}s and writes this result to data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doGetConversationWithUsers(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 2) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 2, data.numObjects);
            if (data.numObjects < 2) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        data.oos.writeObject(MainListOfConversations.getConversationWithUsers(
                (User) data.ois.readObject(), (User) data.ois.readObject()));
        data.oos.flush();
        
        dumpOISStream(data, 2);
    }
    
    /**
     * Creates and writes back a newly constructed {@link Customer} with the given data
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doCreateCustomer(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 4) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 4, data.numObjects);
            if (data.numObjects < 4) {
                data.oos.writeObject(null);
                data.oos.flush();
                return;
            }
        }
        
        Customer customer = null;
        try {
            customer = new Customer(
                    (String) data.ois.readObject(), (String) data.ois.readObject(), (String) data.ois.readObject(),
                    (HashMap<String, String>) data.ois.readObject());
        } catch (IllegalArgumentException iae) {
            if (printDebug) {
                System.err.printf("[%d] Illegal argument passed to doCreateCustomer.%n", data.threadNum);
            }
        }
        
        data.oos.writeObject(customer);
        
        dumpOISStream(data, 4);
    }
    
    /**
     * Creates and writes back a newly constructed {@link Customer} with the given data
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doCreateSeller(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 5) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 5, data.numObjects);
            if (data.numObjects < 5) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        Seller seller = null;
        try {
            seller = new Seller(
                    (String) data.ois.readObject(), (String) data.ois.readObject(), (String) data.ois.readObject(),
                    (HashMap<String, String>) data.ois.readObject(), (ArrayList<String>) data.ois.readObject()
            );
        } catch (IllegalArgumentException iae) {
            if (printDebug) {
                System.err.printf("[%d] Illegal argument passed to doCreateSeller.%n", data.threadNum);
            }
        }
        
        data.oos.writeObject(seller);
        
        dumpOISStream(data, 5);
    }
    
    /**
     * Creates and writes back a new {@link Message} with the passed data
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doCreateMessage(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 7) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 7, data.numObjects);
            if (data.numObjects < 7) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        Message m = new Message(
                (String) data.ois.readObject(), (String) data.ois.readObject(), (Boolean) data.ois.readObject(),
                (Boolean) data.ois.readObject(), (String) data.ois.readObject(), (Long) data.ois.readObject()
        );
        Conversation found = MainListOfConversations.getConversation((Conversation) data.ois.readObject());
        m.setParent(found);
        
        data.oos.writeObject(m);
        data.oos.flush();
        
        dumpOISStream(data, 7);
    }
    
    /**
     * Creates and writes back a new {@link Conversation} with the passed data
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doCreateConversation(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 4) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 4, data.numObjects);
            if (data.numObjects < 4) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        data.oos.writeObject(new Conversation(
                (Seller) data.ois.readObject(), (String) data.ois.readObject(), (Customer) data.ois.readObject(),
                (Boolean) data.ois.readObject()
        ));
        
        dumpOISStream(data, 4);
    }
    
    /**
     * Adds a store to a {@link Seller} and writes back the updated {@link Seller}
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doSellerAddStore(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 2) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 2, data.numObjects);
            if (data.numObjects < 2) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        Seller sellerToEdit = (Seller) data.ois.readObject();
        String storeName = (String) data.ois.readObject();
        Seller toEdit = (Seller) User.getUser(sellerToEdit.getEmail());
        
        toEdit.addStoreName(storeName);
        
        data.oos.writeObject(toEdit);
        data.oos.flush();
        
        dumpOISStream(data, 2);
    }
    
    /**
     * Gets the {@link Conversation} with the two given {@link User}s and the provided store then writes this result to
     * data.oos
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doGetConversationWithUsersWithStore(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 3) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 3, data.numObjects);
            if (data.numObjects < 3) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        data.oos.writeObject(MainListOfConversations.getConversationWithUsersWithStore(
                User.getUser(((User) data.ois.readObject()).getEmail()),
                User.getUser(((User) data.ois.readObject()).getEmail()),
                (String) data.ois.readObject()
        ));
        data.oos.flush();
        
        dumpOISStream(data, 3);
    }
    
    /**
     * Sends a message from the given {@link User} with the content in the passed {@link File} in the given
     * {@link Conversation} then writes back the updated {@link Conversation}
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doSendMessageFromFile(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 3) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 3, data.numObjects);
            if (data.numObjects < 3) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        Conversation conversation = (Conversation) data.ois.readObject();
        conversation = MainListOfConversations.getConversationWithUsersWithStore(conversation.getCustomer(),
                conversation.getSeller(), conversation.getStore());
        
        // Shouldn't ever happen
        if (conversation == null) {
            System.err.printf("[%d] Error finding conversation passed to doSendMessageFromFile%n", data.threadNum);
            data.oos.writeObject(null);
            data.oos.flush();
            
            dumpOISStream(data, 1);
            return;
        }
        
        conversation.sendMessageFromFile((User) data.ois.readObject(), (File) data.ois.readObject());
        
        data.oos.writeObject(conversation);
        data.oos.flush();
        
        dumpOISStream(data, 3);
    }
    
    /**
     * Sets the passed {@link User}'s name to the passed {@link String} and writes back the updated {@link User}
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doSetUserName(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 2) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 2, data.numObjects);
            if (data.numObjects < 2) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        User toEdit = User.getUser(((User) data.ois.readObject()).getEmail());
        toEdit.setName((String) data.ois.readObject());
        
        data.oos.writeObject(toEdit);
        data.oos.flush();
        
        dumpOISStream(data, 2);
    }
    
    /**
     * Sets the passed {@link User}'s password to the passed {@link String} and writes back the updated {@link User}
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doSetUserPass(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 2) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 2, data.numObjects);
            if (data.numObjects < 2) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        User toEdit = User.getUser(((User) data.ois.readObject()).getEmail());
        toEdit.setPassword((String) data.ois.readObject());
        
        data.oos.writeObject(toEdit);
        data.oos.flush();
        
        dumpOISStream(data, 2);
    }
    
    /**
     * Removes the passed {@link User} from the tracked data. Writes back null
     *
     * @param data the {@link DataBundle} containing the socket and thread's information
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void doDeleteUserAccount(DataBundle data) throws IOException, ClassNotFoundException {
        if (data.numObjects != 1) {
            System.err.printf(inputCountErrorMessage, data.threadNum, data.operation, 1, data.numObjects);
            if (data.numObjects < 1) {
                data.oos.writeObject(null);
                data.oos.flush();
                dumpOISStream(data, 0);
                return;
            }
        }
        
        User.removeUser(User.getUser(((User) data.ois.readObject()).getEmail()));
        
        data.oos.writeObject(null);
        data.oos.flush();
        
        dumpOISStream(data, 1);
    }
    
    /**
     * Reads (data.numObjects - consumed) objects from data.ois
     *
     * @param data     the DataBundle containing the number of {@link Object}s passed and the {@link ObjectInputStream}
     * @param consumed the number of {@link Object}s already consumed (read)
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private void dumpOISStream(DataBundle data, int consumed) throws IOException, ClassNotFoundException {
        if (data.numObjects > consumed) {
            for (int i = consumed; i < data.numObjects; i++) {
                data.ois.readObject();
            }
        }
    }
    
    /**
     * Closes this ServerSocket and all Socket connections it has
     *
     * @throws IOException if an I/O Exception occurs while closing any client Sockets or this ServerSocket
     */
    @Override
    public void close() throws IOException {
        saveToMemory();
        
        for (Thread t : threads) {
            if (t.isAlive()) {
                t.interrupt();
            }
        }
        for (Socket socket : clients) {
            socket.close();
        }
        
        super.close();
    }
    
    private static class DataBundle {
        final int threadNum;
        final Socket socket;
        final InputStream is;
        final ObjectInputStream ois;
        final OutputStream os;
        final ObjectOutputStream oos;
        Operation operation;
        int numObjects;
        
        DataBundle(int threadCountCreated, Socket socket) throws IOException {
            this.threadNum = threadCountCreated;
            this.socket = socket;
            
            this.is = socket.getInputStream();
            this.ois = new ObjectInputStream(this.is);
            this.os = socket.getOutputStream();
            this.oos = new ObjectOutputStream(this.os);
        }
        
    }
}
