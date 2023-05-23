import java.util.*;

/**
 * {@link Customer}<br />
 * A {@link User} that can message {@link Seller}s
 *
 * @author Javad Jafarov, Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin
 * @version 12/12/2022
 */
public final class Customer extends User {
    
    public Customer(String name, String email, String password) throws IllegalArgumentException {
        super(name, email, password);
    }
    
    public Customer(String name, String email, String password, HashMap<String, String> blockedPhrases) {
        super(name, email, password, blockedPhrases);
    }
    
    /**
     * Sends a message to a given store
     *
     * @param from           the Customer sending the message
     * @param store          the store (owner) receiving the message
     * @param messageContent the message content
     * @return the Seller associated with the store
     */
    @Deprecated
    private static Seller sendMessageToStore(Customer from, String store, String messageContent) {
        Set<String> uids = User.getUserEmails();
        for (String id : uids) {
            User u = getUser(id);
            if (!(u instanceof Seller)) {
                continue;
            }
            Seller s = (Seller) u;
            if (s.getStoreNames().contains(store)) {
                Conversation c = MainListOfConversations.getConversationWithUsers(from, s);
                if (c == null) {
                    c = User.createConversation(from, s, false);
                    if (c == null)
                        throw new NullPointerException("Unexpected issue here (should never happen?)");
                }
                c.sendMessage(/*s, from,*/ new Message(from, s, messageContent));
//                c.setStore(store);
                return s;
            }
        }
        return null;
    }
    
    /**
     * Gets a customer from their email.
     *
     * @param customerEmail customer's email
     * @return the Customer associated with the email
     */
    public static Customer getCustomerFromCustomerEmail(String customerEmail) {
        ArrayList<Customer> customers = User.getAllCustomers();
        for (Customer c : customers) {
            if (c.getEmail().contains(customerEmail)) {
                return c;
            }
        }
        return null;
    }
    
    public String sortDashboard() {
        return "";
    }
    
    /**
     * Sends a message to the given store
     *
     * @param store   the store to send the message to
     * @param message the message to send
     * @return the Seller associated with the store
     */
    private Seller sendMessageToStore(String store, String message) {
        return null; // sendMessageToStore(this, store, message);
    }
    
    /**
     * Turns this Seller into a file-safe String equivalent
     *
     * @return the file-safe String version of this Seller
     */
    public String toFileLine() {
        final String d = Server.DELIMITER;
        
        StringBuilder postpend = new StringBuilder();
        ArrayList<String> invisIds = super.getInvisEmails();
        for (String s : invisIds) {
            postpend.append("I_").append(clean(s)).append(d);
        }
        ArrayList<String> blockedIds = super.getBlockedEmails();
        for (String s : blockedIds) {
            postpend.append("B_").append(clean(s)).append(d);
        }
        
        return clean(getEmail()) + d
                + clean(getEmail()) + d
                + "CUSTOMER" + d
                + clean(getName()) + d
                + clean(getPassword()) + d
                + postpend;
    }
    
    /**
     * returns a String of the dashboard as a customer
     *
     * @param option a string of "ascending" or "descending"
     */
    public String viewDashboardCustomer(String option) {
        ArrayList<Integer> messagesReceived = new ArrayList<>();
        ArrayList<String> emailInOrder = new ArrayList<>();
        Set<String> emails = getUserEmails();
        String returnString = "";
        String storeString = Seller.getAllStoresAsString();
        ArrayList<Seller> sellers = User.getAllSellers();
        for (Seller seller : sellers) {
            if (seller.getInvisEmails().contains(this.getEmail())) {
                storeString = storeString.replace(seller.getSellerStoresAsString(), "");
            }
        }
        String[] storeSplit = storeString.split("\n");
        ArrayList<String> storeOptions = new ArrayList<>(Arrays.asList(storeSplit));
        Object[] storesOptionsAsArray = storeOptions.toArray();
        //storesOptionsAsArray[0].messagesReceived();
        
        if (option.equalsIgnoreCase("Ascending")) {
            for (String em : emails) {
                User e = getUser(em);
                if (e instanceof Seller) {
                    Seller s = (Seller) e;
                    if (s.getNumberOfMessagesSent() != 0) {
                        messagesReceived.add(s.getNumberOfMessagesSent());
                    }
                }
            }
            Collections.sort(messagesReceived);
            
            for (String em : emails) {
                User e = getUser(em);
                if (e instanceof Seller) {
                    Seller s = (Seller) e;
                    for (int i = 0; i < messagesReceived.size(); i++) {
                        if (s.getNumberOfMessagesSent() == messagesReceived.get(i)) {
                            emailInOrder.add(i, s.getEmail());
                        }
                    }
                }
            }
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Seller) {
                    Seller s = (Seller) u;
                    if (s.getNumberOfMessagesSent() == 0) {
                        returnString += ("Store name: " + s.getStoreNames() +
                                ", Email: " + s.getEmail() +
                                ", Messages Received: " + s.getNumberOfMessagesSent() +
                                ", Most Common Word: " + s.getMostCommonWord());
                    }
                }
            }
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Seller) {
                    Seller s = (Seller) u;
                    for (int i = 0; i < emailInOrder.size(); i++) {
                        if (s.getEmail().equalsIgnoreCase(emailInOrder.get(i))) {
                            returnString += ("Store name: " + s.getStoreNames() +
                                    ", Email: " + s.getEmail() +
                                    ", Messages Received: " + getNumberOfMessagesSentTo(s) +
                                    ", Messages Sent To: " + this.getNumberOfMessagesSentTo(s));
                        }
                    }
                }
            }
            
            return returnString;
            
        } else if (option.equalsIgnoreCase("Descending")) {
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Seller) {
                    Seller s = (Seller) u;
                    if (s.getNumberOfMessagesSent() != 0) {
                        messagesReceived.add(s.getNumberOfMessagesSent());
                    }
                }
            }
            Collections.reverse(messagesReceived);
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Seller) {
                    Seller s = (Seller) u;
                    for (int i = 0; i < messagesReceived.size(); i++) {
                        if (s.getNumberOfMessagesSent() == messagesReceived.get(i)) {
                            emailInOrder.add(i, s.getEmail());
                        }
                    }
                }
            }
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Seller) {
                    Seller s = (Seller) u;
                    for (int i = 0; i < emailInOrder.size(); i++) {
                        if (s.getEmail().equalsIgnoreCase(emailInOrder.get(i))) {
                            returnString += ("Store Name: " + s.getStoreNames() +
                                    ", Email: " + s.getEmail() +
                                    ", Messages Received: " + s.getNumberOfMessagesSent() +
                                    ", Most Common Word: " + s.getMostCommonWord());
                        }
                    }
                }
            }
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Seller) {
                    Seller s = (Seller) u;
                    if (s.getNumberOfMessagesSent() == 0) {
                        returnString += ("Store Name: " + s.getStoreNames() +
                                ", Email: " + s.getEmail() +
                                ", Messages Received: " + s.getNumberOfMessagesSent() +
                                ", Most Common Word: " + s.getMostCommonWord());
                    }
                }
            }
            
            return returnString;
            
        } else {
            returnString += ("Not a valid sorting option!");
            
            return returnString;
        }
    }
    
}
