package part4;

import java.io.BufferedReader;
import java.io.IOException;
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

	public boolean isFriendReply(String s) {
		String[] message = s.split("\\s+");
		if (message.length > 1 && message[1].equals("#friends")) {
			return true;
		}
		return false;
	}

	public boolean isFriendRequest(String s) {
		String[] message = s.split("\\s+");
		if (message.length > 1 && message[0].equals("#friendme")) {
			return true;
		}
		return false;
	}

	public boolean isFriendRemove(String s) {
		String[] message = s.split("\\s+");
		if (message.length > 1 && message[1].equals("#unfriend")) {
			return true;
		}
		return false;

	}

	public boolean isFriend(String s) {
		String[] message = s.split("\\s+");
		String user = getUnicastUserName(message);
		if (!isUserExist(user)) {
			return false;
		}
		for (String friend : friends) {
			if (friend.equals(user)) {
				return true;
			}
		}
		try {
			output_stream = new PrintStream(userSocket.getOutputStream(), true);
			output_stream.println("You are not friends with " + user);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public boolean isFriendRelated(String s) throws InterruptedException {
		String[] message = s.split("\\s+");
		if (isFriendRequest(s)) {
			String user = message[1].substring(1, message[1].length());
			if (!isDuplicateRequest(user)) {
				sendFriendRequest(user);
			} else if (isFriend(user)) {
				sendMessage("You are already friends");
			}
			return true;
		} else if (isFriendReply(s)) {
			// message format: @user #friends
			String user = message[0].substring(1, message[0].length());
			addFriend(user);
			return true;
		} else if (isFriendRemove(s)) {
			String user = message[0].substring(1, message[0].length());
			removeFriend(user);
			return true;
		}
		return false;
	}

	public boolean isDuplicateRequest(String user) {
		if (friendrequests.contains(user)) {
			sendMessage("Easy, we have already sent the request, give " + user + " some time to handle it");
			return true;
		}
		return false;

	}

	public void sendFriendRequest(String user) throws InterruptedException {
		UserThread thread = findUserThreadByName(user);
		if (thread == null) {
			return;
		}
		friendrequests.add(user);
		sendMessage("<" + userName + ">" + " Would you like to be friends?");
		sendMessage(thread, "<" + userName + ">" + " Would you like to be friends?");
	}

	public void sendMessage(String template) {
		try {
			output_stream = new PrintStream(userSocket.getOutputStream(), true);
			output_stream.println(template);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(UserThread thread, String template) {
		try {
			thread.output_stream = new PrintStream(thread.userSocket.getOutputStream(), true);
			thread.output_stream.println(template);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public UserThread findUserThreadByName(String user) {
		if (!isUserExist(user)) {
			return null;
		}
		for (UserThread thread : threads) {
			if (thread.userName.equals(user)) {
				return thread;
			}
		}
		return null;
	}

	public void addFriend(String user) {
		UserThread thread = findUserThreadByName(user);
		if (thread == null) {
			return;
		}
		friends.add(user);
		thread.friends.add(userName);
		removeFriendRequest(thread);
		sendMessage(userName + " and " + user + " are now friends!");
		sendMessage(thread, userName + " and " + user + " are now friends!");
	}

	public void removeFriendRequest(UserThread thread) {
		for (int i = 0; i < thread.friendrequests.size(); i++) {
			if (userName.equals(thread.friendrequests.get(i))) {
				thread.friendrequests.remove(i);
			}
		}
	}

	public void replyAddFriend() {

	}

	public void removeFriend(String user) {
		for (int i = 0; i < friends.size(); i++) {
			if (user.equals(friends.get(i))) {
				friends.remove(i);
			}
		}
		UserThread thread = findUserThreadByName(user);
		for (int i = 0; i < thread.friends.size(); i++) {
			if (userName.equals(thread.friends.get(i))) {
				thread.friends.remove(i);
			}
		}
		sendMessage(userName + " and " + user + " are not friends anymore!");
		sendMessage(thread, userName + " and " + user + " are not friends anymore!");
	}

	public void logout() {
		// Anyone must wait until the previous user logout, it's by design
		synchronized (UserThread.class) {
			try {
				for (UserThread thread : threads) {
					thread.output_stream = new PrintStream(thread.userSocket.getOutputStream(), true);
					for (int i = 0; i < thread.friends.size(); i++) {
						if (userName.equals(thread.friends.get(i))) {
							thread.friends.remove(i);
						}
					}
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
		} catch (IOException | InterruptedException e) {
			System.err.println(
					"Error: Client closed the session, reset the connection, other threads will be maintained, don't worry.");
			errorClose();
			this.stop();
			return;
		}
	}

	public void errorClose() {
		try {
			input_stream.close();
			output_stream.close();
			userSocket.close();
			for (int i = 0; i < threads.size(); i++) {
				if (threads.get(i) == this) {
					threads.remove(i);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	public boolean isUnicast(String s) {
		String[] message = s.split("\\s+");
		if (message[0].charAt(0) == '@') {
			return true;
		} else {
			return false;
		}
	}

	public String getUnicastUserName(String[] message) {
		return message[0].substring(1, message[0].length());
	}

	public String getUnicastMessage(String s, String[] message) {
		if (s.length() < message[0].length() + 1) {
			return null;
		}
		return s.substring(message[0].length() + 1);
	}

	public void unicast(String s) {
		String[] messages = s.split("\\s+");
		String unicastUser = getUnicastUserName(messages);
		if (unicastUser == null || !isUserExist(unicastUser)) {
			return;
		}
		String message = getUnicastMessage(s, messages);
		if (!isValidMessage(message)) {
			return;
		}
		try {
			output_stream = new PrintStream(userSocket.getOutputStream(), true);
			output_stream.println("<" + userName + ">: " + message);
			for (UserThread thread : threads) {
				if (thread.userName.equals(unicastUser)) {
					// synchronize the thread to prevent other visit
					synchronized (thread) {
						thread.output_stream = new PrintStream(thread.userSocket.getOutputStream(), true);
						thread.output_stream.println("<" + userName + ">: " + message);
					}
				}
			}
		} catch (Exception e) {
			System.err.println(
					"Client closed the session, reset the connection, other threads will be maintained, don't worry.");
			errorClose();
			this.stop();
			return;
		}
	}

	public boolean isUserExist(String s) {
		for (UserThread thread : threads) {
			if (thread.userName != null && thread.userName.equals(s)) {
				return true;
			}
		}
		try {
			output_stream = new PrintStream(userSocket.getOutputStream(), true);
			output_stream.println("<" + s + ">" + " is not exists");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;

	}

	public boolean isValidMessage(String s) {
		try {
			if (s == null || s == "") {
				output_stream = new PrintStream(userSocket.getOutputStream(), true);
				output_stream.println("Please input something");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;

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
					} else if (isFriendRelated(s)) {
					} else if (isUnicast(s)) {
						if (isFriend(s)) {
							unicast(s);
						}
					} else {
						broadcast(s);
					}
				}
			} catch (Exception e) {
				System.err.println(
						"Error: Client closed the session, reset the connection, other threads will be maintained, don't worry.");
				errorClose();
				this.stop();
				return;
			}
		}
	}
}
