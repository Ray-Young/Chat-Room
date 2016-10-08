package part3;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/*
 * A chat server that delivers public and private messages.
 */
public class Server {

	// Create a socket for the server
	private static ServerSocket serverSocket = null;
	// Create a socket for the user
	private static Socket userSocket = null;
	// Maximum number of users
	private static int maxUsersCount = 5;
	// An array of threads for users
	private static ArrayList<UserThread> threads = null;

	public static void main(String args[]) throws Exception {

		// The default port number.
		int portNumber = 8000;
		if (args.length < 1) {
			System.out.println("Usage: java Server <portNumber>\n" + "Now using port number=" + portNumber + "\n");
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			System.out.println("Usage: java Server <portNumber>\n" + "Now using port number=" + portNumber + "\n");
		}
		serverSocket = new ServerSocket(portNumber);
		threads = new ArrayList<UserThread>();

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
		while (true) {
			userSocket = serverSocket.accept();
			if (threads.size() < maxUsersCount) {
				threads.add(new UserThread(userSocket, threads));
				threads.get(threads.size() - 1).start();
			}
			else if (threads.size() == maxUsersCount) {
				PrintStream output_stream = new PrintStream(userSocket.getOutputStream(), true);
				output_stream.println("Currently the server is full loaded, please connect later");
				Thread.sleep(100);
				output_stream.println("exit-1_SpeCiALIdENtiFer");
			}
		}
	}
}