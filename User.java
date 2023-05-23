import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * {@link User}<br />
 * A User superclass to structure how {@link Customer}s and {@link Seller}s should function
 *
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 * @version 12/12/2022
 */
public abstract class User implements Serializable {
    //////// CLASS VARIABLES ////////
    /**
     * RESERVED_EMAILS - contains a full list of all the user's emails so the
     * USERS - a hasmap containing all the users to ensure there are no duplicates
     * WHITESPACE_CHARS - checks for whitespace characters in the emails
     */
    private static final Set<String> RESERVED_EMAILS; // the emails that have been used so far
    private static final HashMap<String, User> USERS;
    private static final Object ADD_USER;
    
    // I am sorry for this; Java's built-in Regex does not match \s properly; explanation from stackoverflow below
    // Java doesn’t support the Unicode white space property even though doing so is required to meet UTS#18’s RL1.2
    /**
     * Updates USERS hashmap when a new USER is added
     * Updates the RESERVED_EMAILS using the USERS hashmap keys
     */
    private static final String WHITESPACE_CHARS = ""
            + "\\u0009" // CHARACTER TABULATION
            + "\\u000A" // LINE FEED (LF)
            + "\\u000B" // LINE TABULATION
            + "\\u000C" // FORM FEED (FF)
            + "\\u000D" // CARRIAGE RETURN (CR)
            + "\\u0020" // SPACE
            + "\\u0085" // NEXT LINE (NEL)
            + "\\u00A0" // NO-BREAK SPACE
            + "\\u1680" // OGHAM SPACE MARK
            + "\\u180E" // MONGOLIAN VOWEL SEPARATOR
            + "\\u2000" // EN QUAD
            + "\\u2001" // EM QUAD
            + "\\u2002" // EN SPACE
            + "\\u2003" // EM SPACE
            + "\\u2004" // THREE-PER-EM SPACE
            + "\\u2005" // FOUR-PER-EM SPACE
            + "\\u2006" // SIX-PER-EM SPACE
            + "\\u2007" // FIGURE SPACE
            + "\\u2008" // PUNCTUATION SPACE
            + "\\u2009" // THIN SPACE
            + "\\u200A" // HAIR SPACE
            + "\\u2028" // LINE SEPARATOR
            + "\\u2029" // PARAGRAPH SEPARATOR
            + "\\u202F" // NARROW NO-BREAK SPACE
            + "\\u205F" // MEDIUM MATHEMATICAL SPACE
            + "\\u3000"; // IDEOGRAPHIC SPACE
    
    static {
        USERS = new HashMap<>();
        RESERVED_EMAILS = USERS.keySet();
        ADD_USER = "Lock for adding Users to User.USERS";
    }
    
    private final String email;
    private final HashMap<String, String> blockedPhrases;
    private final ArrayList<String> blockedEmails;
    private final ArrayList<String> invisToEmails;
    //////// INSTANCE VARIABLES ////////
    private String name;
    private String password;
    
