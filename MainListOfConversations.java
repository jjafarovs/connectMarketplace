import java.util.ArrayList;
import java.util.Objects;

/**
 * {@link MainListOfConversations}<br />
 * Main list of conversations
 *
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 * @version 12/12/2022
 */
public final class MainListOfConversations {
    private static final ArrayList<Conversation> mainListOfConversations;
    
    static {
        mainListOfConversations = new ArrayList<>();
    }
    
    /**
     * Blank constructor
     */
    private MainListOfConversations() {
    }
    
    /**
     * Returns the list of all conversations
     */
    public static ArrayList<Conversation> getMainListOfConversations() {
        return mainListOfConversations;
    }
    
    /**
     * Returns a list of Conversations associated with the given User
     *
     * @param user the user
     * @return a list of Conversations associated with the given User
     */
    public static ArrayList<Conversation> getConversationsWithUser(User user) {
        ArrayList<Conversation> arrayListToReturn = new ArrayList<>();
        for (int i = 0; i < getMainListOfConversations().size(); i++) {
            if (user instanceof Customer) {
                if (getMainListOfConversations().get(i).getCustomer().equals(user)) {
                    arrayListToReturn.add(mainListOfConversations.get(i));
                }
            } else if (user instanceof Seller) {
                if (getMainListOfConversations().get(i).getSeller().equals(user)) {
                    arrayListToReturn.add(mainListOfConversations.get(i));
                }
            }
        }
        return arrayListToReturn;
    }
    
    /**
     * Adds a new conversation to the list of all conversations
     */
    @Deprecated
    public static void addConversation(Conversation c) {
        if (!mainListOfConversations.contains(c)) {
            mainListOfConversations.add(c);
        }
    }
    
    /**
     * Removes a new conversation to the list of all conversations
     */
    @Deprecated
    private static void removeConversation(Conversation c) {
        mainListOfConversations.remove(c);
    }
    
    /**
     * Returns a Conversation associated with the two provided Users
     *
     * @param user1 the first User
     * @param user2 the second User
     * @return a Conversation associated with the two provided Users
     */
    public static Conversation getConversationWithUsers(User user1, User user2) {
        Customer c;
        Seller s;
        if (user1 instanceof Seller && user2 instanceof Customer) {
            s = (Seller) user1;
            c = (Customer) user2;
        } else if (user1 instanceof Customer && user2 instanceof Seller) {
            s = (Seller) user2;
            c = (Customer) user1;
        } else {
            throw new IllegalArgumentException("Conversations can only be between a Seller and a Customer");
        }
        
        ArrayList<Conversation> conversationsToCheck = getConversationsWithUser(c);
        for (Conversation convo : conversationsToCheck) {
            if (convo.getSeller().equals(s)) {
                return convo;
            }
        }
        return null;
    }
    
    /**
     * Returns a {@link Conversation} associated with the two provided Users
     *
     * @param user1 the first {@link User}
     * @param user2 the second {@link User}
     * @param store the store associated with this {@link Conversation}
     * @return a {@link Conversation} associated with the two provided {@link User}s
     */
    public static Conversation getConversationWithUsersWithStore(User user1, User user2, String store) {
        Customer c;
        Seller s;
        if (user1 instanceof Seller && user2 instanceof Customer) {
            s = (Seller) user1;
            c = (Customer) user2;
        } else if (user1 instanceof Customer && user2 instanceof Seller) {
            s = (Seller) user2;
            c = (Customer) user1;
        } else {
            throw new IllegalArgumentException("Conversations can only be between a Seller and a Customer " +
                    "(passed " + user1.getEmail() + ", " + user2.getEmail() + ")");
        }
        
        ArrayList<Conversation> conversationsToCheck = getConversationsWithUser(c);
        for (Conversation convo : conversationsToCheck) {
            if (convo.getSeller().equals(s)) {
                if (Objects.equals(store, convo.getStore())) {
                    return convo;
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the stored & "official" version of this {@link Conversation}
     *
     * @param toFind the equivalent {@link Conversation} to look for
     * @return the stored "official" {@link Conversation}
     */
    static Conversation getConversation(Conversation toFind) {
        for (Conversation c : mainListOfConversations) {
            if (c.equals(toFind)) {
                return c;
            }
        }
        return null;
    }
    
}
