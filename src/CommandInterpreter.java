/**
 * 
 * Class which handles the input of the user, by accessing the database and selecting
 * calls based on their arguments. Returns the output to the user.
 * 
 * @author hxm02u
 * @version 2.0.0
 * 
 */

public class CommandInterpreter {

    public State state = State.AUTHORIZATION; // start the initial state
    private IDatabase idb = Database.getDatabase();
    
    /**
     * Gets the database object so the CommandInterpreter can 
     * call the database methods.
     * @return the database object
     */
    public IDatabase getDatabase() {
		return idb;
	}

	private static final String CRLF = "\n";
    private String userLoggedIn = "";
    
    /**
     * Return the current state
     * 
     * @return what state the commandInterpreter is in
     */
    public State getState() {
        return state;
    }
    /**
     * Set the current state to another.
     * 
     * @param state
     */
    public void setState(State s) {
        state = s;
    }
        
    /**
     * Handles the following keywords:
     *
     * USER PASS QUIT STAT LIST RETR DELE NOOP RSET TOP UIDL
     * 
     * Splits the input into the keywords above and arguments which follow the keyword.
     * Based on the keyword different methods are called corresponding to the keyword and
     * each return a string which uses database calls to retrieve data.
     *
     * @param input string containing a keyword and argument(s)
     * @return answer to the keyword inputed
     */
    public String handleInput(String input) {
        if (input == null || input.length() < 3) { state = State.QUIT; return createERR(input);}
        
        String[] inputSplit = (input + " ").split(" ", 2); 
        String keyword = (inputSplit[0].toUpperCase()).trim();
        String arguments = (inputSplit[1]).trim(); 
        
        switch (state) {
        case AUTHORIZATION: 
            switch (keyword) {
            case "USER": return commandUSER(((arguments + " ").split(" ", 2))[0]);
            
            case "PASS":
                if(!userLoggedIn.equals("")) { return commandPASS(arguments);
                } else { return createERR("need a USER first. " + input); }
                
                
            case "QUIT": state = State.QUIT; break;
                
            
            default : userLoggedIn = ""; return createERR("enter USER (PASS) or QUIT " + input);
            }   
            
        
        case TRANSACTION:
            switch (keyword) {
                case "STAT": return commandSTAT(-1);
                
                case "LIST": return commandLIST(arguments);
                
                case "RETR":return commandRETR(arguments);
                
                case "DELE": return commandDELE(arguments);
                
                case "NOOP": return createOK("");
                
                case "RSET":return commandRSET();
                
                case "TOP":return commandTOP(arguments);
                
                case "UIDL":return commandUIDL(arguments);
                
                case "QUIT": state = State.UPDATE; break;
                
                default: return createERR("enter a valid command or QUIT " + input);
            }
            
            
        case UPDATE:
            switch (keyword) {
                case "QUIT": if(idb.deleteMailMessages()) { state = State.QUIT; return commandQUIT();} else { break; }
                
                default: return createERR("Enter a valid command or QUIT"  + input);
            }
            
        default: state = State.QUIT; return commandQUIT();
        
        }
    }
    
    /**
     * Standard function to print out an error message.
     * 
     * @param input
     * @return +ERR added to input
     */
    public String createERR(String input) {
        return ("-ERR " + input + CRLF);
    }
    
    /**
     * Standard function to print out an OK message.
     * 
     * @param input
     * @return +OK added to input
     */
    public String createOK(String input) {
        return ("+OK " + input + CRLF);
    }
    
    /**
     * Processes the USER pop3 command returns a OK message if 
     * the user is found.
     * 
     * @param user to check if exists or logged in
     * @return confirmation string if successful
     */
    public String commandUSER(String user)   {
        if (idb.userExists(user)) {
            userLoggedIn = user;
            return createOK(user + " would I say no? USER " + user);
        } else {
        	return createERR("never heard of mailbox user: " + user + " USER " + user);
        }
    }
    
