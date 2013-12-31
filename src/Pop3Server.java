import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
/**
 * Starts a mail server using the POP3 protocol. 
 * Needs 1 or 2 arguments, 
 * If 1 argument the port number, uses a default timeout value of 60 minutes
 * If 2 arguments the port number then the timeout in seconds
 * Listens on the port specified, normally 110 for pop3 until a client connects. 
 * Then creates a new session for each client connected.
 * Handles multi user support and database crashes.
 * 
 * @author hxm02u
 * @version 2.0.0
 * 
 */
public class Pop3Server {
    public final ServerSocket serverSocket = null;
    
    /* 60 minute. socket read timeout */
    private static final int DEFAULT_TIMEOUT = 600 * 1000;
    
    /**
     * Standard function to convert a string into an int with error handling.
     * Returns -1 on failure.
     * 
     * @param str to be converted
     * @return result as an int, after conversion
     */
    private static int convertStrToInt(String str) {
        int result = 0;
        try {
            result = Integer.parseInt(str.trim());
        } catch (Exception e) {
            result = -1;
        }
        return result;
    }

    /**
     *  Listens to the given port for any client connections.
     *  Then creates a new session object for each new client connected.
     *  Handles client crashes in a finally catch statement.
     *  
     *  @param Can accept multiple arguments like:
     *            >java Pop3Server 110 600. java Pop3Server <port> <timeout> 
     *         or >java Pop3Server 110      java Pop3Server <port>.
     *  @result program is returned if arguments are invalid
     */
    public static void main(String[] args) {
    	int timeout = DEFAULT_TIMEOUT;
    	
        if (!(args.length == 2 || args.length == 1)) {
            System.err.println("Usage: java Pop3Server <port number> <timeout>");
            return;
        }
        
    	if(args.length == 2) {
    		timeout = convertStrToInt(args[1]) * 1000;
            if (timeout == -1) {
                System.err.println("-ERR Please enter a valid timeout value in seconds");
                return;
            }
       	}
    	
        int port = convertStrToInt(args[0]);
        if (port == -1) {
            System.err.println("-ERR Please enter a valid port number");
            return;
        }
  
        /* Listening variable can be used to leave the program with a simple if statement */
        boolean listening = true;

        /* On startup and server crashes, resets the locks and marked messages in the database */
        resetDatabase();
        
        /* Creates a server socket with the given port number */
        try (ServerSocket serverSocket = new ServerSocket(port)) { 
        	
            System.out.println("<wait for connection on TCP port " + port + ">");
            while (listening) {
            	/* Creates a new thread with the new class Pop3Session.*/
            	/* Program waits here until a client connects with the given serverSocket */
                new Thread (new Pop3Session(timeout, serverSocket.accept())).start();
            }
        } catch (SocketException se) {
            System.err.println("Could not listen on port " + port);
            return;
        } catch (IOException e) {
            System.err.println("IO Error");
            return;
        }
    }
    
    /**
     * On startup method is called. This resets the locks and marked messages 
     * in the database. Prevents an error where if a user is logged in and the server
     * crashes, resetDatabase is called to clear all.
     */
    private static void resetDatabase() {
    	IDatabase tempDb = new Database();
    	tempDb.resetDatabase();
    	tempDb.resetMarkedMessages(1);
    }
}
