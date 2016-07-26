package network;

import game.Manager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.swing.Timer;

import org.apache.log4j.Logger;

/**
 * Represents the local client.<br/>
 * Used to comunicate with the server and transfer and receive datas with him.<br/>
 */
public class Client {
	public static final byte PLAYER_LOGIN = 0, LOGIN_AUTHORIZATION = 1, LOGIN_REJECTION = 2, UPDATE_MAP = 32, PLAYER_DISPLACEMENT = 33, PLAYER_MOVE = 34, MOVE_REJECTION = 35, LOCAL_PLAYER_LOGOUT = 36, PLAYER_JOIN = 37, PLAYER_LEAVE = 38, PING = 64, CONNECTION_LOST = 65;
	public static final int ID = 0x2d4a68e9;
	private String serverAddress;
	private int port;

	private BufferedInputStream bis;
	private BufferedOutputStream bos;

	private Socket socketClient;
	private Manager manager;

	public Timer timerPing;

	/**
	 * Constructs the local <b>Client</b>.<br/>
	 * 
	 * @param manager
	 *            : a reference to the <b>Manager</b>.
	 * @param isConsoleMode
	 *            : indicates if the program should run in console mode.
	 */
	public Client(Manager manager) {
		Logger.getLogger(Client.class).info("Client creation");
		this.manager = manager;
		initPortAndServerAddress();

		timerPing = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!sendDataPing()) {
					Logger.getLogger(Client.class).warn("Connection lost (no ping response)");
					if (!socketClient.isClosed()) {
						notifyLostConnection();
						closeClient();
					}
					timerPing.stop();
				} else {
					Logger.getLogger(Client.class).trace("Ping response received");
				}
			}
		});
	}

	/**
	 * Notifies the GUI that connection to the server has been lost
	 */
	private void notifyLostConnection() {
		manager.setData(manager.dataObserverConnectionLost());
	}


	/**
	 * Reads the file <b>Gameclient.properties</b> and extracts the address of the server from it.<br/>
	 * If it's malformed, the application closes.
	 */
	private void initPortAndServerAddress() {
		try {
			String GameClientPropertiesFilePath = "/GameClient.properties";
			BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(GameClientPropertiesFilePath)));

			String serverAddressTemp = br.readLine();
			if (serverAddressTemp.matches("(\\d{1,3}[.]){3}\\d{1,3}")) {
				serverAddress = serverAddressTemp;
			} else {
				Logger.getLogger(Client.class).fatal("Server address malformed");
				System.exit(-2);
			}
			String portTemp = br.readLine();
			br.close();
			if (portTemp.matches("\\d{1,5}")) {
				port = Integer.valueOf(portTemp);
			} else {
				Logger.getLogger(Client.class).fatal("Port address malformed");
				System.exit(-2);
			}
		} catch (FileNotFoundException e) {
			Logger.getLogger(Client.class).fatal("GameClient.properties not found");
			System.exit(-2);
		} catch (IOException e) {
			Logger.getLogger(Client.class).fatal("IO error reading GameClient.properties");
			System.exit(-2);
		}
	}

	/**
	 * Initializes the <b>Client</b> by initializing input and output streams, and the socket.<br />
	 * Launches the listening Thread and the ping Timer.<br/>
	 */
	public void initClient() {
		Logger.getLogger(Client.class).info("Client initialization");
		boolean successConnection = false;
		try {
			socketClient = new Socket(InetAddress.getByName(serverAddress), port);
			bis = new BufferedInputStream(socketClient.getInputStream(), 65536);
			bos = new BufferedOutputStream(socketClient.getOutputStream());
			successConnection = true;
		} catch (NoRouteToHostException nrthe) {
			Logger.getLogger(Client.class).error("No Route To Host");
		} catch (PortUnreachableException pue) {
			Logger.getLogger(Client.class).error("Port Unreachable");
		} catch (ConnectException ce) {
			Logger.getLogger(Client.class).error("Server Down");
		} catch (BindException be) {
			Logger.getLogger(Client.class).error("Binding Error");
		} catch (UnknownHostException uhe) {
			Logger.getLogger(Client.class).error("Could not find host");
		} catch (IOException e) {
			Logger.getLogger(Client.class).error("IO error");
		}

		if (successConnection) {
			new Thread(new Runnable() {
				byte[] buffer = new byte[16777216];
				byte[] bufferTemp = new byte[16777216];
				byte type;
				int lengthBuffer, lengthPaquetData;
				ByteBuffer bf;

				@Override
				public void run() {
					try {
						while (true) {
							if(lengthBuffer < buffer.length - 20); {
								lengthBuffer = bis.read(buffer);
								if (lengthBuffer != -1) {
									//Logger.getLogger(Client.class).trace("Received packet length : " + lengthBuffer + " - Packet content : " + Manager.byteToString(buffer, lengthBuffer));
									bf = ByteBuffer.wrap(buffer);
	
									while (lengthBuffer > 0) {
	
										while (lengthBuffer < 4) {
											int lengthBufferTemp = bis.read(buffer, lengthBuffer, buffer.length);
											if (lengthBufferTemp != -1)
												lengthBuffer += lengthBufferTemp;
										}
	
										if (bf.getInt() == ID) {
											while (lengthBuffer < 8) {
												int lengthBufferTemp = bis.read(buffer, lengthBuffer, buffer.length);
												if (lengthBufferTemp != -1)
													lengthBuffer += lengthBufferTemp;
											}
											lengthPaquetData = bf.getInt();
	
											while (lengthBuffer < 9 + lengthPaquetData) {
												int lengthBufferTemp = bis.read(buffer, lengthBuffer, buffer.length);
												if (lengthBufferTemp != -1)
													lengthBuffer += lengthBufferTemp;
											}
	
											type = bf.get();
	
											bf.get(bufferTemp, 0, lengthPaquetData);
											lengthBuffer -= (lengthPaquetData + 9);
	
											//Logger.getLogger(Client.class).trace("Processing => Packet length : " + lengthPaquetData + " Packet content : " + Manager.byteToString(bufferTemp, lengthPaquetData));
											//Logger.getLogger(Client.class).trace("Remaining => Packet length : " + lengthBuffer + " Packet content : " + Manager.byteToString(buffer, lengthBuffer));
	
											manager.packetOpeningFactory(bufferTemp, lengthPaquetData, type);
										} else {
											bf.position(bf.position() - 3);
											lengthBuffer--;
										}
									}
								}
							}
						}
					} catch (ConnectException ce) {
						Logger.getLogger(Client.class).error("Server Down");
					} catch (SocketException se) {
						if (socketClient.isClosed())
							Logger.getLogger(Client.class).error("Socket closed");
						if (!socketClient.isConnected())
							Logger.getLogger(Client.class).error("Socket disconnected");
						if (!socketClient.isBound())
							Logger.getLogger(Client.class).error("Socked unbound");
					} catch (IndexOutOfBoundsException e)  {
						Logger.getLogger(Client.class).error("Buffer length error");
						notifyLostConnection();
					}
					catch (IOException e) {
						Logger.getLogger(Client.class).error("IO Error");
					} finally {
						closeClient();
					}
				}
			}).start();
			timerPing.start();
			Logger.getLogger(Client.class).info("Client initialization succeeded");
		} else
			Logger.getLogger(Client.class).warn("Client initialization failed");
	}


	/**
	 * Closes the input stream, the output stream and the socket, if they are not already closed.
	 * 
	 * @return <code>true</code> if closing went well, <code>false</code>
	 *         otherwise.
	 */
	public boolean closeClient() {
		try {
			bis.close();
			bos.close();
			socketClient.close();
			Logger.getLogger(Client.class).info("Socket and streams closed");
			return true;
		} catch (SocketException se) {
			Logger.getLogger(Client.class).error("Socket error");
			return false;
		} catch (IOException e) {
			Logger.getLogger(Client.class).error("IO error");
			return false;
		}
	}

	/**
	 * Creates ping packet and calls sendData() to send it.
	 */
	public boolean sendDataPing() {
		int lengthData = 0;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(ID);
		bf.putInt(lengthData);
		bf.put((byte) Client.PING);

		if (sendData(bf.array(), lengthData)) {
			Logger.getLogger(Client.class).info("Sent interpretation => Ping request");
			return true;
		} else
			return false;
	}

	/**
	 * Sends given packet.
	 * 
	 * @param bf
	 *            : the <b>byte</b> array to send/
	 * @param lengthData
	 *            : the length of the <b>byte</b> array to send.
	 * @return <code>true</code> if the sending worked, <code>false</code>
	 *         otherwise.
	 */
	public boolean sendData(byte[] bf, int lengthData) {
		try {
			bos.write(bf, 0, lengthData + 9);
			bos.flush();
			Logger.getLogger(Client.class).trace("Sent packet data length : " + (lengthData + 9) + " - Packet content : " + Manager.byteToString(bf, lengthData + 9));
			return true;
		} catch (IndexOutOfBoundsException ioobe) {
			Logger.getLogger(Client.class).error("Index out of bounds exception");
			return false;
		} catch (NullPointerException npe) {
			Logger.getLogger(Client.class).error("Null pointer exception");
			return false;
		} catch (SocketException se) {
			Logger.getLogger(Client.class).error("Server disconnected");
			return false;
		} catch (IOException e) {
			Logger.getLogger(Client.class).error("IO error");
			return false;
		}
	}



}