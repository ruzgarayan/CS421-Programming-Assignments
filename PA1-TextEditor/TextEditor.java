import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * CS421 2020 Spring Programming Assignment 1 Ruzgar Ayan 21801984
 */

public class TextEditor {

	static String address; // IP adress of the server given as program arguments
	static int port; // Port of the server socket given as program arguments

	static BufferedReader inFromUser;
	static Socket clientSocket;
	static DataOutputStream outToServer;
	static BufferedReader inFromServer;

	static boolean connection; // Shows the state of the connection.

	static boolean madeUpdate; // Shows if an update has been recently made.
	static boolean userAccepted, passAccepted; // Shows if the client has successfully entered the 
												// username or the password.
	
	static String user, pass; // The username and the password of the client if successfully entered.

	public static void main(String[] args) {
		// Initialize variables.
		address = args[0];
		port = Integer.parseInt(args[1]);

		connection = false;
		madeUpdate = false;
		userAccepted = false;
		passAccepted = false;
		user = "";
		pass = "";

		String command;
		try {
			// Create input and output streams and the socket.
			inFromUser = new BufferedReader(new InputStreamReader(System.in));
			clientSocket = new Socket(address, port);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			connection = true;
			// While the connection is on, continuously get commands from the user,
			// send them to the server and process the responses.
			while (connection) {
				command = inFromUser.readLine();
				// To process each command accordingly, call the appropriate function
				// by looking at the first word of the command.
				switch (command.split(" ")[0]) {
				case "USER":
					sendCommandUSER(command);
					break;
				case "PASS":
					sendCommandPASS(command);
					break;
				case "WRTE":
					sendCommandWRTE(command);
					break;
				case "APND":
					sendCommandAPND(command);
					break;
				case "UPDT":
					sendCommandUPDT(command);
					break;
				case "EXIT":
					sendCommandEXIT(command);
					break;
				default:
					System.out.println("Your command is not recognized. Please use one of the following:");
					System.out.println("USER\nPASS\nWRTE\nAPND\nUPDT\nEXIT");
					break;
				}
			}
			clientSocket.close(); // Close the socket when connection becomes false before exiting the program.

		} catch (IOException e) {
			System.out.println("Connection between the program and server is lost. Exiting the program.");
			connection = false;
		}
	}

	// This function is used to reconnect to the server socket
	// in case of the server closing the previous connection.
	static void reconnect() {
		try {
			clientSocket.close(); // For guaranteeing

			// Recreate the socket and the streams.
			clientSocket = new Socket(address, port);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			// If the user has already entered the username or the password,
			// enter them automatically when the new connection is established.
			if (userAccepted) {
				outToServer.writeBytes("USER " + user + "\r\n");
				inFromServer.readLine();
			}
			if (passAccepted) {
				outToServer.writeBytes("PASS " + pass + "\r\n");
				inFromServer.readLine();
			}

		} catch (IOException e) {
			System.out.println("Connection between the program and server is lost. Exiting the program.");
			connection = false;
		}
	}

	static void sendCommandUSER(String command) throws IOException {
		String response;
		outToServer.writeBytes(command + "\r\n");
		response = inFromServer.readLine();

		if (response.contains("OK")) {
			System.out.println("Username accepted. Enter a password now.");
			userAccepted = true;
			user = command.split(" ")[1];
		} else {
			if (response.contains("Wrong username")) {
				System.out.println("You have entered a wrong username.");
			} else if (response.contains("Expecting PASS.")) {
				System.out.println("You should enter your password now.");
			} else if (response.contains("command is already sent and processed.")) {
				System.out.println("You have already entered your username.");
			} else {
				System.out.println(response);
			}
			reconnect();
		}
	}

