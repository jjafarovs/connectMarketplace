import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * {@link Client}<br />
 * This class extends and acts as a Socket with some helper methods to interact with the messaging system
 *
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 * @version 12/12/2022
 */
public class Client extends Socket {
    private final ObjectInputStream ois;
    private final OutputStream os;
    private final ObjectOutputStream oos;
    
    /**
     * A {@link Client} constructor
     *
     * @param host the host to connect to
     * @param port the port to connect to
     * @throws IOException if any I/O Exceptions occur
     */
    public Client(String host, int port) throws IOException {
        super(host, port);
        
        this.os = getOutputStream();
        this.oos = new ObjectOutputStream(os);
        InputStream is = getInputStream();
        this.ois = new ObjectInputStream(is);
    }
    
    /**
     * Sends a set of {@link Object}s to the server that this {@link Client} is connected to with an {@link Operation}
     * to guide the server how to use the data provided
     *
     * @param operation the operation for the server to perform with this data
     * @param objects   the objects passed along with the Operation
     * @throws IOException if an I/O Exception occurs while sending the data to the server
     */
    private void sendToServer(Operation operation, Serializable... objects) throws IOException {
        os.write(operation.ordinal());
        os.write(objects.length);
        
        for (Serializable object : objects) {
            oos.writeObject(object);
        }
        oos.flush();
    }
    
    /**
     * Writes a message to the ServerSocket
     *
     * @param message the message to send
     */
    public void messageServer(String message) throws IOException {
        sendToServer(Operation.Message, message);
    }
    
    /**
     * Gets a user from the server
     *
     * @param userEmail the user's email
     * @return the User from the server
     * @throws IOException            if an I/O Exception occurs when sending or receiving data
     * @throws ClassNotFoundException the class was not found or could not be cast
     */
    public User getUser(String userEmail) throws IOException, ClassNotFoundException {
        sendToServer(Operation.GetUser, userEmail);
        Object o = ois.readObject();
        return (User) o;
    }
    
    /**
     * Gets a String of the customer list
     *
     * @return the String of the Customer list
     * @throws IOException            if an I/O Exception occurs when sending or receiving data
     * @throws ClassNotFoundException the class was not found or could not be cast
     */
    public String listCustomers() throws IOException, ClassNotFoundException {
        sendToServer(Operation.ListCustomers);
        Object o = ois.readObject();
        return (String) o;
    }
    
    /**
     * Gets the ArrayList of all customers
     *
     * @return the ArrayList of all customers
     * @throws IOException            if an I/O Exception occurs when sending or receiving data
     * @throws ClassNotFoundException the class was not found or could not be cast
     */
    public ArrayList<Customer> getAllCustomers() throws IOException, ClassNotFoundException {
        sendToServer(Operation.GetAllCustomers);
        Object o = ois.readObject();
        return (ArrayList<Customer>) o;
    }
    
    /**
     * Gets the {@link Conversation} with given {@link User}
     *
     * @param user the user's email
     * @return the ArrayList of the conversation with given user
     * @throws IOException            if an I/O Exception occurs when sending or receiving data
     * @throws ClassNotFoundException the class was not found or could not be cast
     */
    public ArrayList<Conversation> getConversationsWithUser(User user) throws IOException, ClassNotFoundException {
        sendToServer(Operation.GetConversationsWithUser, user);
        Object o = ois.readObject();
        return (ArrayList<Conversation>) o;
    }
    
    /**
     * Closes this {@link Socket}. Informs the server.
     *
     * @throws IOException if an I/O error occurs when closing this socket.
     */
    @Override
    public void close() throws IOException {
        if (isConnected() && os != null) {
            sendToServer(Operation.Disconnect);
        }
        super.close();
    }
    
    public Customer createCustomer(String name, String email, String password)
            throws IOException, ClassNotFoundException {
        return createCustomer(name, email, password, null);
    }
    
