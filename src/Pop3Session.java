import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
/**
 * 
 * This class prints out the output after processing the users input with CommandInterpreter.
 * Handles the timing out of users and server crashes.
 * 
 * @author hxm02u
 * @version 2.0.0
 *
 */
public class Pop3Session implements Runnable {
    private Socket socket = null;
    int timeout;
    
    /**
     * Constructor of the session, class is created each time a new
     * client connects, so a new socket needs to be created with the timeout value.
     * 
     * @param timeout
     * @param socket
     */
    public Pop3Session(int timeout, Socket socket) {
        this.timeout = timeout;
        this.socket = socket;
    }
    /**
     * Standard function to print out an message to the Pop3Server not the client
     * 
     * @param the input that needs to be printed
     */
    public void serverPrint(String input) {
        System.out.print(input + "\r\n");
    }
    
    /**
     * Method which handles the input from the user and processes socket time outs.
     * Prints out the processed user input.
     * 
     */
    public void run() {
    	CommandInterpreter ci = new CommandInterpreter();

    	/* Uses brackets to close the readers and writers if they fail instead of using in.close() */
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) 
            {
        	
        	/* Set the timeout to the given value */
			socket.setSoTimeout(this.timeout);
                    
            out.println("+OK POP3 server ready on port <" + socket.getLocalPort()  +"> "); 
            
            String outputLine = "";
            while (!(ci.getState() == State.QUIT)) {
                out.printf("");    
                outputLine = ci.handleInput(in.readLine());
                out.print(outputLine);
            }
            out.close();
            in.close();
        } catch (SocketTimeoutException se) {
        	serverPrint("Connection timed out");
        	ci.getDatabase().resetMarkedMessages(0);
        	
        } catch (IOException e) {
            serverPrint("IO Error");
        } finally {
        	ci.toggleLock(false);
            try {
    			socket.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    			serverPrint("IO Error");
    		}
        }
    }
}