    /**
     * If a user command is successful then the PASS command is used.
     * This uses the userLoggedIn variable to define which user is logged in.
     * This is good for tracking of who is in the system and also means that this function
     * can log in using the given password and the user from that variable.
     * 
     * @param password for the user logged in
     * @return confirmation string if successful
     */
    public String commandPASS(String pass) {
        if (idb.userExists(userLoggedIn) && idb.authenticate(userLoggedIn, pass)) {
            this.state = State.TRANSACTION;
            idb.toggleLock(true);
             return createOK(userLoggedIn + "'s Mailbox. PASS " + pass);
        } else { userLoggedIn = ""; return createERR("wrong password or mailbox locked, PASS " + pass);}
    }
 
    /**
     * The quit command used in either the Authorisation state or Update state.
     * Simply displays a OK statement notifying the user that they are leaving.
     * 
     * @return string confirmation message added
     */
    public String commandQUIT() {
        return createOK("leaving mailbox QUIT");
    }
    
    /**
     * Standard function to convert a string into an int with error handling.
     * Returns -1 on failure.
     * 
     * @param str to be converted
     * @return int, after conversion.
     * 			If error returns -1
     */
    public int convertStrToInt(String str) {
        int output = 0;
        try{output = Integer.parseInt(str.trim());} catch (Exception e) {
            output = -1;
        }
        return output;
    }
    
    /**
     * Stat command returns a number of messages and their size in Octets.
     * If a message number is 0 then prints out everything.
     * or a messageNumber can be specified and prints only that message size. 
     * Message needs to be commandSTAT(index -1)
     * 
     * @param the message number, or -1 prints all messages
     * @return if @param -1 then all msgs, else prints given messageNumber
     */
    public String commandSTAT(int messageNumber) {
        String[] messages = idb.getMailMessages();
        int mbSizeOctets = 0;
        int msgNum = 0;
        if(messageNumber != -1) {return messages[messageNumber].length() + mbSizeOctets +  " octets";}
        for (int i = 0; i < idb.getMessagesLengthIncMarked(); i++) {
        	if(!idb.isMessageMarked(idb.getiMailIDFromRow(i))){
        		mbSizeOctets += messages[i].length();
        		msgNum++;
        	}
        }
        return createOK(msgNum + " messages ("  + mbSizeOctets +  " octets)");
    }
    
