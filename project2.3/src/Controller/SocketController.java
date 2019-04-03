package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import Main.Main;
import Model.Config;
import View.Popup;

/**
 * ClientSockerController connects with the server Trough the extension
 * ClientCommandHandler, this class can call Commands And can handle Server
 * requests
 *
 * @author Daniel Geerts
 * @since 2019-03-28
 */
public final class SocketController extends CommandHandler {

	private Socket socket = null;

	public Socket getSocket() {
		return socket;
	}

	private boolean msgReceived = false;
	private String msgData = "";
	private Thread runningThread = null;

	private boolean stayConnected = true;

	private static SocketController instance = null;
	private static boolean popupOpen = false;

	/*
	 * getInstance(), else create a error popup
	 */
	public static SocketController getInstance(boolean trySetInstance) {
		if (instance == null && trySetInstance) {
			try {
				instance = new SocketController(Config.REMOTE_IP, Config.REMOTE_PORT);
			} catch (UnknownHostException e) {
				if (!popupOpen) {
					Popup.getInstance().newPopup("Ongeldig IP adres", Popup.Type.OK);
					popupOpen = true;
				}
			} catch (Exception e) {
				if (!popupOpen) {
					Popup.getInstance().newPopup("Kan geen verbinding met Server maken", Popup.Type.OK);
					popupOpen = true;
					// Main.switchScene(Main.SceneType.START);
					// is this the way to go?? ^^
				}
			}
			return instance;
		}
		popupOpen = false;
		return instance;
	}

	/*
	 * Constructor
	 */
	public SocketController(String remoteIp, int serverPort) throws Exception {
		this.socket = new Socket(remoteIp, serverPort);

		// New Thread for receiving input coming from the Server
		runningThread = new Thread() {
			public void run() {
				while (stayConnected) {
					readServerInput();
				}
			}
		};

		runningThread.start();

		System.out.println("Connected to Server: " + this.socket.getInetAddress());
	}

	@Override
	protected void sendMessageToServer(String msg) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(this.socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		out.println(msg);
		out.flush();
		System.out.println("Client send: " + msg);
	}

	protected void readServerInput() {
		msgReceived = false;
		String data = null;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			while ((data = in.readLine()) != null) {
				System.out.println("Message from server: " + data);
				msgReceived = true;
				msgData = data;
				if (popupOpen && data.contains("ERR")) {
					Popup.getInstance().newPopup(data, Popup.Type.OK);
				}

				super.doCommand(data);
			}

		} catch (Exception e) {
			System.out.println(e);
			// this.disconnect();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Controller.ClientCommandHandler#waitForResponse(boolean)
	 */
	@Override
	protected boolean waitForResponse(boolean skipOK) {
		// Timer for when the server is not responding within 3 seconds
		boolean timedOut = false;
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0L;

		msgReceived = false;
		if (this.socket.isConnected()) {
			while (!timedOut && !msgReceived) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				elapsedTime = (new Date()).getTime() - startTime;
				if (elapsedTime > 3000) {
					timedOut = true;
					System.out.println("Server timed-out: main-thread running again");
				}
			}
			
			if (timedOut) {
				System.out.println(" ====== Server timed-out: " + elapsedTime + "ms ======");
			} else {
				System.out.println(" ====== Server responce time: " + elapsedTime + "ms ======");
			}

			if (skipOK && !timedOut && msgData.contains("OK")) {
				waitForResponse(skipOK);
			}
		} else {
			msgReceived = true;
			msgData = "";
		}

		return timedOut;
	}

	@Override
	protected String getMsgData() {
		return msgData;
	}

	public void disconnect() {
		super.logoutFromServer();

		stayConnected = false;
		if (runningThread != null) {
			runningThread.interrupt();
		}
		runningThread = null;

		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		socket = null;
		instance = null;
	}
}