import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * {@link Message}<br />
 * A class that represents a message sent in this messaging program
 *
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 * @version 12/12/2022
 */
public class Message implements Serializable {
    private String senderEmail;
    private String receiverEmail;
    private boolean canSenderView;
    private boolean canReceiverView;
    private String messageContent;
    private long timeSent;
    private Conversation parent;
    
    /**
     * Message constructor
     *
     * @param sender         the sender of this message
     * @param receiver       the receiver of this message
     * @param messageContent the message content
     */
    public Message(User sender, User receiver, String messageContent) {
        this(sender.getEmail(), receiver.getEmail(), messageContent);
    }
    
    /**
     * Message constructor
     *
     * @param senderEmail    the sender of this message
     * @param receiverEmail  the receiver of this message
     * @param messageContent the message content
     */
    public Message(String senderEmail, String receiverEmail, String messageContent) {
        this(senderEmail, receiverEmail, true, true, messageContent, System.currentTimeMillis());
    }
    
    /**
     * Message constructor
     *
     * @param senderEmail     the sender of this message
     * @param receiverEmail   the receiver of this message
     * @param canSenderView   can the sender view this message
     * @param canReceiverView can the receiver view this messsage
     * @param messageContent  the message content
     * @param timeSent        the time the message was sent
     */
    protected Message(String senderEmail, String receiverEmail, boolean canSenderView,
                      boolean canReceiverView, String messageContent, long timeSent) {
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.canSenderView = canSenderView;
        this.canReceiverView = canReceiverView;
        this.messageContent = messageContent;
        this.timeSent = timeSent;
        
        this.parent = null;
    }
    
    /**
     * Sets the parent {@link Conversation}
     *
     * @param conversation the {@link Conversation} this {@link Message} belongs to
     * @return the previous parent (should be null)
     */
    protected Conversation setParent(Conversation conversation) {
        Conversation out = this.parent;
        parent = conversation;
        return out;
    }
    
    /**
     * Returns the parent {@link Conversation}
     *
     * @return the parent {@link Conversation}
     */
    protected Conversation getParent() {
        return this.parent;
    }
    
    /**
     * Returns the message sender's email
     *
     * @return the message sender's email
     */
    public String getSenderEmail() {
        return senderEmail;
    }
    
    /**
     * Changes the sender of this message
     * don't use
     *
     * @param senderEmail the new email
     */
    private void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public User getSender() {
        return User.getUser(getSenderEmail());
    }
    
    /**
     * Returns the message receiver's email
     *
     * @return the message receiver's email
     */
    public String getReceiverEmail() {
        return receiverEmail;
    }
    
    /**
     * Sets the email of the receiver of this message
     * don't use
     *
     * @param receiverEmail the email to set
     */
    private void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }
    
    public User getReceiver() {
        return User.getUser(getReceiverEmail());
    }
    
    /**
     * Returns whether the sender of this message can view it
     *
     * @return whether the sender of this message can view it
     */
    public boolean canSenderView() {
        return canSenderView;
    }
    
    /**
     * Sets whether the sender of this message can view it
     *
     * @param canSenderView the value to set
     */
    public void setCanSenderView(boolean canSenderView) {
        this.canSenderView = canSenderView;
    }
    
    /**
     * Returns whether the receiver of this message can view it
     *
     * @return whether the receiver of this message can view it
     */
    public boolean canReceiverView() {
        return canReceiverView;
    }
    
    /**
     * Sets whether the receiver of this message can view it
     *
     * @param canReceiverView the value to set
     */
    public void setCanReceiverView(boolean canReceiverView) {
        this.canReceiverView = canReceiverView;
    }
    
    /**
     * Returns the message content
     *
     * @return the message content
     */
    public String getMessageContent() {
        return messageContent;
    }
    
    /**
     * Sets the message content
     * Shouldn't need to be used?
     *
     * @param messageContent the message content to set
     */
    protected void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    
    /**
     * Returns the time this message was sent
     *
     * @return the time this message was sent
     */
    public long getTimeSent() {
        return timeSent;
    }
    
    /**
     * Sets the time this message was sent
     * Shouldn't need to be used unless editing shows time
     *
     * @param timeSent the time this message was sent
     */
    public void setTimeSent(long timeSent) {
        this.timeSent = timeSent;
    }
    
    /**
     * Returns the message content replacing words blocked by the given User with their replacements
     *
     * @param user the User with blocked phrases
     * @return the filtered message content
     */
    public String getMessageContentFiltered(User user) {
        return getMessageContentFiltered(user.getBlockedPhrases());
    }
    
    /**
     * Returns the message content replacing blockedPhrases with their replacements
     *
     * @param blockedPhrases the blocked phrases to replace
     * @return the filtered message content
     */
    public String getMessageContentFiltered(HashMap<String, String> blockedPhrases) {
        String out = messageContent;
        
        if (blockedPhrases != null) {
            Set<String> toFilterWords = blockedPhrases.keySet();
            for (String toFilter : toFilterWords) {
                out = out.replaceAll(toFilter, blockedPhrases.get(toFilter));
            }
        }
        
        return out;
    }
    
    /**
     * Returns a {@link String} representation of this {@link Message}
     *
     * @return a {@link String} representation of this {@link Message}
     */
    @Override
    public String toString() {
        return String.format("{\"type\":\"%s\",\"time\":\"%d\",\"sender\":\"%s\",\"receiver\":\"%s\"," +
                        "\"content\":\"%s\"}",
                this.getClass(), timeSent, senderEmail, receiverEmail,
                messageContent.replace("\n", "\\\\n").replace("\"", "\\\\\""));
    }
    
    /**
     * Returns whether this and o are equal
     *
     * @param o the {@link Object} to compare
     * @return whether this and o are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return canSenderView == message.canSenderView && canReceiverView == message.canReceiverView &&
                timeSent == message.timeSent && senderEmail.equals(message.senderEmail) &&
                receiverEmail.equals(message.receiverEmail) && messageContent.equals(message.messageContent);
    }
    
}
