import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
/*
 * PART 3. CHANGES FROM PREVIOUS PARTS: 
 * --Pop3Server.java--
 * 1. Added a new function resetDatabase(), this resets the locks and marked messages 
 *   in the database. 
 *   REASON: Prevents an error where if a user is logged in and the server
 *   crashes, resetDatabase is called to clear all.
 *    
 * --Pop3Session.java--
 * 1. Removed any S: and C: print outs
 *    REASON: So mail clients like Thunderbird can use this program.
 * 2. Moved my socket.close() statement to a finally block.
 *    REASON: To fully close the socket if or if not a error happens.
 * 3. In that finally block as well added a CommandInterpreter method toggleLock(boolean type)
 * 	  REASON: To lock the used database so users trying the same user name and password wont be able
 * 	  to access the account.
 * 
 * --IDatabase.java--
 * 1. Added synchronize to every method
 *    REASON: To improve concurrency, so they can't be called twice and crash the program
 * 2. Added new methods which are new in the Database.java class, these are as follows:
	 * a. getMessagesLengthIncMarked();
	 * b. getMessageLength(int iMailID);
	 * c. resetDatabase();
	 * d. markMessage(int iMailID);
	 * e. isMessageMarked(int iMailID);
	 * f. deleteMailMessages();
	 * g. getiMailIDFromRow(int msgRow);
	 * h. toggleLock(boolean trueOn);
	 * i. resetMarkedMessages(int resetType);
	 * These are explained in IDatabase.java, reasons for adding them are in Database.java
 *
 * --CommandInterpreter.java--
 * 1. Added a method which creates a new database object if it is the first time running, else
 * 	  uses the one that is created. This getDatabase() method is called each time the object is created, the method returns a new database
 * 	  object if it doesn't already exist. Otherwise it returns that object.
 * 	  REASON: to stop multi-user access from failing.
 * 2. Removed the S: and C: print outs like above,
 *    REASON: So mail clients like Thunderbird can use this program.
 * 3. Changed the way the Method commandPass authenticates the user and locks the database
 *    REASON: To remove any unneeded database calls and implements locking
 * 4. Changed the commandStat method, so it checks if a message is marked for deletion and uses the getMessagesLength()
 *    rather than a .length command.
 * 	  REASON: To improve efficiency of the program and so the messages marked column in the database is used.
 * 5. The methods listed below, also checks if a message is marked now.
 * 		commandLIST
 * 		commandRETR
 * 		commandTOP
 * 		commandUIDL
 * 		commandRSET
 *    REASON: This implements messages being marked 
 * 6. Added a method called toggleLock, if the input is true it locks the mailbox for the logged in user
 *    or if the methods input is false, unlocks the mailbox
 *    REASON: This method is needed to activate the locking for the database
 * 7. commandDELE method uses improved authentication for marking the message deleted.
 *    REASON: This speeds up efficiency of the program.
 * 8. Changed the way commandTOP works, by printing out the header of the message
 *    REASON: So the TOP command matches the pop3 spec.
 * 9. Changed the way the LIST and UIDL commands in commandLIST and commandUIDL works.
 * 	  REASON: So the LIST and UIDL commands match the pop3 spec by printing out the following
 *    e.g. 
 *    >LIST 2
 *    >2 2722 octets
 * 
 * --Database.java--
 * The MockDatabase.java class from my Part 2 has been modified and renamed
 * with the above classes added. Reasons for changes are not included because
 * its a new class for part 3.
 * 
 */
/*
 * How the Entire program works:
 * The Pop3Server.java file runs, connecting to a server on the given address, in this case, it connects to a mysql server.
 * The Pop3Server then waits until an incoming client is received by the server. In this case, the user is then connected with
 * the database so they can enter commands. Each commands connects and receives different output from the database. Any database
 * calls are passed back to the client so they can access the information. e.g. Mail messages.
 * The user can view and delete messages from the server using these commands. 
 * When the user issues the quit command, the messages are deleted from the server that the user has marked.
 * 
 * -Handle crashes-
 * If the Pop3Server.java closes unexpectedly then the client will get a timeout exception.
 * On starting back up the server the mailbox of users are all unlocked allowing the users afterwards to still
 * access their mailboxes if they are logged in and the server fails.
 * 
 */

/**
 * When a user logs in they try to connect to a sql server using the information given
 * as final variables. If it fails the appropriate error messages are displayed.
 * Each method has simple database calls to get the information from the connected
 * sql database. The data is then returned or in the case of update commands in
 * deleting messages, returns a boolean, true if successful. 
 * 
 * 
 * @author hxm02u
 * @version 2.0.0
 */