    /**
     * User constructor
     *
     * @param name     name
     * @param email    email
     * @param password password
     * @throws IllegalArgumentException if email is either of invalid syntax or already taken
     */
    protected User(String name, String email, String password) throws IllegalArgumentException {
        if (!isValidEmailSyntax(email)) {
            throw new IllegalArgumentException("Invalid email format!");
        } else if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid character \";\" in name!");
        } else if (RESERVED_EMAILS.contains(email)) {
            throw new IllegalArgumentException("Email already used! (The string before the @ symbol must be unique!");
        }
        
        this.name = name;
        this.email = email;
        this.password = hashPassword(password);
        
        this.blockedPhrases = new HashMap<>();
        
        this.blockedEmails = new ArrayList<>();
        this.invisToEmails = new ArrayList<>();
        synchronized (ADD_USER) {
            USERS.put(this.email, this);
        }
    }
    
    /**
     * User constructor
     *
     * @param name           name
     * @param email          email
     * @param password       password
     * @param blockedPhrases blockedPhrases
     * @throws IllegalArgumentException if email is either of invalid syntax or already taken
     */
    protected User(String name, String email, String password,
                   HashMap<String, String> blockedPhrases) throws IllegalArgumentException {
        if (!isValidEmailSyntax(email)) {
            throw new IllegalArgumentException("Invalid email format!");
        } else if (RESERVED_EMAILS.contains(email)) {
            throw new IllegalArgumentException("Email already used! (The string before the @ symbol must be unique!");
        }
        this.name = name;
        this.email = email;
        this.password = hashPassword(password);
        
        this.blockedPhrases = blockedPhrases;
        
        this.blockedEmails = new ArrayList<>();
        this.invisToEmails = new ArrayList<>();
        
        USERS.put(this.email, this);
    }
    
    /**
     * Checks if the given email is a valid email address (for our purposes)
     *
     * @param email The email provided
     * @return whether the email is a valid address (is formatted correctly)
     */
    public static boolean isValidEmailSyntax(String email) {
        return email.contains("@") && email.indexOf('@') == email.lastIndexOf('@')
                && (!email.matches(".*[" + WHITESPACE_CHARS + "\\\\/\\(\\);" + "].*")) && email.length() > 3
                && email.indexOf("@") != 0 && email.indexOf("@") != email.length() - 1;
    }
    
    /**
     * Returns the user with the specified email
     *
     * @param email the email of the user
     * @return the user with the specified email or null if there is no such user
     * @throws IllegalArgumentException if email is malformed
     */
    public static User getUser(String email) {
        return USERS.get(email);
    }
    
    /**
     * Returns whether a user with the specified email exists
     *
     * @param email the email to check
     * @return whether the specified user exists
     * @throws IllegalArgumentException if email is malformatted
     */
    public static boolean userExists(String email) throws IllegalArgumentException {
        return getUser(email) != null;
    }
    
    /**
     * Returns all user emails
     *
     * @return all user emails
     */
    protected static Set<String> getUserEmails() {
        return RESERVED_EMAILS;
    }
    
    /**
     * Creates a new message chain with the given user
     *
     * @param user1 the sender
     * @return the new message chain or the current one if one already exists
     */
    protected static Conversation createConversation(User user1, User user2, boolean isTemporary) {
        if (user1 instanceof Customer && user2 instanceof Seller && user1.isValidRecipient(user2)) {
            Conversation c = new Conversation((Seller) user2,
                    (Customer) user1, isTemporary);
//            addMessageChain(c);
            return c;
        } else if (user2 instanceof Customer && user1 instanceof Seller && user1.isValidRecipient(user2)) {
            Conversation c = new Conversation((Seller) user1,
                    (Customer) user2, isTemporary);
//            addMessageChain(c);
            return c;
        }
        return null;
    }
    
    /**
     * Returns whether the provided name is of valid syntax
     *
     * @param name the name to verify
     * @return whether the provided name is of valid syntax
     */
    public static boolean isValidName(String name) {
        return !name.contains(";") && name.length() > 0;
    }
    
    /**
     * Prepares a string for writing to a file
     *
     * @param toClean the String to clean
     * @return the cleaned String
     */
    protected static String clean(String toClean) {
        return toClean.replaceAll(Server.DELIMITER, Server.DELIMITER_REPLACEMENT)
                .replaceAll("[\r\n]+", "\\n")
                .replaceAll("B_", "B\\_") // blocked by indicator
                .replaceAll("I_", "I\\_") // invisible to indicator
                .replaceAll("S_", "S\\_"); // storename indicator
    }
    
    /**
     * Parses a String from the clean(String s) format
     *
     * @param toParse the String to parse
     * @return the parsed String
     */
    protected static String parse(String toParse) {
        return toParse.replaceAll(Server.DELIMITER_REPLACEMENT, Server.DELIMITER)
                .replaceAll("\\\\n", System.lineSeparator())
                .replaceAll("B\\\\_", "B_")
                .replaceAll("I\\\\_", "I_")
                .replaceAll("S\\\\_", "S_");
    }
    
    /**
     * Rebuilds and returns a Seller from a line of a save file
     *
     * @param sellerLine the file-safe String version of a Seller
     * @return the Seller object
     */
    public static User rebuildUser(String sellerLine) {
        String[] split = sellerLine.split(Server.DELIMITER);
        if (split.length < 5) {
            throw new IllegalArgumentException("Not enough data in line to re-create User");
        } else if (split[2].equals("SELLER")) {
            if (USERS.containsKey(parse(split[3]))) {
                return null;
            }
            Seller s = new Seller(parse(split[3]), parse(split[1]), "");
            User s_u = s;
            s_u.password = parse(split[4]);
            for (int i = 5; i < split.length; i++) {
                String line = parse(split[i]);
                if (line.length() < 2) {
                    continue;
                }
                if (line.startsWith("B_")) {
                    s_u.blockedEmails.add(parse(line.substring(2)));
                } else if (line.startsWith("I_")) {
                    s_u.invisToEmails.add(parse(line.substring(2)));
                } else if (line.startsWith("S_")) {
                    if (line.length() != 2) {
                        s.addStoreName(parse(split[i].substring(2)));
                    }
                } else {
                    System.err.println("Unrecognised prefix \"" + line.substring(0, 2) + "\" in line \""
                            + line + "\" for type Seller.");
                }
            }
            return s;
        } else if (split[2].equals("CUSTOMER")) {
            Customer c = new Customer(split[3], split[1], "");
            User c_u = c;
            c_u.password = parse(split[4]);
            for (int i = 5; i < split.length; i++) {
                String line = parse(split[i]);
                if (line.length() < 2) {
                    continue;
                }
                if (line.startsWith("B_")) {
                    c_u.blockedEmails.add(line.substring(2));
                } else if (line.startsWith("I_")) {
                    c_u.invisToEmails.add(line.substring(2));
                } else {
                    System.err.println("Unrecognised prefix \"" + line.substring(0, 2) + "\" in line \""
                            + line + "\" for type Customer.");
                }
            }
            return c;
        } else {
            throw new IllegalArgumentException(split[4] + " is not a valid user type.");
        }
    }
    
    /**
     * Hashes the password
     *
     * @param password the password to hash
     * @return the hashed password
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
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
            // shouldn't ever happen
            nsae.printStackTrace();
            return password;
        }
    }
    
    /**
     * Used for the client to access the hashmap of users
     *
     * @return the hasmap of users
     */
    public static Collection<User> getUsersCollection() {
        return USERS.values();
    }
    
    /**
     * Returns the formatted list all customers
     *
     * @return the list
     */
    public static String listCustomers() {
        StringBuilder sb = new StringBuilder();
        for (User u : USERS.values()) {
            if (u instanceof Customer) {
                sb.append(u.getEmail()).append(" : ").append(u.getName()).append(System.lineSeparator());
            }
        }
        if (sb.length() == 0) {
            return sb.toString();
        }
        
        return sb.substring(0, sb.length() - 1);
    }
    
    /**
     * Returns the formatted list all customers
     *
     * @return the list
     */
    public static String listSellers() {
        StringBuilder sb = new StringBuilder();
        for (User u : USERS.values()) {
            if (u instanceof Seller) {
                sb.append(u.getEmail()).append(" : ").append(u.getName());
            }
        }
        return sb.toString();
    }
    
    /**
     * Returns an ArrayList of all customers
     *
     * @return an ArrayList of all customers
     */
    public static ArrayList<Customer> getAllCustomers() {
        ArrayList<Customer> out = new ArrayList<>();
        Collection<User> users = USERS.values();
        for (User u : users) {
            if (u instanceof Customer) {
                out.add((Customer) u);
            }
        }
        return out;
    }
    
    /**
     * Returns an ArrayList of all customers
     *
     * @return an ArrayList of all customers
     */
    public static ArrayList<Seller> getAllSellers() {
        ArrayList<Seller> out = new ArrayList<>();
        Collection<User> users = USERS.values();
        for (User u : users) {
            if (u instanceof Seller) {
                out.add((Seller) u);
            }
        }
        return out;
    }
    
    /**
     * Removes a {@link User} from the USERS list
     *
     * @param user the {@link User} to remove
     */
    protected static void removeUser(User user) {
        if (user == null) {
            return;
        }
        USERS.remove(user.getEmail(), user);
    }
    
    /**
     * Returns this user's name
     *
     * @return name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the user's name
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the hashed password
     *
     * @return the user's hashed password
     */
    protected String getPassword() {
        return password;
    }
    
    /**
     * Changes the user's password to the new value (is hashed)
     *
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = hashPassword(password);
    }
    
    /**
     * Returns the user's email
     *
     * @return the user's email
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Checks whether the provided password matches the one on record (can the user sign in)
     *
     * @param password the password provided
     * @return whether signing in was a success
     */
    public boolean signIn(String password) {
        return hashPassword(password).equals(this.password);
    }
    
    /**
     * Adds a blocked phrase to the user's list of blocked phrases and replaces them with "****"
     *
     * @param phrase the phrase to block
     * @return whether the adding was successful
     */
    public boolean addBlockedPhrase(String phrase) {
        String replacementString = "";
        for (int i = 0; i < phrase.length(); i++) {
            replacementString += "*";
        }
        return this.addBlockedPhrase(phrase, replacementString);
    }
    
    /**
     * Adds a blocked phrase to the user's list of blocked phrases and replaces them with the provided replacement
     *
     * @param phrase      the phrase to block
     * @param replacement the value to replace the blocked phrase with
     * @return whether adding the replacement was successful
     */
    public boolean addBlockedPhrase(String phrase, String replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("Blocked phrase replacement cannot be null!");
        }
        blockedPhrases.put(phrase, replacement);
        return true;
    }
    
    /**
     * Removes a previously blocked phrase from the list of blocked phrases
     *
     * @param phrase the phrase to un-block
     * @return whether the phrase was previously blocked
     */
    public boolean removeBlockedPhrase(String phrase) {
        return blockedPhrases.remove(phrase) != null;
    }
    
    /**
     * Returns this User's list of blocked phrases
     *
     * @return this User's list of blocked phrases
     */
    public HashMap<String, String> getBlockedPhrases() {
        return blockedPhrases;
    }
    
    /**
     * Returns the User as a file-ready line
     *
     * @return the line
     */
    public abstract String toFileLine();
    
    /**
     * Checks if the recipient is valid to message for this user
     *
     * @param other the recipient
     * @return whether the recipient is a valid user to message
     */
    public boolean isValidRecipient(User other) {
        return ((other instanceof Seller && this instanceof Customer)
                || (other instanceof Customer && this instanceof Seller));
    }
    
    /**
     * Get the message chain associated with this user and the given user
     * This is a safe method (will never return null)
     *
     * @param other the other user
     * @return the message chain associated with both users
     */
    public Conversation getConversation(User other) {
        Conversation out = null;
        
        // Could restructure this to check based on getUserById(otherId)'s type
        if (other instanceof Seller) {
            for (int i = 0; i < MainListOfConversations.getMainListOfConversations().size(); i++) {
                Conversation currentConversation = MainListOfConversations.getMainListOfConversations().get(i);
                if (currentConversation.getSeller().equals(other)) {
                    out = currentConversation;
                }
            }
        } else if (other instanceof Customer) {
            for (int i = 0; i < MainListOfConversations.getMainListOfConversations().size(); i++) {
                Conversation currentConversation = MainListOfConversations.getMainListOfConversations().get(i);
                if (currentConversation.getCustomer().equals(other)) {
                    out = currentConversation;
                }
            }
        }
        
        if (out == null) {
            out = createConversation(this, other, false);
        }
        return out;
    }
    
    /**
     * Returns whether the two objects represent the same user
     *
     * @param o other
     * @return whether the two objects represent the same user
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        // Don't actually need to check anything else I don't think.
        return email.equals(user.email);
        // return id.equals(user.id) && email.equals(user.email)
        // // && name.equals(user.name) && password.equals(user.password) && messages.equals(user.messages);
    }
    
    /**
     * Block a user
     *
     * @param emailToBlock the user to block
     */
    public void blockUser(String emailToBlock) {
        blockedEmails.add(emailToBlock);
    }
    
    /**
     * Become invisible to the given user
     *
     * @param emailToVanishFrom the email of the user to become invisible to
     */
    public void becomeInvisibleToUser(String emailToVanishFrom) {
        invisToEmails.add(emailToVanishFrom);
    }
    
    /**
     * Returns the String equivalent of the dashboard
     *
     * @return the dashboard
     */
    public final String viewDashboard() {
        String stringToReturn = "";
        if (this instanceof Customer) {
            for (Conversation currentConversation : MainListOfConversations.getMainListOfConversations()) {
//                Conversation currentConversation = MainListOfConversations.getMainListOfConversations().get(i);
                if (currentConversation.getCustomer().equals(this)) {
                    stringToReturn += currentConversation.getSeller().getName() + ":\r\n"
                            + Seller.getAllStoresAsString() + "\r\n";
                }
            }
        } else if (this instanceof Seller) {
            for (Conversation currentConversation : MainListOfConversations.getMainListOfConversations()) {
                // int i = 0; i < MainListOfConversations.getMainListOfConversations().size(); i++
//                Conversation currentConversation = MainListOfConversations.getMainListOfConversations().get(i);
                if (currentConversation.getSeller().equals(this)) {
                    Customer c = currentConversation.getCustomer();
                    stringToReturn += c.getName() + "\tNumber of messages sent: "
                            + c.getNumberOfMessagesSent() + " ";
                }
            }
        }
        
        if (stringToReturn.length() > 0) {
            return stringToReturn.substring(0, stringToReturn.length() - 2);
        } else {
            return stringToReturn;
        }
        
    }
    
    /**
     * Sorts the dashboard somehow; may need a rework for parameters
     *
     * @return the dashboard, sorted
     */
    public abstract String sortDashboard();
    
    /**
     * Sends a message to the given User
     *
     * @param u       the User to message
     * @param message the message to send
     * @return whether sending was successful
     */
    public final boolean message(User u, String message) {
        if (isValidRecipient(u)) {
            getConversation(u).sendMessage(new Message(this, u, message));
            return true;
        }
        return false;
    }
    
    /**
     * Message the specified User
     *
     * @param email          the email of the User to message
     * @param messageContent the message to send
     * @return whether sending the message was successful
     */
    public boolean message(String email, String messageContent) {
        return message(getUser(email), messageContent);
    }
    
    /**
     * Returns an ArrayList&lt;User&gt; of all Users
     *
     * @return an ArrayList&lt;User&gt; of all Users
     */
    public ArrayList<User> getAllUsers() {
        Collection<User> set = USERS.values();
        return new ArrayList<>(set);
    }
    
    /**
     * Returns the number of messages sent by this User
     *
     * @return the number of messages sent by this User
     */
    public int getNumberOfMessagesSent() {
        int numMessages = 0;
        ArrayList<Conversation> temp = MainListOfConversations.getConversationsWithUser(this);
        for (Conversation conversation : temp) {
            numMessages += conversation.getNumberOfMessagesSentBy(this);
        }
        return numMessages;
    }
    
    /**
     * Checks number of messages with a specific user
     *
     * @param user - the other user being messaged
     * @return returns the number of messages sent with that user
     */
    public int getNumberOfMessagesSentTo(User user) {
        int numMessages = 0;
        ArrayList<Conversation> sharedConversations = MainListOfConversations.getConversationsWithUser(this);
        for (Conversation c : sharedConversations) {
            if (user instanceof Customer) {
                if (c.getCustomer().equals(user)) {
                    numMessages += c.getMessages().size();
                }
            } else if (user instanceof Seller) {
                if (c.getSeller().equals(user)) {
                    numMessages += c.getMessages().size();
                }
            }
            
        }
        return numMessages;
    }
    
    /**
     * Gets the most common word
     *
     * @return the most common word
     */
    public String getMostCommonWord() {
        ArrayList<String> wordArray = new ArrayList<>();
        String mostCommonWord = "";
        ArrayList<Conversation> temp = MainListOfConversations.getConversationsWithUser(this);
        
        for (Conversation conversation : temp) {
            for (int j = 0; j < conversation.getMessages().size(); j++) {
                String message = conversation.getMessages().get(j).getMessageContent();
                while (!message.equals("")) {
                    String currentWord = message.substring(0, message.indexOf(" "));
                    if (!wordArray.contains(currentWord)) {
                        wordArray.add(currentWord);
                        message = message.substring(message.indexOf("") + 1);
                    }
                }
            }
        }
        
        return mostCommonWord;
    }
    
    /**
     * Returns the list of ids that this User has marked to be invisible
     *
     * @return the list of invis ids
     */
    public ArrayList<String> getInvisEmails() {
        return invisToEmails;
    }
    
    /**
     * Returns the list of ids that this User has blocked
     *
     * @return the list of blocked ids
     */
    public ArrayList<String> getBlockedEmails() {
        return blockedEmails;
    }
    
    /**
     * Basic toString method
     *
     * @return String representation of this Object
     */
    @Override
    public String toString() {
        return "{\"type\":\"" + this.getClass() + "\",\"email\":\"" + email + "\",\"name\":\"" + name + "\"," +
                "\"password\":\"" + password + "\"}";
    }
    
}
