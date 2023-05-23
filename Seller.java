import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * {@link Seller}<br />
 * A {@link User} that has stores and can message {@link Customer}s
 *
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 * @version 12/12/2022
 */
public final class Seller extends User {
    private final ArrayList<String> storeNames;
    
    /**
     * A Seller constructor
     *
     * @param name       name
     * @param email      email
     * @param password   password
     * @param storeNames storeNames
     * @throws IllegalArgumentException if email is either of invalid syntax or already taken
     */
    public Seller(String name, String email, String password, String... storeNames)
            throws IllegalArgumentException {
        super(name, email, password);
        
        this.storeNames = new ArrayList<>();
        Collections.addAll(this.storeNames, storeNames);
    }
    
    /**
     * A Seller constructor
     *
     * @param name       name
     * @param email      email
     * @param password   password
     * @param storeNames storeNames
     * @throws IllegalArgumentException if email is either of invalid syntax or already taken
     */
    public Seller(String name, String email, String password, ArrayList<String> storeNames)
            throws IllegalArgumentException {
        super(name, email, password);
        
        this.storeNames = storeNames;
    }
    
    /**
     * A Seller constructor
     *
     * @param name           name
     * @param email          email
     * @param password       password
     * @param blockedPhrases blockedPhrases
     * @param storeNames     storeNames
     * @throws IllegalArgumentException if email is either of invalid syntax or already taken
     */
    public Seller(String name, String email, String password,
                  HashMap<String, String> blockedPhrases, ArrayList<String> storeNames)
            throws IllegalArgumentException {
        super(name, email, password, blockedPhrases);
        
        this.storeNames = storeNames;
    }
    
    /**
     * A Seller constructor
     *
     * @param name           name
     * @param email          email
     * @param password       password
     * @param blockedPhrases blockedPhrases
     * @param storeNames     storeNames
     * @throws IllegalArgumentException if email is either of invalid syntax or already taken
     */
    public Seller(String name, String email, String password,
                  HashMap<String, String> blockedPhrases, String... storeNames) throws IllegalArgumentException {
        super(name, email, password, blockedPhrases);
        
        this.storeNames = new ArrayList<>();
        Collections.addAll(this.storeNames, storeNames);
    }
    
    /**
     * Returns (not prints) a String representation of all stores
     *
     * @return the String representation of all stores
     */
    public static String getAllStoresAsString() {
        StringBuilder out = new StringBuilder();
        Set<String> emails = getUserEmails();
        for (String email : emails) {
            User u = User.getUser(email);
            if (!(u instanceof Seller)) {
                continue;
            }
            out.append(((Seller) u).getSellerStoresAsString()).append(System.lineSeparator());
        }
        return out.toString();
    }
    
    /**
     * Returns the Seller associated with a store name
     *
     * @param storeName the storeName to search for
     * @return the Seller
     */
    public static Seller getSellerFromStore(String storeName) {
        ArrayList<Seller> sellers = User.getAllSellers();
        for (Seller s : sellers) {
            if (s.getStoreNames().contains(storeName)) {
                return s;
            }
        }
        return null;
    }
    
    /**
     * Returns an {@link ArrayList} of store names
     *
     * @return an {@link ArrayList} of store names
     */
    ArrayList<String> getStoreNames() {
        return storeNames;
    }
    
    /**
     * Returns the stores associated with this seller
     *
     * @return the stores associated with this seller
     */
    public String getSellerStoresAsString() {
        StringBuilder out = new StringBuilder();
        for (String storeName : storeNames) {
            out.append(storeName).append(System.lineSeparator());
        }
        return out.substring(0, out.length() - System.lineSeparator().length());
    }
    
    /**
     * Adds a store name to this seller's list of stores
     *
     * @param storeName the store name to add
     */
    public void addStoreName(String storeName) {
        storeNames.add(storeName);
    }
    
    /**
     * Remove a store from this seller's list of stores
     *
     * @param storeName the store to remove
     * @return whether the store previously existed
     */
    public boolean removeStoreName(String storeName) {
        return storeNames.remove(storeName);
    }
    
    
    /**
     * Returns the statistics for this {@link Seller}
     *
     * @return the statistics for this {@link Seller}
     */
    public String getStatistics() {
        ArrayList<Conversation> conversationsList = MainListOfConversations.getConversationsWithUser(this);
        StringBuilder out = new StringBuilder();
        for (Conversation c : conversationsList) {
            if (!(getInvisEmails().contains(this.getEmail()))) {
                out.append("Store: ").append(c.getStore() == null ? "none" : c.getStore())
                        .append(c.getCustomer().getName()).append(", ")
                        .append(c.getCustomer().getEmail()).append(", Number of Messages: ")
                        .append(c.getCustomer().getNumberOfMessagesSent()).append(", Most Common Word Used: ")
                        .append(c.getCustomer().getMostCommonWord()).append(System.lineSeparator());
            }
        }
        return out.toString();
    }
    
    /**
     * does not do the thing
     *
     * @return notihng
     */
    public String sortDashboard() {
        return "";
    }
    