public class Database implements IDatabase {

	private static Database database = null;
    private int usersiMaildropID;
    private static Connection connection = null;
    private static Statement statement;
    private String sql = "";
    
    private static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
    private static final String URL_NAME =  "jdbc:mysql://mysql.cs.nott.ac.uk/";
    private static final String DB_NAME = "hxm02u";
    private static final String SQL_USERNAME = "hxm02u";
    private static final String SQL_PASSWORD = "monday";

    /**
     * Constructor for the database class, which connects to the pop3 server using
     * the connection info above.
     */
    public Database() {
    	if(connection == null) {
    		/* Connects to the mysql database using the given jdbc driver */ 
			try {
				Class.forName(MYSQL_DRIVER);
			} catch (ClassNotFoundException e) {
				System.out.println("System MySQL JDBC Driver not found");
				e.printStackTrace();
				return;
			}
			try {
				connection = DriverManager
				.getConnection(URL_NAME + DB_NAME, SQL_USERNAME, SQL_PASSWORD);		 
			} catch (SQLException e) {
				System.out.println("<user failed to connect>");
				e.printStackTrace();
				return;
			}
			if (connection != null) {
			} else {
				System.out.println("<user failed to connect>");
				return;
			}
			try {
				statement = connection.createStatement();
			} catch (SQLException e) {
				System.out.println("<user failed to connect>");
				e.printStackTrace();
				return;
			}
    	}
    }
    
    /**
     * Returns the existing or create a new one.
     *
     * @return the database
     */
    synchronized public static Database getDatabase() {
    	if(database == null) {
    		Database.database = new Database();
    	} 
    	return Database.database;
    }
    
    
    /**
     * Gets the length of all of the messages in the database,
     * including the marked ones from the user logged in.
     * 
     * @return true if successful
     */
    synchronized public int getMessagesLengthIncMarked() {
    	sql = "SELECT Count(*) AS msgLen FROM m_Mail WHERE iMaildropID = " + usersiMaildropID;
		try {
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next()) {
				return rs.getInt("msgLen");
			}
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
	    return 0;
    }
    
