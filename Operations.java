/**
 * {@link Operation}<br />
 * An enum that represents the operation that represents a hint for a {@link Server} to use when processing data
 *
 * @author Mikk Sanborn, Eric Qian, Caitlin Wilson, Jimmy Lin, Javad Jafarov
 * @version 12/12/2022
 */
public enum Operation {
    Message, GetUser, Exit, Disconnect, UserExists, AllStoresAsString, GetAllSellers, GetSellerFromStore,
    ListCustomers, GetAllCustomers, GetConversationsWithUser, SetMessageContent, AddMessageToConversation,
    UserBlocksUser, UserInvisibleToUser, GetConversationWithUsers, CreateCustomer, CreateSeller, CreateMessage,
    CreateConversation, SellerAddStore, GetConversationWithUsersWithStore, SendMessageFromFile, SetUserName,
    SetUserPass, DeleteUserAccount
}