	static void sendCommandPASS(String command) throws IOException {
		String response;
		outToServer.writeBytes(command + "\r\n");
		response = inFromServer.readLine();
		
		// From the assignment specifications:
		// "Upon connecting to the server, a client should first
		// download the text file by using the UPDT command."
		
		// Upon having received a correct password, make an UPDT call automatically
		// to learn the version and the contents of the text file.
		if (response.contains("OK")) {
			outToServer.writeBytes("UPDT -1\r\n"); // Call with -1 to always get a correct update.
			response = inFromServer.readLine();
			System.out.println("Authentication is done successfully.");
			System.out.println("The current version of the text file is " + response.split(" ")[1] + ".");
			System.out.println("Current contents are as follows:");

			System.out.println("######### Beginning #########");
			// While there are lines to be read in the input stream, read and print them.
			while (inFromServer.ready()) {
				System.out.println(inFromServer.readLine());
			}
			System.out.println("############ End ############");

			passAccepted = true;
			madeUpdate = true;
			pass = command.split(" ")[1];
		} else {
			if (response.contains("Wrong password")) {
				System.out.println("You have entered a wrong password.");
			} else if (response.contains("Expecting USER.")) {
				System.out.println("You should first enter your username.");
			} else if (response.contains("command is already sent and processed.")) {
				System.out.println("You have already entered your password.");
			} else {
				System.out.println(response);
			}
			reconnect();
		}
	}

	static void sendCommandAPND(String command) throws IOException {
		// From the assignment specifications:
		// "It is very important that a client cannot change 
		// the text file without getting an update first"
		if (!madeUpdate) {
			System.out.println("Please use UPDT command before modifying the file.");
			return;
		}

		String response;
		outToServer.writeBytes(command + "\r\n");
		response = inFromServer.readLine();
		madeUpdate = false; // Reset to false so that client must update again to make another modification.

		if (response.contains("OK")) {
			System.out.println("Appended successfully, new version is " + response.split(" ")[1] + ".");
		} else {
			if (response.contains("is the current version, please get an update.")) {
				System.out.println("The current version is " + response.split(" ")[1] + ", please get an update.");
			} else if (response.contains("Expecting USER.")) {
				System.out.println("You should first enter your username.");
				reconnect();
			} else if (response.contains("Expecting PASS.")) {
				System.out.println("You should enter your password now.");
				reconnect();
			} else {
				System.out.println(response);
				reconnect();
			}
		}
	}

	static void sendCommandWRTE(String command) throws IOException {
		// From the assignment specifications:
		// "It is very important that a client cannot change 
		// the text file without getting an update first"
		if (!madeUpdate) {
			System.out.println("Please use UPDT command before modifying the file.");
			return;
		}

		String response;
		outToServer.writeBytes(command + "\r\n");
		response = inFromServer.readLine();
		madeUpdate = false; // Reset to false so that client must update again to make another modification.
		
		if (response.contains("OK")) {
			System.out.println("Written successfully, new version is " + response.split(" ")[1] + ".");
		} else {
			if (response.contains("is the current version, please get an update.")) {
				System.out.println(
						"Unsuccessful, the current version is " + response.split(" ")[1] + ", please get an update.");
			} else if (response.contains("Expecting USER.")) {
				System.out.println("You should first enter your username.");
				reconnect();
			} else if (response.contains("Expecting PASS.")) {
				System.out.println("You should enter your password now.");
				reconnect();
			} else {
				System.out.println(response);
				reconnect();
			}
		}
	}

	static void sendCommandUPDT(String command) throws IOException {
		String response;
		outToServer.writeBytes(command + "\r\n");
		response = inFromServer.readLine();
		madeUpdate = true; //Client can now make a modification to the text file.
		
		if (response.contains("OK")) {
			System.out.println("The correct version is " + response.split(" ")[1] + " and the contents of the "
					+ "text file currently is as follows:");
			System.out.println("######### Beginning #########");

			// While there are lines to be read in the input stream, read and print them.
			while (inFromServer.ready()) {
				System.out.println(inFromServer.readLine());
			}
			System.out.println("############ End ############");
		} else {
			if (response.contains("is already the last version.")) {
				System.out.println("Your version is already up to date.");
			} else if (response.contains("is invalid for Update.")) {
				System.out.println("Your version is invalid.");
			} else if (response.contains("Expecting USER.")) {
				System.out.println("You should first enter your username.");
				reconnect();
			} else if (response.contains("Expecting PASS.")) {
				System.out.println("You should enter your password now.");
				reconnect();
			} else {
				System.out.println(response);
				reconnect();
			}
		}
	}

	static void sendCommandEXIT(String command) throws IOException {
		String response;
		outToServer.writeBytes(command + "\r\n");
		response = inFromServer.readLine();
		if (response.contains("OK")) {
			System.out.println("Disconnected from the server successfully, exiting the program.");
		} else {
			System.out.println("Disconnecting from the server is unsuccessful, but still exiting the program.");
		}
		connection = false;
	}
}
