package part2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class User extends Thread {

	// The user socket
	private static Socket userSocket = null;
	// The output stream
	private static PrintStream output_stream = null;
	// The input stream
	private static BufferedReader input_stream = null;

	private static BufferedReader inputLine = null;
	private static boolean closed = false;

	public static void setClose(boolean flag) {
		closed = flag;
	}

	public static void main(String[] args) throws Exception {

		// The default port.
		int portNumber = 8000;
		// The default host.
		String host = "localhost";

		/*
		 * Open a socket on a given host and port. Open input and output
		 * streams.
		 */
		if (args.length < 2) {
			System.out.println(
					"Usage: java User <host> <portNumber>\n" + "Now using host=" + host + ", portNumber=" + portNumber);
		} else {
			host = args[0];
			portNumber = Integer.valueOf(args[1]).intValue();
			System.out.println(
					"Usage: java User <host> <portNumber>\n" + "Now using host=" + host + ", portNumber=" + portNumber);
		}
		try {
			userSocket = new Socket(host, portNumber);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*
		 * If everything has been initialized then create a listening thread to
		 * read from the server. Also send any user’s message to server until
		 * user logs out.
		 */

		/*
		 * Keep on reading from the socket till we receive “### Bye …” from the
		 * server. Once we received that then we want to break and close the
		 * connection.
		 */
		User listenThread = new User();
		listenThread.start();

		while (true && !closed) {
			try {
				inputLine = new BufferedReader(new InputStreamReader(System.in));
				String s;
				if ((s = inputLine.readLine()) != null) {
					output_stream = new PrintStream(userSocket.getOutputStream(), true);
					output_stream.println(s);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		inputLine.close();
		output_stream.close();
	}

	public void run() {
		while (true && !closed) {
			try {
				input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
				String str;
				if ((str = input_stream.readLine()) != null) {
					if (str.equals("exit-1_SpeCiALIdENtiFer")) {
						closed = true;
						break;
					}
					System.out.println(str);
				}
				Thread.sleep(50);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			input_stream.close();
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
