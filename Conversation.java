import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

/**
 * {@link Conversation}<br />
 * This class represents a conversation between two {@link User}s and keeps track of the {@link Message}s between them
 *
 * @author Javad Jafarov, Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin
 * @version 12/12/2022
 */
public class Conversation implements Serializable {
    private final Seller seller;
    private final Customer customer;
    private ArrayList<Message> messages;
    private String store;
    private boolean isDisappearing;
    
    /**
     * Constructor for Conversation
     *
     * @param seller   the Seller
     * @param store    the Store
     * @param customer the Customer
     */
    public Conversation(Seller seller, String store, Customer customer) {
        this(seller, store, customer, false);
    }
    
    /**
     * Constructor for Conversation
     *
     * @param seller   the Seller
     * @param customer the Customer
     */
    public Conversation(Seller seller, Customer customer) {
        this(seller, "unknown store", customer);
    }
    
    /**
     * Constructor for Conversation
     *
     * @param seller         the Seller
     * @param customer       the Customer
     * @param isDisappearing is disappearing
     */
    public Conversation(Seller seller, Customer customer, boolean isDisappearing) {
        this(seller, "unknown store", customer, isDisappearing);
    }
    
    /**
     * Constructor for Conversation
     *
     * @param seller         the Seller
     * @param store          the Store
     * @param customer       the Customer
     * @param isDisappearing is disappearing
     */
    public Conversation(Seller seller, String store, Customer customer, boolean isDisappearing) {
        this.seller = seller;
        this.store = store;
        this.customer = customer;
        this.isDisappearing = isDisappearing;
        
        boolean conversationExists = false;
        ArrayList<Conversation> mainListOfConversations = MainListOfConversations.getMainListOfConversations();
        for (Conversation mainListConversation : mainListOfConversations) {
            if (mainListConversation.getCustomer().equals(customer) &&
                    mainListConversation.getSeller().equals(seller) &&
                    Objects.equals(mainListConversation.getStore(), store)) {
                this.messages = mainListConversation.getMessages();
                conversationExists = true;
            }
        }
        
        if (!conversationExists) {
            this.messages = new ArrayList<>();
        }
        
        MainListOfConversations.getMainListOfConversations().add(this);
    }
    
    /**
     * Returns the Seller associated with this Conversation
     *
     * @return the Seller associated with this Conversation
     */
    public Seller getSeller() {
        return seller;
    }
    
    /**
     * Returns the Customer associated with this Conversation
     *
     * @return the Customer associated with this Conversation
     */
    public Customer getCustomer() {
        return customer;
    }
    
    /**
     * Returns the store associated with this Conversation
     *
     * @return the store associated with this Conversation
     */
    public String getStore() {
        return store;
    }
    
    /**
     * Sets the store associated with this Conversation
     *
     * @param store the store to set
     */
    private void setStore(String store) {
        this.store = store;
    }
    
    /**
     * Returns this Conversation's list of Messages
     *
     * @return this Conversation's list of Messages
     */
    public ArrayList<Message> getMessages() {
        return messages;
    }
    
    /**
     * Prints this conversation to the console
     */
    public void printConversation() {
        for (Message message : messages) {
            System.out.println("[" + message.getTimeSent() + "] " + message.getSender().getName()
                    + " : " + message.getMessageContent());
        }
    }
    
    /**
     * Returns the number of messages sent by the User provided
     *
     * @param sender the User to check number of messages by
     * @return the number of messages sent by the User provided
     */
    public int getNumberOfMessagesSentBy(User sender) {
        int counter = 0;
        for (Message m : messages) {
            if (m.getSender().equals(sender)) {
                counter++;
            }
        }
        return counter;
    }
    
    /**
     * Returns whether this conversation is disappearing
     *
     * @return whether this conversation is disappearing
     */
    public boolean isDisappearing() {
        return isDisappearing;
    }
    
    /**
     * Sets this Conversation's disappearing status
     *
     * @param isDisappearing the value to set
     */
    protected void setIsDissapearing(boolean isDisappearing) {
        this.isDisappearing = isDisappearing;
    }
    
    /**
     * Adds a {@link Message} to this {@link Conversation}
     *
     * @param message the {@link Message} to add
     */
    public void sendMessage(Message message) {
        addMessage(message);
    }
    
    /**
     * //Sends a message from fromUser with messageContent collected from the provided file
     *
     * @param fromUser         the User to send a message from
     * @param fromFileLocation the (.txt) file with the desired message content
     */
    public void sendMessageFromFile(User fromUser, User toUser, String fromFileLocation) throws IOException {
        sendMessageFromFile(fromUser, new File(fromFileLocation));
        File fileToSend = new File(fromFileLocation);
        if (fileToSend.exists()) {
            
            StringBuilder messageToSend = new StringBuilder("");
            BufferedReader bfr = new BufferedReader(new FileReader(fromFileLocation));
            String line = bfr.readLine();
            messageToSend.append(line);
            while (line != null) {
                messageToSend.append("\n");
                line = bfr.readLine();
                if (line != null) {
                    messageToSend.append(line);
                }
            }
            
            messageToSend = new StringBuilder(messageToSend.substring(0, messageToSend.length() - 1));
            sendMessage(new Message(fromUser.getEmail(), toUser.getEmail(), messageToSend.toString()));
        }
    }
    
    /**
     * Sends a message from fromUser with messageContent collected from the provided file
     *
     * @param fromUser the User to send a message from
     * @param fromFile the (.txt) file with the desired message content
     */
    public void sendMessageFromFile(User fromUser, File fromFile) {
        StringBuilder messageContent = new StringBuilder();
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(fromFile));
            
