/**
 * 
 * Interface for the database, a list of Database.java commands used in CommandInterpreter. 
 * CommandInterpreter uses this class to access the methods
 * rather than the Database.java.
 * 
 * @author hxm02u
 * @version 2.0.0
 *
 */
public interface IDatabase {
	
    /**
     * Gets the length of all of the messages in the database,
     * including the marked ones from the user logged in.
     * 
     * @return true if successful
     */
	int getMessagesLengthIncMarked();
	
    /**
     * Gets the length of the message at @param iMailID 
     * in the database, including the marked ones.
     * 
     * @return true if successful
     */
	int getMessageLength(int iMailID);
	
    /**
     * Method is called only when server crashes.
     * Resets the database by unlocking all of the users by changing
     * all of the tiLocked values to 0.
     * @return true if successful
     */
	boolean resetDatabase();

    /**
     * Checks the database to see if the @param user exists
     * @return true if successful
     */
	boolean userExists(String user);
	
    /**
     * Checks the authentication of the given @param user, @param password and
     * checks if the given user is locked.
     * @return true if successful.
     */
    boolean authenticate(String u, String p);
    
    /**
     * Gets all of the messages from the server from the user
     * logged in, String[]. 
     * 
     * @return msg, the array of messages from the server.
     * or @return null if unsuccessful.
     * 
     */
    String[] getMailMessages();

    /**
     * Marks for deletion the given message shown by @param iMailID.
     * @return true if successful
     */
	boolean markMessage(int iMailID);
	
    /**
     * Checks if the given message shown by @param iMailID is marked for deletion.
     * @return true if it is marked
     */
	boolean isMessageMarked(int iMailID);

    /**
     * Deletes all the message from the server that are marked for deletion
     * from the current user.
     * 
     * @return true if successful
     */
    boolean deleteMailMessages();

    /**
     * Gets the iMailID for the message at the @param msgRow.
     * @return iMailID for the row.
     * @return 0 if unsuccessful
     * 
     * @param msgRow
     */
    int getiMailIDFromRow(int msgRow);

    /**
     * Gets the UIDL for the message at the given @param iMailID
     * @return UIDL String relating to that message.
     * 
     * @param iMailID of the message
     * @return UIDL 
     */
	String getUIDL(int iMailID);
	
    /**
     * Toggles the lock for the logged in user.
     * 
     * @return true if successful
     * @param lock - true lock, false unlock
     */
	boolean toggleLock(boolean trueOn);
	
	/**
	 * Reset the marked messages for users.
	 * @param resetType,
	 * 			0 for a single user, the one logged in 
	 * 			1 for all users in the database
	 * @return true if successful
	 */
	boolean resetMarkedMessages(int resetType);
}