    /**
     * List command, lists the number of messages and each of their size in octets
     * 
     * @param the row number relating to the message
     * @return all of the messages or the given message at the row
     */
    public String commandLIST(String sRow) {
        String[] messages = idb.getMailMessages();
        int mailNum = messages.length;
        String output = "";
        int row = convertStrToInt(sRow);
        int start = 0;
        if(sRow.length() == 0) {
            output = commandSTAT(-1);
            row = mailNum;
        } else {
            if (row == -1) {return createERR("not a valid number: " + "\'" + sRow + "\' LIST " + sRow);}
            if(idb.isMessageMarked(idb.getiMailIDFromRow(row - 1))) {return createERR("Msg deleted. LIST " + sRow);}
            start = row - 1;
        }
        if(row > mailNum) { return createERR("no such message. LIST " + sRow);}
        
        for (int j = start; j < row; j++) {
        	if(!idb.isMessageMarked(idb.getiMailIDFromRow(j))){
        		output += ((j + 1) + " " + commandSTAT(j) + CRLF);
        	}
        }
        return output + "." + CRLF;
    }
    /**
     * 
     * Prints out the message at the given row index including the header.
     * 
     * @param row index
     * @result a confirmation msg if successful
     */ 
    public String commandRETR(String rowIndex) {
        int index = convertStrToInt(rowIndex); if(index == -1) {return createERR("please enter a valid message number. RETR " + rowIndex);}
        
        String[] messages = idb.getMailMessages();
        int mailNum = messages.length;
        if(idb.isMessageMarked(idb.getiMailIDFromRow(index - 1))) {return createERR("Msg deleted. RETR " + rowIndex);}
        
        if(index > mailNum || index == 0) { return createERR("no such message RETR " + rowIndex);}
        String output = createOK(commandSTAT(index - 1) + "RETR indexString");
        
        String[] splitM = messages[index - 1].split(CRLF);
        for (int j = 0; j < splitM.length; j++) {
            output += (splitM[j] + CRLF);
        }
        return output + "." + CRLF;
    }
    /**
     * Shows the first 10 lines of message header and then prints the number of
     * lines in the given message.
     * @param msg n, message row number, and number of lines
     * @return Shows the number of lines and header to the given message
     */
    public String commandTOP(String arguments) {
        // Splits the input down to the message number and number of lines.
        String[] argSplit = (arguments + " ").split(" ", 2); 
        String msgs = (argSplit[0].toUpperCase()).trim();
        String ns = (argSplit[1]).trim(); 
        int msg = convertStrToInt(msgs); if(msg == -1) {return createERR("no such message TOP " + arguments);} 
        int n = convertStrToInt(ns); if(n == -1){ return createERR("please enter a valid line number TOP " + arguments);}
        
        if(idb.isMessageMarked(idb.getiMailIDFromRow(msg - 1))) {return createERR("Msg deleted. TOP " + arguments);}
        
        String[] messages = idb.getMailMessages();
        String output = createOK("TOP " + arguments);
        
        if(msg == 0 || msg > messages.length) { return createERR("no such message, only " + messages.length + " messages in maildrop TOP " + arguments);}        
        
        String[] msgHeadersSplit = messages[msg - 1].split(CRLF + CRLF, 2);
        String[] msgHeaders = msgHeadersSplit[0].split(CRLF);
        String[] messageWithoutHeader = msgHeadersSplit[1].split(CRLF , n + 1);
        for (int i = 0; i < msgHeaders.length &&  i < 10; i++) {
			output += (msgHeaders[i] + CRLF);
		}
        output += (CRLF + CRLF);
        for (int i = 0; i < messageWithoutHeader.length - 1; i++) {
            output += ( messageWithoutHeader[i] + CRLF);
        }
        return output + "." + CRLF;
    }
    
    
    /**
     * Delete the message at the given index i. 
     * 
     * @param index of the message to be deleted
     * @return output, confirmation or error if msg exists
     */
    public String commandDELE(String indexString) {
        int index = convertStrToInt(indexString); if(index == -1){return createERR("please enter a valid message number. DELE " + indexString);}
        
        String[] messages = idb.getMailMessages();
        if(messages.length < index || index == 0) { return createERR("no such message, only " + messages.length + " messages in maildrop DELE " + indexString);}
        
        int iMailID = idb.getiMailIDFromRow(index - 1);
        
        if(!idb.isMessageMarked(iMailID)) {
        	idb.markMessage(iMailID);
        	return createOK("message " + index + " deleted. DELE " + indexString);
        } else {
        	return createERR("message " + index + " already deleted. DELE " + indexString);
        }
        
    }   
    
    
    /**
     * Resets all of the messages that are marked for deletion.
     * To not marked for deletion.
     * 
     * @return Stat printout
     */
    public String commandRSET() {
        if(idb.resetMarkedMessages(0)) {
        	return commandSTAT(-1);
        } else {
        	return null;
        }
    }
    
    
    /**
     * Gets the UIDL representing the row id of the message
     * 
     * @param index of the uidl row [optional]
     * @return the uidl at the index given or if none, lists all
     */
    public String commandUIDL(String indexString) { 
        String[] messages = idb.getMailMessages();
        int mailNum = messages.length;
        String output = createOK("");
        int index = convertStrToInt(indexString);
        int start = 0;
        if(indexString.length() == 0) {
            output = commandSTAT(-1);
            index = mailNum;
        } else {
            if(index == -1) {return createERR("not a valid number: " + "\'" + indexString + "\'");}
            if(idb.isMessageMarked(idb.getiMailIDFromRow(index - 1))) {return createERR("Msg deleted. UIDL " + indexString);}
            start = index - 1;
        }
        if(index > mailNum) { return createERR("no such message, only " + mailNum + " messages in maildrop");}
        
        
        for (int j = start; j < index; j++) {
           output += ((j + 1) + " " + idb.getUIDL(idb.getiMailIDFromRow(j)) + CRLF);
        }
        
        return output + "." + CRLF;
    }
    
    /**
     * Unlocks and Locks the database on exit/entrance of a user logged in and 
     * if an error happens
     * 
     * @param locked true for lock, false unlock
     */
    public void toggleLock(boolean locked) {
    	idb.toggleLock(locked);
    }

}