            String line = br.readLine();
            while (line != null) {
                messageContent.append(line);
                line = br.readLine();
                if (line != null) {
                    messageContent.append('\u00B6');
                }
            }
            
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        messages.add(new Message(fromUser, otherUser(fromUser), messageContent.toString()));
    }
    
    /**
     * Adds a Message to the list of {@link Message}s contained by this {@link Conversation}
     * <br /><em>Functionally indifferent from sendMessage(Message message)</em>
     *
     * @param message the {@link Message} to add
     */
    public void addMessage(Message message) {
        if (message.getParent() != null && (!message.getParent().equals(this) ||
                !message.getParent().getStore().equals(store))) {
            return;
        }
        User receiver = (message.getReceiverEmail().equals(customer.getEmail()) ? customer : seller);
        User sender = otherUser(receiver);
        if (receiver.getBlockedEmails().contains(sender.getEmail()) ||
                (getMessageByTimestamp(message.getTimeSent()) != null &&
                        getMessageByTimestamp(message.getTimeSent()).equals(message))) {
            return;
        }
        // Ensures concurrency safety by disallowing messages from sharing a timestamp (effectively an id)
        while (getMessageByTimestamp(message.getTimeSent()) != null) {
            message.setTimeSent(message.getTimeSent() + 1);
        }
        this.messages.add(message);
        if (message.getParent() == null) {
            message.setParent(this);
        }
    }
    
    /**
     * Removes the given {@link Message} from this {@link Conversation}'s history
     *
     * @param message the message to remove
     */
    private void removeMessage(Message message) {
        getMessages().remove(message);
    }
    
    /**
     * Gets the {@link Message} with the given timestamp
     *
     * @param timestamp the timestamp to check for
     * @return the {@link Message} with the given timestamp
     */
    public Message getMessageByTimestamp(long timestamp) {
        Message out = null;
        
        for (Message m : messages) {
            if (m.getTimeSent() == timestamp) {
                out = m;
            }
        }
        
        return out;
    }
    
    /**
     * Returns the other User associated with this conversation
     *
     * @param user the user to compare to
     * @return the other User associated with this conversation
     */
    public User otherUser(User user) {
        if (user.equals(seller)) {
            return customer;
        } else if (user.equals(customer)) {
            return seller;
        } else {
            return null;
        }
    }
    
    /**
     * Saves this Conversation as a CSV as seen by the given User
     *
     * @param filename the file to save it to
     * @param user     the User to save this Conversation for
     * @return the File object
     */
    public File saveToCSVFile(String filename, User user) {
        if (!filename.endsWith(".csv")) {
            System.out.println("Saving csv data to non-csv file (\"" + filename + "\")");
        }
        
        File f = new File(filename);
        
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
            
            bw.write(asCSV(user));
            
            bw.flush();
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return f;
    }
    
    /**
     * Returns this Conversation as a CSV representation
     *
     * @param user the User to save this Conversation for
     * @return the String containing the CSV representation of this Conversation
     */
    public String asCSV(User user) {
        // Returns null iff user is not in this Conversation
        User other = otherUser(user);
        if (other == null) {
            throw new IllegalArgumentException("User not a part of this Conversation!");
        }
        StringBuilder sb = new StringBuilder();
        
        sb.append("userEmail,otherUserEmail,senderName,timestamp,messageContents").append(System.lineSeparator());
        
        // Participants, Message sender, timestamp, and contents
        for (Message m : messages) {
            if ((m.getReceiverEmail().equals(user.getEmail()) && m.canReceiverView())
                    || (m.getSenderEmail().equals(user.getEmail()) && m.canSenderView())) {
                String senderName = (m.getSenderEmail().equals(user.getEmail()) ? user.getName() : other.getName());
                sb.append(user.getEmail().replace(",", "\\,")).append(',').
                        append(other.getEmail().replace(",", "\\,")).append(',')
                        .append(senderName.replace(",", "\\,")).append(',')
                        .append(m.getTimeSent()).append(',')
                        .append(m.getMessageContentFiltered(user).replace(",", "\\,")).append(System.lineSeparator());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a {@link String} representation of this {@link Conversation}
     *
     * @return a {@link String} representation of this {@link Conversation}
     */
    @Override
    public String toString() {
        StringBuilder messageString = new StringBuilder();
        for (Message m : messages) {
            messageString.append(m.toString()).append(",");
        }
        return String.format(
                "{\"type\":\"%s\",\"seller\":\"%s\",\"customer\":\"%s\",\"store\":\"%s\",\"messages\":[%s]}",
                this.getClass(), seller.toString(), customer.toString(), (store == null ? "null" : store),
                (messageString.length() > 0 ? messageString.substring(0, messageString.length() - 1) : ""));
    }
    
    /**
     * An alternate toString method
     *
     * @param light whether to print the {@link Seller} and {@link Customer} as an email (lighter format(
     * @return toString but light
     */
    public String toString(boolean light) {
        StringBuilder messageString = new StringBuilder();
        for (Message m : messages) {
            messageString.append(m.toString()).append(",");
        }
        return String.format(
                "{\"type\":\"%s\",\"seller\":\"%s\",\"customer\":\"%s\",\"store\":\"%s\",\"messages\":[%s]}",
                this.getClass(), (light ? seller.getEmail() : seller.toString()),
                (light ? customer.getEmail() : customer.toString()), (store == null ? "null" : store),
                (messageString.length() > 0 ? messageString.substring(0, messageString.length() - 1) : ""));
    }
    
    /**
     * Returns whether this equals o
     *
     * @param o the {@link Object} to compare
     * @return whether this equals o
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return isDisappearing == that.isDisappearing && seller.equals(that.seller) && customer.equals(that.customer)
                && Objects.equals(this.store, that.store);
    }
    
}