    public Customer createCustomer(String name, String email, String password, HashMap<String, String> blockedPhrases)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.CreateCustomer, name, email, password, blockedPhrases);
        
        return (Customer) ois.readObject();
    }
    
    public Seller createSeller(String name, String email, String password, String... storeNames)
            throws IOException, ClassNotFoundException {
        ArrayList<String> storeNamesArrList = new ArrayList<>(Arrays.asList(storeNames));
        
        return createSeller(name, email, password, null, storeNamesArrList);
    }
    

    public Seller createSeller(String name, String email, String password, HashMap<String, String> blockedPhrases,
                               ArrayList<String> storeNames) throws IOException, ClassNotFoundException {
        sendToServer(Operation.CreateSeller, name, email, password, blockedPhrases, storeNames);
        
        return (Seller) ois.readObject();
    }

    public Message createMessage(String senderEmail, String receiverEmail, boolean canSenderView,
                                 boolean canReceiverView, String messageContent, long timeSent, Conversation parent)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.CreateMessage, senderEmail, receiverEmail, canSenderView, canReceiverView,
                messageContent, timeSent, parent);
        
        return (Message) ois.readObject();
    }
    
    public Conversation createConversation(Seller seller, String store, Customer customer, boolean isDisappearing)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.CreateConversation, seller, store, customer, isDisappearing);
        
        return (Conversation) ois.readObject();
    }
    
    public boolean userExists(String email) throws IOException, ClassNotFoundException {
        sendToServer(Operation.UserExists, email);
        
        return (Boolean) ois.readObject();
    }
    
    String getAllStoresAsString() throws IOException, ClassNotFoundException {
        sendToServer(Operation.AllStoresAsString);
        
        return (String) ois.readObject();
    }
    
    public ArrayList<Seller> getAllSellers() throws IOException, ClassNotFoundException {
        sendToServer(Operation.GetAllSellers);
        
        return (ArrayList<Seller>) ois.readObject();
    }
    
    public Seller getSellerFromStoreName(String storeName) throws IOException, ClassNotFoundException {
        sendToServer(Operation.GetSellerFromStore, storeName);
        
        return (Seller) ois.readObject();
    }
    
    public Message setMessageContent(Message messageToEdit, String contentToSet)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.SetMessageContent, messageToEdit, contentToSet);
        
        return (Message) ois.readObject();
    }
    
    public Message setMessageContent(Conversation conversationWithMessage, long timestamp, String contentToSet)
            throws IOException, ClassNotFoundException {
        return setMessageContent(conversationWithMessage.getMessageByTimestamp(timestamp), contentToSet);
    }
    
    public Conversation addMessageToConversation(Conversation conversationToModify, Message messageToAdd)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.AddMessageToConversation, conversationToModify, messageToAdd);
        
        conversationToModify.addMessage(messageToAdd);
        
        return (Conversation) ois.readObject();
    }
    
    public User userBlocksUser(User toModify, User toBlock) throws IOException, ClassNotFoundException {
        sendToServer(Operation.UserBlocksUser, toModify, toBlock);
        
        if (!toModify.getBlockedEmails().contains(toBlock.getEmail())) {
            toModify.blockUser(toBlock.getEmail());
        }
        
        return (User) ois.readObject();
    }
    
    public User userInvisibleToUser(User userToModify, User userToBecomeInvisible)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.UserInvisibleToUser, userToModify, userToBecomeInvisible);
        
        if (!userToModify.getInvisEmails().contains(userToBecomeInvisible.getEmail())) {
            userToModify.blockUser(userToBecomeInvisible.getEmail());
        }
        
        return (User) ois.readObject();
    }
   
    public Seller addStoreToSeller(Seller sellerToModify, String storeName)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.SellerAddStore, sellerToModify, storeName);
        
        sellerToModify.addStoreName(storeName);
        
        return (Seller) ois.readObject();
    }
    
    public Conversation getConversationBetweenUsersWithStore(User user1, User user2, String store)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.GetConversationWithUsersWithStore, user1, user2, store);
        
        return (Conversation) ois.readObject();
    }
    
    public Conversation sendMessageFromFile(Conversation conversationToSendIn, User sender, File messageContent)
            throws IOException, ClassNotFoundException {
        sendToServer(Operation.SendMessageFromFile, conversationToSendIn, sender, messageContent);
        
        return (Conversation) ois.readObject();
    }
    
    public User setUserName(User userToEdit, String nameToSet) throws IOException, ClassNotFoundException {
        sendToServer(Operation.SetUserName, userToEdit, nameToSet);
        
        return (User) ois.readObject();
    }
    
    public User setUserPass(User userToEdit, String passToSet) throws IOException, ClassNotFoundException {
        sendToServer(Operation.SetUserPass, userToEdit, passToSet);
        
        return (User) ois.readObject();
    }
    
    public User deleteUser(User userToDelete) throws IOException, ClassNotFoundException {
        sendToServer(Operation.DeleteUserAccount, userToDelete);
        
        return (User) ois.readObject(); // will always be null
    }
    
}
