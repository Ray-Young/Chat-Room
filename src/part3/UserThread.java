package part3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;

class UserThread extends Thread {

	private String userName = null;
	private BufferedReader input_stream = null;
	private PrintStream output_stream = null;
	private Socket userSocket = null;
	private final ArrayList<UserThread> threads;

	// only relevant for Part IV: adding friendship
	ArrayList<String> friends = new ArrayList<String>();
	ArrayList<String> friendrequests = new ArrayList<String>(); // keep track of
																// sent friend
																// requests
	//

	public UserThread(Socket userSocket, ArrayList<UserThread> threads) {
		this.userSocket = userSocket;
		this.threads = threads;
	}

	public void logout() {
		// Anyone must wait until the previous user logout, it's by design
		synchronized (UserThread.class) {
			try {
				for (UserThread thread : threads) {
					thread.output_stream = new PrintStream(thread.userSocket.getOutputStream(), true);
					thread.output_stream.println("### Bye <" + userName + "> ###");
				}
				Thread.sleep(100);

				output_stream = new PrintStream(userSocket.getOutputStream(), true);
				output_stream.println("exit-1_SpeCiALIdENtiFer");
				Thread.sleep(1000);

				input_stream.close();
				output_stream.close();
				userSocket.close();
				for (int i = 0; i < threads.size(); i++) {
					if (threads.get(i) == this) {
						threads.remove(i);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isLegalUserName(String s) {
		for (UserThread thread : threads) {
			// Check duplicate username
			if (thread.userName != null && thread.userName.equals(s)) {
				output_stream.println(s + " already exists, please pick another name");
				return false;
			}
			// Check illegal username
			if (s.contains("@") || s.contains(" ")) {
				output_stream.println(s + " is not a legal name, '@' and space are not allowed in name");
				return false;
			}
		}
		return true;

	}

	public void addUser() {
		try {
			output_stream = new PrintStream(userSocket.getOutputStream(), true);
			while (userName == null) {
				output_stream.println("Enter your name: ");
				String s;
				input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
				if ((s = input_stream.readLine()) != null) {
					// Check duplicate user name
					if (!isLegalUserName(s)) {
						Thread.sleep(100);
						continue;
					}
					userName = s;
					for (UserThread thread : threads) {
						// The next thread need wait until the previous finished
						synchronized (thread) {
							thread.output_stream = new PrintStream(thread.userSocket.getOutputStream(), true);
							thread.output_stream.println("Welcome " + userName + " to our chat room");
						}
					}
				}
			}
		} catch (

		Exception e) {
			e.printStackTrace();
		}

	}

	public void unicast(String s) {
		String message = s.split("\\s+");
		try {
			for (UserThread thread : threads) {
				// The next thread need wait until the previous finished
				synchronized (thread) {
					thread.output_stream = new PrintStream(thread.userSocket.getOutputStream(), true);
					thread.output_stream.println("<" + userName + ">: " + s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public void broadcast(String s) {
		try {
			for (UserThread thread : threads) {
				// The next thread need wait until the previous finished
				synchronized (thread) {
					thread.output_stream = new PrintStream(thread.userSocket.getOutputStream(), true);
					thread.output_stream.println("<" + userName + ">: " + s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while (true) {
			try {
				addUser();

				input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
				String s;
				if ((s = input_stream.readLine()) != null) {
					if (s.toLowerCase().equals("logout")) {
						logout();
						return;
					} else {
						broadcast(s);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