    /**
     * Gets the length of the message at @param iMailID 
     * in the database, including the marked ones.
     * 
     * @return true if successful
     */
    synchronized public int getMessageLength(int iMailID) {
    	sql = "SELECT Count(*) AS msgLen FROM m_Mail WHERE iMaildropID = " + usersiMaildropID + " AND iMailID = " + iMailID;
		try {
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next()) {
				return rs.getInt("msgLen");
			}
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
	    return 0;
    }
    
    
    /**
     * Method is called only when server crashes.
     * Resets the database by unlocking all of the users by changing
     * all of the tiLocked values to 0.
     * 
     * @return true if successful
     */
    synchronized public boolean resetDatabase() {
		sql = "UPDATE m_Maildrop SET tiLocked = 0";
		try {
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
		return false;
	}
	
    /**
     * Checks the database to see if the @param user exists
     * @return true if successful
     */
    synchronized public boolean userExists(String userName) {
    	sql = "SELECT vchUsername FROM m_Maildrop WHERE vchUsername = '" + userName + "'";
    	try {
    		ResultSet rs = statement.executeQuery(sql);
			while (!rs.isBeforeFirst()) {
				/* If it doesn't exist */
				return false;
			}
			while (rs.next()) {
				if(rs.getString("vchUsername").equals(userName)) {
					return true;
				}
			}
    	} catch (SQLException e) {
    		System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
    	return false;
    }
    
    /**
     * Checks the database to see if the @param user And the @param password exists
     * @return true if successful
     */
    synchronized private boolean userPassExists(String user, String password) {
    	sql = "SELECT * FROM m_Maildrop"
				+ " WHERE vchUsername = " + "'" + user + "' AND vchPassword = " + "'" + password + "'";
    	try {
    		ResultSet rs = statement.executeQuery(sql);
			while (!rs.isBeforeFirst()) {
				return false;
			}
			while (rs.next()) {
				usersiMaildropID = rs.getInt("iMaildropID");
				return true;
			}
			
    	} catch (SQLException e) {
    		System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
    	return false;
    }
    
    /**
     * Checks the authentication of the given @param user, @param password and
     * checks if the given user is locked.
     * @return true if successful.
     */
    synchronized public boolean authenticate(String user, String password) {
        return (userPassExists(user, password) && !isUserLocked());
    }
    
    /**
     * Checks with the database to see if the current user logged in
     * has their mailbox locked.
     * 
     * @return true if it is locked.
     */
    synchronized private boolean isUserLocked() {
    	sql = "SELECT tiLocked FROM m_Maildrop WHERE iMaildropID = " + usersiMaildropID;
		try {
			statement.executeQuery(sql);
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				if(rs.getInt("tiLocked") == 1) {
					return true;
				}
			}   
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
		return false;
	}
    
    /**
     * Gets all of the messages from the server from the user
     * logged in, String[]. 
     * 
     * @return msg, the array of messages from the server.
     * or @return null if unsuccessful.
     * 
     */
    synchronized public String[] getMailMessages() {
    	sql = "SELECT txMailContent FROM m_Mail"
    					+ " WHERE iMaildropID = " + usersiMaildropID
    					+ " ORDER BY iMaildropID ASC";
    	try {
			ResultSet rs = statement.executeQuery(sql);
	    	int rowCount = 0;
			if (rs.last()) {
				rowCount = rs.getRow();
				rs.beforeFirst(); 
			}
			String[] msg = new String[rowCount];
			int i = 0;
			while (rs.next()) {
				msg[i] = rs.getString("txMailContent");
				i++;
			}
			return msg;
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
		return null;    
	}
	
    /**
     * Marks for deletion the given message shown by @param iMailID.
     * @return true if successful
     */
    synchronized public boolean markMessage(int iMailID) {
    	sql = "UPDATE m_Mail SET markedForDeletion = 1"
				+ " WHERE iMaildropID = " + usersiMaildropID
				+ " AND iMailID = " + iMailID;
		try {
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
		return false;
    }
    
   
    /**
     * Checks if the given message shown by @param iMailID is marked for deletion.
     * 
     * @return true if it is marked
     * @param iMailID of the message to be marked
     */
    synchronized public boolean isMessageMarked(int iMailID) {
    	sql = "SELECT markedForDeletion FROM m_Mail WHERE iMailID = " + iMailID;
		try {
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next()) {
				if(rs.getInt("markedForDeletion") != 0) { 
					return true; 
				}
			}
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
		return false;
    }
    
    /**
     * Deletes all the message from the server that are marked for deletion
     * from the current user.
     * 
     * @return true if successful
     */
    synchronized public boolean deleteMailMessages() {
		sql ="DELETE FROM m_Mail WHERE markedForDeletion = 1 AND iMaildropID = " + usersiMaildropID;
    	try {
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
		return false;     
    }
    
    /**
     * Gets the iMailID for the message at the @param msgRow.
     * @return iMailID for the row.
     * @return 0 if unsuccessful
     * 
     * @param msgRow
     */
    synchronized public int getiMailIDFromRow(int msgRow) {
    	sql = "SELECT iMailID FROM m_Mail WHERE iMaildropID = " + usersiMaildropID;
		try {
			ResultSet rs = statement.executeQuery(sql);
			int row = 0;
			while(rs.next()) {
				if(row == msgRow) {
					return rs.getInt("iMailID");
				}
				row++;
			}
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
	    return 0;
    }

    /**
     * Gets the UIDL for the message at the given @param iMailID
     * @return UIDL String relating to that message.
     * 
     * @param iMailID of the message
     * @return UIDL 
     */
    synchronized public String getUIDL(int iMailID) {
    	sql = "SELECT vchUIDL FROM m_Mail"
				+ " WHERE iMaildropID = " + usersiMaildropID
				+ " AND iMailID = " + iMailID;
		try {
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next()) {
				return rs.getString("vchUIDL");
			}
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
    	return null;
    }
    
    /**
     * Toggles the lock for the logged in user.
     * 
     * @return true if successful
     * @param lock - true lock, false unlock
     */
    synchronized public boolean toggleLock(boolean lock) {
    	int set = lock ? 1 : 0;
    	sql = "UPDATE m_Maildrop SET tiLocked = " + set + " WHERE iMaildropID = " + usersiMaildropID;
		try {
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}
		return false;
    }
    
	/**
	 * Reset the marked messages for users.
	 * @param resetType,
	 * 			0 for a single user, the one logged in 
	 * 			1 for all users in the database
	 * @return true if successful
	 */
    synchronized public boolean resetMarkedMessages(int resetType) {
		sql = "UPDATE m_Mail SET markedForDeletion = 0";
		if(resetType == 0) {
			sql += " WHERE iMaildropID = " + usersiMaildropID;
		} /* else for 1 (all users), don't add the sql line */
    	try {
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("Error in SQL Statement");
			e.printStackTrace();
		}	
    	return false;
	}
}