    /**
     * Turns this Seller into a file-safe String equivalent
     *
     * @return the file-safe String version of this Seller
     */
    public String toFileLine() {
        final String d = Server.DELIMITER;
        
        StringBuilder postpend = new StringBuilder();
        for (String s : storeNames) {
            postpend.append("S_").append(clean(s)).append(d);
        }
        ArrayList<String> invisEmails = super.getInvisEmails();
        for (String s : invisEmails) {
            postpend.append("I_").append(clean(s)).append(d);
        }
        ArrayList<String> blockedEmails = super.getBlockedEmails();
        for (String s : blockedEmails) {
            postpend.append("B_").append(clean(s)).append(d);
        }
        
        return clean(getEmail()) + d
                + clean(getEmail()) + d
                + "SELLER" + d
                + clean(getName()) + d
                + clean(getPassword()) + d
                + postpend;
    }
    
    /**
     * Returns a {@link String} representation of this {@link Seller}
     *
     * @return a {@link String} representation of this {@link Seller}
     */
    @Override
    public String toString() {
        String superString = super.toString();
        superString = superString.substring(0, superString.length() - 1);
        return superString + ",\"storeNames\":\"[" + getSellerStoresAsString().replace(System.lineSeparator(),
                ",") + "]\"}";
    }
    
    /**
     * returns a String of the dashboard for a seller
     *
     * @param option a string of "ascending" or "descending"
     */
    public String viewDashboardSeller(String option) {
        ArrayList<Integer> messagesSent = new ArrayList<>();
        ArrayList<String> emailInOrder = new ArrayList<>();
        Set<String> emails = getUserEmails();
        String returnString = "";
        if (option.equalsIgnoreCase("Ascending")) {
            for (String em : emails) {
                User e = getUser(em);
                if (e instanceof Customer) {
                    Customer c = (Customer) e;
                    if (c.getNumberOfMessagesSent() != 0) {
                        messagesSent.add(c.getNumberOfMessagesSent());
                    }
                }
            }
            Collections.sort(messagesSent);
            
            for (String em : emails) {
                User e = getUser(em);
                if (e instanceof Customer) {
                    Customer c = (Customer) e;
                    for (int i = 0; i < messagesSent.size(); i++) {
                        if (c.getNumberOfMessagesSent() == messagesSent.get(i)) {
                            emailInOrder.add(i, c.getEmail());
                        }
                    }
                }
            }
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Customer) {
                    Customer c = (Customer) u;
                    if (c.getNumberOfMessagesSent() == 0) {
                        returnString += ("Name: " + c.getName() +
                                ", Email: " + c.getEmail() +
                                ", Messages Sent: " + c.getNumberOfMessagesSent() +
                                ", Most Common Word: " + c.getMostCommonWord());
                    }
                }
            }
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Customer) {
                    Customer c = (Customer) u;
                    for (int i = 0; i < emailInOrder.size(); i++) {
                        if (c.getEmail().equalsIgnoreCase(emailInOrder.get(i))) {
                            returnString += ("Name: " + c.getName() +
                                    ", Email: " + c.getEmail() +
                                    ", Messages Sent: " + c.getNumberOfMessagesSent() +
                                    ", Most Common Word: " + c.getMostCommonWord());
                        }
                    }
                }
            }
            
            return returnString;
            
        } else if (option.equalsIgnoreCase("Descending")) {
            
            for (String em : emails) {
                User e = getUser(em);
                if (e instanceof Customer) {
                    Customer c = (Customer) e;
                    if (c.getNumberOfMessagesSent() != 0) {
                        messagesSent.add(c.getNumberOfMessagesSent());
                    }
                }
            }
            Collections.sort(messagesSent);
            
            for (String em : emails) {
                User e = getUser(em);
                if (e instanceof Customer) {
                    Customer c = (Customer) e;
                    for (int i = 0; i < messagesSent.size(); i++) {
                        if (c.getNumberOfMessagesSent() == messagesSent.get(i)) {
                            emailInOrder.add(i, c.getEmail());
                        }
                    }
                }
            }
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Customer) {
                    Customer c = (Customer) u;
                    for (int i = 0; i < emailInOrder.size(); i++) {
                        if (c.getEmail().equalsIgnoreCase(emailInOrder.get(i))) {
                            returnString += ("Name: " + c.getName() +
                                    ", Email: " + c.getEmail() +
                                    ", Messages Sent: " + c.getNumberOfMessagesSent() +
                                    ", Most Common Word: " + c.getMostCommonWord());
                        }
                    }
                }
            }
            
            for (String em : emails) {
                User u = getUser(em);
                if (u instanceof Customer) {
                    Customer c = (Customer) u;
                    if (c.getNumberOfMessagesSent() == 0) {
                        returnString += ("Name: " + c.getName() +
                                ", Email: " + c.getEmail() +
                                ", Messages Sent: " + c.getNumberOfMessagesSent() +
                                ", Most Common Word: " + c.getMostCommonWord());
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
