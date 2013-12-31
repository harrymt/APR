

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class G51OOPInput {

	private static BufferedReader brd = new BufferedReader(
			new InputStreamReader(System.in));
	private static String input;

	/**
	 * This will return an integer from the input line, prepended or appended
	 * whitespace is ignored.
	 * 
	 * @return The integer parsed from the whole line of input,
	 *         Integer.MAX_VALUE is returned in error
	 */
	public static int readInt() {
		int retVal = Integer.MAX_VALUE;
		try {
			input = brd.readLine();
			retVal = Integer.parseInt(input.trim());
		} catch (Exception e) {
			System.out.println("Exception: Could not parse \"" + input
					+ "\" as an int");
		}
		return retVal;
	}

	/**
	 * getChar() works the same as C's getChar(), the stream is flushed to the
	 * end of the line. So from the input of <code>a b</code> only
	 * <code>a</code> obtainable.
	 * 
	 * @return The first character in the input stream, if unable the null
	 *         character is returned
	 */
	public static char getChar() {
		int retVal = (char) 0;
		try {
			retVal = brd.read();
			if (retVal < 32)
				throw new Exception();
			brd.readLine();
		} catch (Exception e) {
			System.out
					.println("Exception: Could not getChar() from input stream");
		}
		return (char) retVal;
	}

	/**
	 * This will return a double from the input line, prepended or appended
	 * whitespace is ignored.
	 * 
	 * @return The double parsed from the whole line of input, Double.MAX_VALUE
	 *         is returned in error
	 */
	public static double readDouble() {
		double retVal = Double.MAX_VALUE;
		try {
			input = brd.readLine();
			retVal = Double.parseDouble(input.trim());
		} catch (Exception e) {
			System.out.println("Exception: Could not parse \"" + input
					+ "\" as a double");
		}
		return retVal;
	}

	/**
	 * This will return a float from the input line, prepended or appended
	 * whitespace is ignored.
	 * 
	 * @return The float parsed from the whole line of input, Float.MAX_VALUE is
	 *         returned in error
	 */
	public static float readFloat() {
		float retVal = Float.MAX_VALUE;
		try {
			input = brd.readLine();
			retVal = Float.parseFloat(input.trim());
		} catch (Exception e) {
			System.out.println("Exception: Could not parse \"" + input
					+ "\" as a float");
		}
		return retVal;
	}

	/**
	 * This will return a short from the input line, prepended or appended
	 * whitespace is ignored.
	 * 
	 * @return The short parsed from the whole line of input, Short.MAX_VALUE is
	 *         returned in error
	 */
	public static short readShort() {
		short retVal = Short.MAX_VALUE;
		try {
			input = brd.readLine();
			retVal = Short.parseShort(input.trim());
		} catch (Exception e) {
			System.out.println("Exception: Could not parse \"" + input
					+ "\" as a short");
		}
		return retVal;
	}

	/**
	 * This will return a long from the input line, prepended or appended
	 * whitespace is ignored.
	 * 
	 * @return The long parsed from the whole line of input, Long.MAX_VALUE is
	 *         returned in error
	 */
	public static long readLong() {
		long retVal = Long.MAX_VALUE;
		try {
			input = brd.readLine();
			retVal = Long.parseLong(input.trim());
		} catch (Exception e) {
			System.out.println("Exception: Could not parse \"" + input
					+ "\" as a long");
		}
		return retVal;
	}

	/**
	 * Will build a string from the data stream until a new line character is reached.
	 * @return The input string or NULL if an error occurs
	 */
	public static String readString() {
		String retStr = null;
		try {
			retStr = brd.readLine();
		} catch (Exception e) {
			System.out.println("Exception: Could not read a string from input");
		}
		return retStr;
	}

	/**
	 * Prompt the user for input given the required input.
	 * @param s The string to be used as the prompt
	 */
	public static void prompt(String s) {
		System.out.print(s + " ");
	}

}
