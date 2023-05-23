import javax.swing.*;
import java.awt.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * {@link Main}<br />
 * Main class; has main method for the client to run and connect to the server.
 *
 * @version 12/12/2022
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 */
public class Main {
    /**
     * Main method. Hosts a message client through a GUI.
     *
     * @param args the args
     */
    public static void main(String[] args) {
        // Prompt user for which server they want to use (or just the IP/port thereof)
        try {
            String host = "localhost";
            int port = 1200;
            Client client = new Client(host, port);
            
            runProgram(client);
            
            client.close();
        } catch (ConnectException connE) {
            JOptionPane.showMessageDialog(null,
                    "Connection refused. (Is server open and accessible?)");
        } catch (HeadlessException he) {
            System.err.println("JOptionPane not supported on this JVM.");
        } catch (SocketException | EOFException se) {
            try {
                // Try to close all JOptionPanes that are open
                Window[] windows = Window.getWindows();
                for (Window window : windows) {
                    if (window instanceof JDialog) {
                        JDialog dialog;
                        dialog = (JDialog) window;
                        if (dialog.getContentPane().getComponentCount() == 1
                                && dialog.getContentPane().getComponent(0) instanceof JOptionPane) {
                            dialog.dispose();
                        }
                    }
                }
                JOptionPane.showMessageDialog(null, "Server closed unexpectedly. Goodbye.");
            } catch (Exception e) {
                // ignored
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Runs the client-side of the messaging program with GUI; moved from main to keep things less cluttered
     *
     * @param client the {@link Client} object to use to get data from
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     * @throws HeadlessException      if a {@link HeadlessException} occurs
     */
    private static void runProgram(Client client) throws HeadlessException, ClassNotFoundException, IOException {
        // Welcome message
        int registerInt;
        String register = "";
        String[] options = {"Sign In", "Sign Up", "Exit"};
        
        // Run the program until the user exits
        do {
            System.gc();
            
            // Ask for registration
            do {
                registerInt = JOptionPane.showOptionDialog(null, "Please Sign Up or Sign In", "Login",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
                
                if (registerInt == JOptionPane.YES_OPTION) {
                    register = "sign in";
                } else if (registerInt == JOptionPane.NO_OPTION) {
                    register = "sign up";
                } else if (registerInt == JOptionPane.CANCEL_OPTION || registerInt == JOptionPane.CLOSED_OPTION) {
                    register = "exit";
                } else {
                    System.err.println("Unexpected registerInt : " + registerInt);
                }
            } while (!register.equalsIgnoreCase("sign up") && !register.equalsIgnoreCase("sign in") &&
                    !register.equalsIgnoreCase("exit"));
            
            User user;
            // Sign in
            if (register.equalsIgnoreCase("sign in")) {
                String email = JOptionPane.showInputDialog("Please enter your email:");
                if (email == null || !User.isValidEmailSyntax(email)) {
                    JOptionPane.showMessageDialog(null, "Invalid email syntax!");
                    continue;
                }
                String password = JOptionPane.showInputDialog("Please enter your password:");
                if (password == null) {
                    JOptionPane.showMessageDialog(null, "Incorrect password!");
                    continue;
                }
                // check if user exists
                if (client.userExists(email)) {
                    user = client.getUser(email);
                    if (user.signIn(password)) {
                        // Check if the user is Customer or Seller
                        if (user instanceof Customer) {
                            try {
                                doCustomerInteraction(client, user);
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else if (user instanceof Seller) {
                            try {
                                doSellerInteraction(client, user);
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Incorrect password");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "User does not exist.");
                }
            } else if (register.equalsIgnoreCase("sign up")) {
                String type = null;
                do {
                    String[] typesToChooseFrom = {"Customer", "Seller"};
                    int typeInt = JOptionPane.showOptionDialog(null, "Choose Type", "Choose Your Type of User",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, typesToChooseFrom, null);
                    if (typeInt == -1) {
                        continue;
                    }
                    type = typesToChooseFrom[typeInt];
                    if (type == null || (!type.equalsIgnoreCase("Customer") && !type.equalsIgnoreCase("Seller"))) {
                        typeInt = JOptionPane.showOptionDialog(null, "Choose Type", "Choose Your Type of User",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                typesToChooseFrom, null);
                        type = typesToChooseFrom[typeInt];
                    }
                } while (type == null || (!type.equalsIgnoreCase("Customer") && !type.equalsIgnoreCase("Seller")));
                
                // Get all user info
                // name
                String name = JOptionPane.showInputDialog("Please enter your name:");
                if (name == null || !User.isValidName(name)) {
                    JOptionPane.showMessageDialog(null, "Name invalid!");
                    continue;
                }
                // email
                String email = JOptionPane.showInputDialog("Please enter your email (this cannot be changed later!):");
                if (email == null || client.userExists(email) || !User.isValidEmailSyntax(email)) {
                    JOptionPane.showMessageDialog(null, "Email invalid (may already be in use!) or invalid format!");
                    continue;
                }
                // password
                String password = JOptionPane.showInputDialog("Please enter your password:");
                if (password == null || password.equals("")) {
                    JOptionPane.showMessageDialog(null, "You must enter a password!");
                    continue;
                }
                
                // if the user is a seller, get the store name
                String storeName = null;
                if (type.equalsIgnoreCase("Seller")) {
                    storeName = JOptionPane.showInputDialog("Please enter your (first) store name:");
                    if (storeName == null || storeName.equals("")) {
                        JOptionPane.showMessageDialog(null, "Store name invalid!");
                        continue;
                    } else if (client.getSellerFromStoreName(storeName) != null) {
                        JOptionPane.showMessageDialog(null, "Store name already in use!");
                        continue;
                    }
                }
                
                // initialise and instantiate the user
                user = null;
                try {
                    if (type.equalsIgnoreCase("Customer")) {
                        user = client.createCustomer(name, email, password);
                    } else if (type.equalsIgnoreCase("Seller")) {
                        user = client.createSeller(name, email, password, storeName);
                    }
                } catch (IllegalArgumentException iae) {
                    JOptionPane.showMessageDialog(null, "Invalid value entered! Reason: " + iae.getMessage());
                    continue;
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                
                // Shouldn't ever happen, just in case.
                if (user == null) {
                    throw new IllegalArgumentException("Value still null!");
                }
                
                // If a seller, keep asking for new stores
                if (user instanceof Seller) {
                    int moreStores;
                    moreStores = JOptionPane.showConfirmDialog(null, "Would you like to enter any more stores?",
                            "Store Entry", JOptionPane.YES_NO_OPTION);
                    while (moreStores == JOptionPane.YES_OPTION) {
                        String newStore = JOptionPane.showInputDialog("What is the store name?");
                        if (newStore == null || newStore.equals("")) {
                            JOptionPane.showMessageDialog(null, "You must enter a value!");
                        } else if (client.getAllStoresAsString().contains(newStore)) {
                            JOptionPane.showMessageDialog(null, "A store with this name already exists!");
                        } else {
                            user = client.addStoreToSeller((Seller) user, newStore);
                            JOptionPane.showMessageDialog(null, "Store added");
                        }
                        moreStores = JOptionPane.showConfirmDialog(null, "Would you like to enter any more stores?",
                                "Store Entry", JOptionPane.YES_NO_OPTION);
                    }
                }
                
                // Run the relevant interaction
                try {
                    if (user instanceof Customer) {
                        doCustomerInteraction(client, user);
                    }
                    if (user instanceof Seller) {
                        doSellerInteraction(client, user);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(null, "Thank you for using our program!");
                register = "exit";
            }
        } while (!register.equals("exit"));
    }
    
    /**
     * Runs the customer interaction
     *
     * @param client communicates with the server
     * @param user   the user (Customer) related to this interaction
     * @throws IllegalArgumentException to break the parent loop
     */
    private static void doCustomerInteraction(Client client, User user) throws IOException, ClassNotFoundException {
        Customer customer;
        JOptionPane.showMessageDialog(null, "Welcome, " + user.getName() + "!");
        // Customers should be able to view a list of stores to select a message recipient
        String storeString = client.getAllStoresAsString();
        if (storeString.equals("")) {
            JOptionPane.showMessageDialog(null,
                    "No stores or sellers available right now; nothing to do.\r\nSigning out...");
            return;
        }
        
        boolean staySignedIn = true;
        while (staySignedIn) {
            // update: no, not quite, I think there's an issue with passing things by reference but whatever...
            user = client.getUser(user.getEmail());
            customer = (Customer) user;
            
            int choice = -1;
            while (choice == -1) {
                String[] options = {"View conversation", "Message a Store", "Block a Seller",
                        "Become invisible to a Seller", "View your dashboard", "Save conversation to file",
                        "Account Settings", "Exit"};
                
                choice = Arrays.stream(options).toList().indexOf(String.valueOf(JOptionPane.showInputDialog(
                        null, "Would you like to:", "Customer Menu", JOptionPane.PLAIN_MESSAGE,
                        null, options, options[0])));
                if (choice < 0 || choice > options.length - 1) {
                    choice = options.length - 1;
                }
            }
            
            if (choice == 0) {
                viewConversations(client, user);
            } else if (choice == 1) { // MESSAGE (customer -> seller)
                ArrayList<Seller> sellers = client.getAllSellers();
                
                for (Seller seller : sellers) {
                    if (seller.getInvisEmails().contains(customer.getEmail())) {
                        // storeString = storeString.replace(seller.getSellerStoresAsString(), "");
                        storeString = storeString.replace(
                                seller.getSellerStoresAsString() + System.lineSeparator(), "");
                    }
                }
                
                String[] storeSplit = storeString.split(System.lineSeparator());
                ArrayList<String> storeOptions = new ArrayList<>(Arrays.asList(storeSplit));
                Object[] storesOptionsAsArray = storeOptions.toArray();
                
                String store = (String) JOptionPane.showInputDialog(null, "Please select a store to message:",
                        "Customer Menu", JOptionPane.PLAIN_MESSAGE, null,
                        storesOptionsAsArray, storesOptionsAsArray[0]);
                Seller seller = client.getSellerFromStoreName(store);
                if (store == null) {
                    JOptionPane.showMessageDialog(null, "No store selected!", "Store Selection",
                            JOptionPane.ERROR_MESSAGE);
                } else if (storeString.contains(store)) {
                    try {
                        Conversation conversation = client.getConversationBetweenUsersWithStore(customer, seller, store);
                        if (client.getSellerFromStoreName(store).getBlockedEmails().contains(customer.getEmail())) {
                            JOptionPane.showMessageDialog(null,
                                    "Cannot send message," + seller.getEmail() + " has blocked you.",
                                    "Error Sending Message", JOptionPane.ERROR_MESSAGE);
                            continue;
                        }
                        if (conversation == null) {
                            conversation = client.createConversation(seller, store, customer, false);
                        }
                        
                        Object messageType = JOptionPane.showInputDialog(
                                null, "How would you like to send this message?",
                                "How would you like to send this message?", JOptionPane.QUESTION_MESSAGE, null,
                                new String[]{"Type message", "Send message from file"}, "Type message");
                        if (messageType == null) {
                            JOptionPane.showMessageDialog(null, "Message cancelled!");
                            
                        } else if (messageType.equals("Send message from file")) {
                            sendMessageFromFile(client, user, conversation);
                        } else {
                            String message = JOptionPane.showInputDialog(null, "Please enter your message:",
                                    "Message", JOptionPane.QUESTION_MESSAGE);
                            if (message == null) {
                                JOptionPane.showMessageDialog(null, "Message was not sent!", "Sent Message",
                                        JOptionPane.INFORMATION_MESSAGE);
                            } else if (client.addMessageToConversation(conversation, client.createMessage(
                                    customer.getEmail(), seller.getEmail(), true, true,
                                    message, System.currentTimeMillis(), conversation)) != null) {
                                JOptionPane.showMessageDialog(null, "Message sent!", "Sent Message",
                                        JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(null, "Sending Error", "Error Sending Message",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } catch (NullPointerException npe) {
                        JOptionPane.showMessageDialog(null, "Error finding Seller associated with store " + store);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Store Error", "Not a Valid Store!",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else if (choice == 2) { // BLOCK
                String emailToBlock = JOptionPane.showInputDialog(null,
                        "Please enter the email of the user you would like to block: ", "Email Prompt",
                        JOptionPane.QUESTION_MESSAGE);
                if (emailToBlock == null) {
                    JOptionPane.showMessageDialog(null, "No email selected.");
                } else if (client.getUser(emailToBlock) != null) {
                    client.userBlocksUser(user, client.getUser(emailToBlock));
                    JOptionPane.showMessageDialog(null, "Operation Complete", "Operation Status",
                            JOptionPane.PLAIN_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Not a Valid User", "Operation Status",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else if (choice == 3) { // INVISIBLE
                String emailToVanishFrom = JOptionPane.showInputDialog(null,
                        "Please enter the email of the user you would like to become invisible to:", "Email Prompt",
                        JOptionPane.QUESTION_MESSAGE);
                if (emailToVanishFrom == null) {
                    JOptionPane.showMessageDialog(null, "No email selected.");
                } else if (client.getUser(emailToVanishFrom) != null) {
                    client.userInvisibleToUser(user, client.getUser(emailToVanishFrom));
                    JOptionPane.showMessageDialog(null, "Operation complete");
                } else {
                    JOptionPane.showMessageDialog(null, "Not a valid user!");
                }
            } else if (choice == 4) { // VIEW DASHBOARD
                String[] options = {"Ascending", "Descending"};
                int sortOption;
                sortOption = JOptionPane.showOptionDialog(null, "How would you like to sort your dashboard? ",
                        "Dashboard Prompt", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        options, options[0]);
                
                if (sortOption == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(null, customer.viewDashboardCustomer("Ascending"),
                            "Sorted Dashboard", JOptionPane.PLAIN_MESSAGE);
                } else if (sortOption == JOptionPane.NO_OPTION) {
                    JOptionPane.showMessageDialog(null, customer.viewDashboardCustomer("Descending"),
                            "Sorted Dashboard", JOptionPane.PLAIN_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Chose not to view dashboard");
                }
            } else if (choice == 5) {
                doSaveConversation(client, user);
            } else if (choice == 6) {
                if (doSettingsInteraction(client, user)) {
                    return;
                }
            } else {
                staySignedIn = false;
            }
        }
    }
    
    /**
     * Chooses a {@link File} to import and message
     *
     * @param client       the {@link Client} with the data
     * @param user         the {@link User} to message from
     * @param conversation the {@link Conversation} to message in
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private static void sendMessageFromFile(Client client, User user, Conversation conversation)
            throws IOException, ClassNotFoundException {
        String filepath;
        File f;
        do {
            JFileChooser fc = new JFileChooser();
            
            fc.setDialogTitle("Choose Message File");
            fc.showOpenDialog(null);
            
            f = fc.getSelectedFile();
            if (f != null) {
                filepath = f.getAbsolutePath();
            } else {
                break;
            }
            
            if (!(filepath.substring(filepath.lastIndexOf('.')).equals(".txt"))) {
                JOptionPane.showMessageDialog(null, "This is not a txt file!", "File Type Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            
        } while (!filepath.substring(filepath.lastIndexOf('.')).equals(".txt"));
        
        if (f == null) {
            JOptionPane.showMessageDialog(null, "No File Selected");
        } else if (f.exists()) {
            if (client.sendMessageFromFile(conversation, user, f) != null) {
                JOptionPane.showMessageDialog(null, "Message sent!");
            }
        } else {
            JOptionPane.showMessageDialog(null, "File not found!", "Finding File Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Runs the seller interaction
     *
     * @param client communicates with the server
     * @param user   the user (Seller) related to this interaction
     * @throws IllegalArgumentException to break the parent loop
     */
    private static void doSellerInteraction(Client client, User user) throws IOException, ClassNotFoundException {
        Seller seller;
        JOptionPane.showMessageDialog(null, "Welcome, " + user.getName() + "!");
        // Sellers should be able to view a list of customers to select an individual to message
        String customerList = client.listCustomers();
        if (customerList.equals("")) {
            JOptionPane.showMessageDialog(null, "There are no customers to interact with at this time.");
            return;
        }
        
        boolean staySignedIn = true;
        while (staySignedIn) {
            user = client.getUser(user.getEmail());
            seller = (Seller) user;
            
            int choice;
            String[] options = {"View conversations", "Message a Customer", "Block a Customer",
                    "Become invisible to a Customer", "View your dashboard", "Add a store",
                    "Save a conversation to file", "Account Settings", "Exit"};
            
            choice = Arrays.stream(options).toList().indexOf(String.valueOf(JOptionPane.showInputDialog(null,
                    "Would you like to:", "Seller Menu", JOptionPane.PLAIN_MESSAGE, null, options, options[0])));
            if (choice < 0 || choice > options.length - 1) {
                choice = options.length - 1;
            }
            
            if (choice == 0) {
                viewConversations(client, user);
            } else if (choice == 1) { // MESSAGE (seller -> customer)
                ArrayList<Customer> customers = client.getAllCustomers();
                
                for (Customer value : customers) {
                    if (value.getInvisEmails().contains(seller.getEmail())) {
                        customerList = customerList.replace(value.getEmail() + " : " + value.getName() +
                                System.lineSeparator(), "");
                    }
                }
                
                String customerEmail = JOptionPane.showInputDialog(null,
                        "Please select a customer to message (enter their email):" +
                                System.lineSeparator() + customerList);
                if (customerEmail != null && customerList.contains(customerEmail) && !customerEmail.equals("")) {
                    Customer customer = (Customer) client.getUser(customerEmail);
                    
                    seller = (Seller) client.getUser(seller.getEmail());
                    
                    String[] stores = seller.getStoreNames().toArray(new String[0]);
                    int storeChoice = JOptionPane.showOptionDialog(null,
                            "Choose a store to message this customer through", "Choose a Store",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, stores, 1);
                    String store;
                    
                    if (storeChoice == JOptionPane.CLOSED_OPTION) {
                        store = null;
                    } else {
                        store = stores[storeChoice];
                    }
                    
                    Conversation conversation = client.getConversationBetweenUsersWithStore(customer, seller, store);
                    
                    if (customer.getBlockedEmails().contains(seller.getEmail())) {
                        JOptionPane.showMessageDialog(null, "Cannot send message, " + customer.getEmail() +
                                " has blocked you.");
                        continue;
                    }
                    if (conversation == null) {
                        conversation = client.createConversation(seller, store, customer, false);
                    }
                    Object messageType = JOptionPane.showInputDialog(null, "How would you like to send this message?",
                            "How would you like to send this message?", JOptionPane.QUESTION_MESSAGE, null,
                            new String[]{"Type message", "Send message from file"}, "Type message");
                    if (messageType == null) { // if user presses cancel
                        JOptionPane.showMessageDialog(null, "Message cancelled!");
                        // continue;
                    } else if (messageType.equals("Send message from file")) {
                        sendMessageFromFile(client, user, conversation);
                    } else {
                        String message = JOptionPane.showInputDialog(null, "Please enter your message:");
                        if (message == null) {
                            JOptionPane.showMessageDialog(null, "Message was not sent!");
                        } else if (client.addMessageToConversation(conversation, client.createMessage(
                                seller.getEmail(), customer.getEmail(), true, true, message,
                                System.currentTimeMillis(), conversation)) != null) {
                            JOptionPane.showMessageDialog(null, "Message sent!");
                        } else {
                            JOptionPane.showMessageDialog(null, "Error sending message!");
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Customer " + customerEmail + " does not exist!");
                }
                
            } else if (choice == 2) { // BLOCK
                String emailToBlock = JOptionPane.showInputDialog(null,
                        "Please enter the email of the user you would like to block:" +
                                System.lineSeparator() + customerList);
                if (emailToBlock == null) {
                    JOptionPane.showMessageDialog(null, "No email selected.");
                } else if (client.getUser(emailToBlock) != null) {
                    client.userBlocksUser(user, client.getUser(emailToBlock));
                    JOptionPane.showMessageDialog(null, "Operation complete.");
                } else {
                    JOptionPane.showMessageDialog(null, "Not a valid user.");
                }
            } else if (choice == 3) { // INVISIBLE
                String emailToVanishFrom = JOptionPane.showInputDialog(null,
                        "Please enter the email of the user you would like to become invisible to:" +
                                System.lineSeparator() + customerList);
                if (emailToVanishFrom == null) {
                    JOptionPane.showMessageDialog(null, "No email selected.");
                } else if (client.getUser(emailToVanishFrom) != null) {
                    client.userInvisibleToUser(user, client.getUser(emailToVanishFrom));
                    JOptionPane.showMessageDialog(null, "Operation complete.");
                } else {
                    JOptionPane.showMessageDialog(null, "Not a valid user.");
                }
            } else if (choice == 4) { // VIEW DASHBOARD
                String[] sortOptions = {"Ascending", "Descending"};
                int sort;
                sort = JOptionPane.showOptionDialog(null, "How would you like to sort your dashboard? ",
                        "Dashboard Prompt", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        sortOptions, sortOptions[0]);
                
                if (sort == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(null, seller.viewDashboardSeller("Ascending"),
                            "Sorted Dashboard", JOptionPane.PLAIN_MESSAGE);
                } else if (sort == JOptionPane.NO_OPTION) {
                    JOptionPane.showMessageDialog(null, seller.viewDashboardSeller("Descending"),
                            "Sorted Dashboard", JOptionPane.PLAIN_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Chose not to view dashboard");
                }
            } else if (choice == 5) {
                String newStore;
                newStore = JOptionPane.showInputDialog(null, "What is the store name?");
                
                if (!(newStore == null || newStore.equals(""))) {
                    
                    if (client.getAllStoresAsString().contains(newStore)) {
                        JOptionPane.showMessageDialog(null, "A store with this name already exists!");
                    } else {
//                        seller.addStoreName(newStore);
                        client.addStoreToSeller(seller, newStore);
                        JOptionPane.showMessageDialog(null, "Store added");
                    }
                }
                
            } else if (choice == 6) {
                doSaveConversation(client, user);
            } else if (choice == 7) {
                if (doSettingsInteraction(client, user)) {
                    return;
                }
            } else {
                staySignedIn = false;
            }
        }
    }
    
    /**
     * View conversation
     *
     * @param client communicates with the server
     * @param user   the user to show conversations for
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private static void viewConversations(Client client, User user) throws ClassNotFoundException, IOException {
        ArrayList<Conversation> conversations = client.getConversationsWithUser(user);
        ArrayList<String> customerConversations = new ArrayList<>();
        ArrayList<String> sellerConversations = new ArrayList<>();
        if (conversations.size() > 0) {
            for (Conversation c : conversations) {
                String store = c.getStore();
                if (store != null && !store.equals("unknown store")) {
                    // Through a storefront
                    if (user instanceof Customer) {
                        customerConversations.add("Conversation with store \"" + store
                                + "\" owned by seller " + c.getSeller().getName() +
                                " (" + c.getSeller().getEmail() + ")");
                    } else if (user instanceof Seller) {
                        sellerConversations.add("Conversation through storefront \"" + store + "\" with customer "
                                + c.getCustomer().getName() + " (" + c.getCustomer().getEmail() + ")");
                    }
                } else {
                    // No store associated; direct message
                    if (user instanceof Customer) {
                        customerConversations.add("Conversation with seller "
                                + c.getSeller().getName() + " (" + c.getSeller().getEmail() + ")");
                    } else if (user instanceof Seller) {
                        sellerConversations.add("Conversation with customer "
                                + c.getCustomer().getName() + " (" + c.getCustomer().getEmail() + ")");
                    }
                }
            }
            
            int c_num;
            String[] choices = (user instanceof Customer ?
                    customerConversations.toArray(new String[0]) : sellerConversations.toArray(new String[0]));
            String conversationChoice = (String) JOptionPane.showInputDialog(null, "Choose conversation to view",
                    "Conversation", JOptionPane.QUESTION_MESSAGE, null,
                    choices, // Array of choices
                    choices[0]); // Initial choice
            c_num = Arrays.stream(choices).toList().indexOf(conversationChoice);
            
            if (conversationChoice == null || c_num < 0 || c_num > choices.length - 1) {
                JOptionPane.showMessageDialog(null, "No conversation selected.");
                return;
            }
            
            Conversation c = conversations.get(c_num);
            
            ArrayList<Message> messages = c.getMessages();
            StringBuilder sb = new StringBuilder();
            for (Message m : messages) {
                if ((m.getReceiverEmail().equals(user.getEmail()) && m.canReceiverView())
                        || (m.getSenderEmail().equals(user.getEmail()) && m.canSenderView())) {
                    User sender = (user.getEmail().equals(m.getSenderEmail()) ? user : c.otherUser(user));
                    sb.append("[").append(m.getTimeSent()).append("] ").append(sender.getName()).append(" : ").
                            append(m.getMessageContentFiltered(user)).append(System.lineSeparator());
                }
            }
            
            if (sb.length() == 0) {
                JOptionPane.showMessageDialog(null, "No messages to show.");
            } else {
                JOptionPane.showMessageDialog(null, sb.toString());
            }
            
            String[] yesNo = {"Yes", "No"};
            int typeInt = JOptionPane.showOptionDialog(null, "Would you like to reply or" +
                            " edit or delete one of your messages in this conversation?", "Alter Messages",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, yesNo, null);
            
            // If dialogue closed or "no" selected, go back
            if (typeInt == JOptionPane.CLOSED_OPTION || typeInt == 1) {
                return;
            }
            
            String[] actionOptions = {"Reply", "Edit", "Delete"};
            String messageAction = (String) JOptionPane.showInputDialog(null,
                    "Would you like to reply, edit, or delete?", "Action Options", JOptionPane.PLAIN_MESSAGE, null,
                    actionOptions, actionOptions[0]);
            if (messageAction.equalsIgnoreCase("reply")) {
                String message = JOptionPane.showInputDialog(null, "Please type your message below:");
                if (c.otherUser(user).getBlockedEmails().contains(user.getEmail())) {
                    JOptionPane.showMessageDialog(null, "Cannot reply, user has blocked you!");
                } else {
                    client.addMessageToConversation(c, client.createMessage(
                            user.getEmail(), c.otherUser(user).getEmail(), true, true, message,
                            System.currentTimeMillis(), c));
                    JOptionPane.showMessageDialog(null, "Reply sent!");
                }
            } else if (messageAction.equalsIgnoreCase("edit")) {
                String timestamp = JOptionPane.showInputDialog(null,
                        "Enter the timestamp of the message you'd like to edit (13 digit unix timestamp) " +
                                "(you must be the sender of this message!)\n" + sb);
                for (int i = 0; i < messages.size(); i++) {
                    try {
                        if (timestamp == null) {
                            JOptionPane.showMessageDialog(null, "No message selected to edit");
                            break;
                        } else if (timestamp.equals(String.valueOf(messages.get(i).getTimeSent()))) {
                            if (messages.get(i).getSenderEmail().equals(user.getEmail())) {
                                String newMessage = JOptionPane.showInputDialog(null,
                                        "Enter the message content:");
                                messages.get(i).setMessageContent(newMessage);
                                JOptionPane.showMessageDialog(null, "Message edited!");
                            } else {
                                JOptionPane.showMessageDialog(null, "You are not the sender of this message!");
                                JOptionPane.showMessageDialog(null, "Message was not edited");
                            }
                            break;
                        } else if (i == messages.size() - 1) {
                            JOptionPane.showMessageDialog(null, "This is not a valid timestamp!");
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(null, "Please enter a valid timestamp!");
                    }
                }
            } else if (messageAction.equalsIgnoreCase("delete")) {
                String timestamp = JOptionPane.showInputDialog(null,
                        "Enter the timestamp of the message you'd like to delete " +
                                "(you must be the sender of this message!)\n" + sb);
                for (int i = 0; i < messages.size(); i++) {
                    try {
                        if (timestamp == null) {
                            JOptionPane.showMessageDialog(null, "No message selected to delete");
                            break;
                        } else if (timestamp.equals(String.valueOf(messages.get(i).getTimeSent()))) {
                            if (messages.get(i).getSenderEmail().equals(user.getEmail())) {
                                messages.get(i).setCanSenderView(false);
                                JOptionPane.showMessageDialog(null, "Message deleted!");
                            } else {
                                JOptionPane.showMessageDialog(null, "You are not the sender of this message!");
                                JOptionPane.showMessageDialog(null, "Delete failed");
                            }
                            break;
                        } else if (i == messages.size() - 1) {
                            JOptionPane.showMessageDialog(null, "That is not a valid timestamp!");
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(null, "Please enter a valid timestamp");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "Not a recognised command");
            }
        } else {
            JOptionPane.showMessageDialog(null, "No conversations to view at this time!");
        }
    }
    
    /**
     * Runs the interaction for a {@link User} to save a {@link Conversation}
     *
     * @param client the {@link Client} carrying the data
     * @param user   the {@link User} to interact with
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private static void doSaveConversation(Client client, User user) throws IOException, ClassNotFoundException {
        ArrayList<Conversation> conversations = client.getConversationsWithUser(user);
        ArrayList<String> customerConversations = new ArrayList<>();
        ArrayList<String> sellerConversations = new ArrayList<>();
        
        if (conversations.size() == 0) {
            JOptionPane.showMessageDialog(null, "No conversations to save!");
            return;
        }
        
        for (Conversation c : conversations) {
            String store = c.getStore();
            if (store != null && !store.equals("unknown store")) {
                // Through a storefront
                if (user instanceof Customer) {
                    customerConversations.add("Conversation with store \"" + store
                            + "\" owned by seller " + c.getSeller().getName() + " (" + c.getSeller().getEmail() + ")");
                } else if (user instanceof Seller) {
                    sellerConversations.add("Conversation through storefront \"" + store + "\" with customer "
                            + c.getCustomer().getName() + " (" + c.getCustomer().getEmail() + ")");
                }
            } else {
                // No store associated; direct message
                if (user instanceof Customer) {
                    customerConversations.add("Conversation with seller "
                            + c.getSeller().getName() + " (" + c.getSeller().getEmail() + ")");
                } else if (user instanceof Seller) {
                    sellerConversations.add("Conversation with customer "
                            + c.getCustomer().getName() + " (" + c.getCustomer().getEmail() + ")");
                }
            }
        }
        
        int c_num;
        String[] choices = (user instanceof Customer ?
                customerConversations.toArray(new String[0]) : sellerConversations.toArray(new String[0]));
        String conversationChoice = (String) JOptionPane.showInputDialog(null, "Choose conversation to view",
                "Conversation", JOptionPane.QUESTION_MESSAGE, null,
                choices, // Array of choices
                choices[0]); // Initial choice
        c_num = Arrays.stream(choices).toList().indexOf(conversationChoice);
        
        if (conversationChoice == null || c_num < 0 || c_num > choices.length - 1) {
            JOptionPane.showMessageDialog(null, "No conversation selected.");
            return;
        }
        
        String filename = JOptionPane.showInputDialog(null, "Where would you like to save this file?");
        
        if (filename == null) {
            JOptionPane.showMessageDialog(null, "No file selected");
            return;
        }
        conversations.get(c_num).saveToCSVFile(filename, user);
        
        JOptionPane.showMessageDialog(null, "Success!");
    }
    
    /**
     * Runs the settings menu interaction for the given {@link User} with the {@link Client} having access to the data
     *
     * @param client the {@link Client} with the data
     * @param user   the {@link User} to run the interaction for
     * @return whether the {@link User} has deleted their account
     * @throws IOException            if an {@link IOException} occurs
     * @throws ClassNotFoundException if a {@link ClassNotFoundException} occurs
     */
    private static boolean doSettingsInteraction(Client client, User user) throws IOException, ClassNotFoundException {
        
        String[] settingChoices = {"Change Name", "Change Password", "Delete Account"};
        
        int settingChoice = JOptionPane.showOptionDialog(null, "Choose which setting you'd like to change:",
                "Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                settingChoices, settingChoices[0]);
        
        // ignore case -1
        if (settingChoice == 0) {
            String newName = JOptionPane.showInputDialog(null, "What would you like to change your name to?");
            
            if (newName == null || !User.isValidName(newName)) {
                JOptionPane.showMessageDialog(null, "Must enter value to change name.");
                return false;
            }
            
            client.setUserName(user, newName);
        } else if (settingChoice == 1) {
            String current = JOptionPane.showInputDialog(null, "Please enter your current password:");
            
            if (current == null || !user.signIn(current)) {
                JOptionPane.showMessageDialog(null, "Incorrect password!");
                return false;
            }
            
            String newPass = JOptionPane.showInputDialog(null, "Please enter your new password:");
            
            if (newPass == null || newPass.equals("")) {
                JOptionPane.showMessageDialog(null, "Invalid password! (Old password saved)");
                return false;
            }
            
            client.setUserPass(user, newPass);
            // again don't like this, but I think there's an issue with references being passed incorrectly by java
            // the ObjectStreams here
            user.setPassword(newPass);
            JOptionPane.showMessageDialog(null, "Updated successfully!");
        } else if (settingChoice == 2) {
            int sure = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete your account?",
                    "Are you sure?", JOptionPane.YES_NO_OPTION);
            
            if (sure != JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(null, "Operation cancelled.");
                return false;
            }
            
            String current = JOptionPane.showInputDialog(null, "Please enter your current password to confirm:");
            
            if (current == null || !user.signIn(current)) {
                JOptionPane.showMessageDialog(null, "Incorrect password! Operation cancelled.");
                return false;
            }
            
            client.deleteUser(user);
            JOptionPane.showMessageDialog(null, "Goodbye!");
            return true;
        }
        
        return false;
    }
    
}
