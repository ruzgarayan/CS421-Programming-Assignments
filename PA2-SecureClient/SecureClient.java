import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

//Ruzgar Ayan
//12.05.2020
public class SecureClient {

	static BufferedReader inFromUser;
	static Socket clientSocket;
	static BufferedOutputStream outToServer;
	static BufferedReader inFromServer;
	
	public static byte[] getSignature(byte[] cert)
	{
		String certS = new String(cert);
		int startingIndex = certS.indexOf("SIGNATURE=") + 10;
		return certS.substring(startingIndex).getBytes();
		
	}
	public static String getCA(byte[] cert)
	{
		String certS = new String(cert);
		int startingIndex = certS.indexOf("CA=") + 3;
		int endingIndex = certS.indexOf("SIGNATURE=");
		return certS.substring(startingIndex, endingIndex);
	}
	public static byte[] getPK(byte[] cert)
	{
		String certS = new String(cert);
		int startingIndex = certS.indexOf("PK=") + 3;
		int endingIndex = certS.indexOf("CA=");
		return certS.substring(startingIndex, endingIndex).getBytes();
	}
	
	public static byte[] receiveHELLO() throws IOException
	{
		char[] type = null, length = null;
		char[] response = readInput(type, length);
		return (new String(response)).getBytes();
	}
	
	public static byte[] receiveRESPONSE() throws IOException
	{
		char[] type = null, length = null;
		char[] response = readInput(type, length);
		return (new String(response)).getBytes();
	}
	
	public static char[] readInput(char[] type, char[] length)
	{
		try {
			type = new char[8];
			length = new char[4];
			inFromServer.read(type);
			inFromServer.read(length);
			int len = new BigInteger(new String(length).getBytes()).intValue();
			//System.out.println(len);
			char[] data = new char[len];
			inFromServer.read(data);
			return data;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String convertLengthTo4Bytes(int length)
	{
		ByteBuffer b = ByteBuffer.allocate(4);
		b.putInt(length);
		return new String(b.array());
	}
	
	public static void sendCommand(String command, byte[] data)
	{
		try {
			outToServer.write(command.getBytes(StandardCharsets.US_ASCII));
			if (data != null)
				outToServer.write(convertLengthTo4Bytes(data.length).getBytes());
			else
				outToServer.write(convertLengthTo4Bytes(0).getBytes());
			if (data != null)
				outToServer.write(data);
			outToServer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void sendHELLO()
	{
		sendCommand("HELLOxxx", null);
	}
	public static void sendSECRET(byte[] secretEncrypted)
	{
		sendCommand("SECRETxx", secretEncrypted);
	}
	public static void sendSTARTENC()
	{
		sendCommand("STARTENC", null);
	}
	public static void sendENDENC()
	{
		sendCommand("ENDENCxx", null);
	}
	public static void sendPUBLIC()
	{
		sendCommand("PUBLICxx", null);
	}
	
	public static void sendPRIVATE()
	{
		sendCommand("PRIVATEx", null);
	}
	
	public static void sendLOGOUT()
	{
		sendCommand("LOGOUTxx", null);
	}
	
	public static void sendAUTH(byte[] authEncrypted)
	{
		sendCommand("AUTHxxxx", authEncrypted);
	}
	
	public static String decodeUS_ASCII(byte[] arr)
	{
		return new String(arr, StandardCharsets.US_ASCII);
	}
	
	public static void main(String[] args) throws IOException {

		// Get the port from command line arguments.
		int port = Integer.parseInt(args[0]);
		// Instantiate CryptoHelper
		CryptoHelper crypto = new CryptoHelper();

		byte[] signature;
		String ca;
		byte[] serverPublicKey;
		
		while(true) {
			//create a socket and connect to ("127.0.0.1", <Port>);
			try {
				inFromUser = new BufferedReader(new InputStreamReader(System.in));
				clientSocket = new Socket("127.0.0.1", port);
				outToServer = new BufferedOutputStream(clientSocket.getOutputStream());
				inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			} catch (IOException e) {
				continue;
			}
		
			// --- HANDSHAKE START
			sendHELLO();

		// Receive the certificate
			byte[] cert = receiveHELLO();

			// Get necessary fields from the certificate
			signature = getSignature(cert);
			ca = getCA(cert);
			serverPublicKey = getPK(cert);

			// Verification is successful:
			if (crypto.verifySignature(cert, signature, ca)) 
				break;

		// Verification fails:
			else 
				clientSocket.close();
		}

		// Create and send encrypted secret
		int secret = crypto.generateSecret();
		byte[] secretEncrypted = crypto.encryptSecretAsymmetric(secret, serverPublicKey);
		sendSECRET(secretEncrypted);
		// --- HANDSHAKE END


		// --- AUTHENTICATION START
		sendSTARTENC();  // Start encryption

		// Send encrypted authentication info
		byte[] authEncrypted = crypto.encryptSymmetric("bilkent cs421", secret);
		sendAUTH(authEncrypted);

		// Receive authentication response
		byte[] data = receiveRESPONSE();
		String response = crypto.decryptSymmetric(data, secret);
		System.out.println(response);  // This should be "OK"

		sendENDENC();  // End encryption
		// --- AUTHENTICATION END
		// --- VIEW PUBLIC POSTS START
		sendPUBLIC();
		byte[] data2 = receiveRESPONSE();

		// Decode the byte array into a string & display
		String response2 = decodeUS_ASCII(data2);
		System.out.println(response2);
		// --- VIEW PUBLIC POSTS END


		// --- VIEW PRIVATE MESSAGES START
		sendSTARTENC();  // Start encryption
		sendPRIVATE();

		// Receive, decrypt & display
		byte[] data3 = receiveRESPONSE();
		String response3 = crypto.decryptSymmetric(data3, secret);
		System.out.println(response3);

		sendENDENC();  // End encryption
		// --- VIEW PRIVATE MESSAGES END

		
		// LOGOUT
		sendLOGOUT();
		clientSocket.close();
	
	}
}
