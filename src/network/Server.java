package network;

import game.Direction;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Server {
	private static final int port = 6798;
	private static final int id = Integer.parseInt("2d4a68e9", 16);
	private static final byte typeAcceptation = 1, typeRefus = 2, typeContexte = 32, typeMiseAJourJoueur = 34, typeRefusDeplacement = 35, typeAjoutJoueur = 37, typeSuprpessionJoueur = 38, typePing = 64;
	private static ServerSocket serverSocket;
	private BufferedInputStream bis;
	private BufferedOutputStream bos;
	private Thread listeningThread;

	/**
	 * Constructeur de client en lui envoyant un manager pour lui permettre d'y
	 * acceder (moche ?)
	 * Initialise le socketClient et les reader/writter de flux
	 */
	public Server() {
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("TCP server is running on " + port + "...");
			Socket socketClient = serverSocket.accept();
			System.out.println("New client, address " + socketClient.getInetAddress() + " on " + socketClient.getPort() + ".");
			bis = new BufferedInputStream(socketClient.getInputStream());
			bos = new BufferedOutputStream(socketClient.getOutputStream());
			sendDataPing();
		} catch (NoRouteToHostException nrthe) {
			nrthe.printStackTrace();
		} catch (PortUnreachableException pue) {
			pue.printStackTrace();
		} catch (ConnectException ce) {
			ce.printStackTrace();
		} catch (BindException be) {
			be.printStackTrace();
		} catch (UnknownHostException uhe) {
			uhe.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		listeningThread = new Thread(new Runnable() {
			byte[] b = new byte[4096];
			int lengthTemp;

			@Override
			public void run() {
				Arrays.fill(b, (byte) -1);
				try {
					while (true) {
						lengthTemp = bis.read(b);
						if (lengthTemp != -1) {
							System.out.println("Paquet recu : " + byteToString(b, lengthTemp));
							if (ByteBuffer.wrap(b).getInt() == id) {
								decoupeurPaquet(b);
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Lance le thread d'ecoute
	 */
	public void startListening() {
		listeningThread.start();
	}






	/**
	 * Methode decoupant les tableaux de byte recus
	 * (Fait des choses)
	 * 
	 * @param b
	 */
	public void decoupeurPaquet(byte[] b) {
		ByteBuffer bf = ByteBuffer.wrap(b);
		int idb = bf.getInt();
		boolean idValide = idb == id;
		int lengthb = bf.getInt();
		byte typeb = bf.get();
		String donneesb = byteToString(b, lengthb, 9);

		System.out.println("Id : " + idb + " - Validite Id : " + idValide + " - Longueur : " + lengthb + " - Type : " + typeb + "\nDonnees : " + donneesb + "\n");

		//		int i;
		//		String login;
		//		byte index;
		//		Coordinate coordTemp;
		//		Player playerTemp;
		//		boolean localPlayer;

		switch (typeb) {
			case 0:		//demande_authentification
				System.out.println("Demande authentification");
				sendDataAcceptation();
				sendDataContexte();
				break;

			case 33:	//demande_deplacement
				System.out.println("Demande deplacement");
				byte direction = bf.get();
				short coordNewX = 5;
				short coordNewY = 5;
				short coordOldX = 5;
				short coordOldY = 5;
				switch (direction) {
					case 1:
						coordNewX = 4;
						coordNewY = 5;
						coordOldX = 5;
						coordOldY = 5;
						break;
					case 2:
						coordNewX = 5;
						coordNewY = 4;
						coordOldX = 5;
						coordOldY = 5;
						break;
					case 3:
						coordNewX = 6;
						coordNewY = 5;
						coordOldX = 5;
						coordOldY = 5;
						break;
					case 4:
						coordNewX = 5;
						coordNewY = 6;
						coordOldX = 5;
						coordOldY = 5;
						break;

					default:
						System.out.println("hhgjhgjhg");
						break;
				}
				//			short coordNewX = bf.getShort();
				//			short coordNewY = bf.getShort();
				//			short coordOldX = bf.getShort();
				//			short coordOldY = bf.getShort();

				//			Coordinate coordNew = new Coordinate(bf.getShort(), bf.getShort());
				//			Coordinate coordOld = new Coordinate(bf.getShort(), bf.getShort());
				sendDataMiseAJourJoueur(coordNewX, coordNewY, coordOldX, coordOldY);
				break;

			case 36:	//deconnexion
				System.out.println("Deconnexion");
				break;

			case 64:	//ping
				System.out.println("Reception ping");
				break;
			default:
				System.err.println("Erreur dans decoupeurPaquet2");
				break;
		}
		System.out.println();
		System.out.println();
	}

	/**
	 * Methode d'envoie des donnees
	 * 
	 * @param bf
	 * @param lengthData
	 * @return
	 */
	public boolean sendData(ByteBuffer bf, int lengthData) {
		try {
			bos.write(bf.array(), 0, lengthData + 9);
			bos.flush();
			System.out.println("Paquet envoye : " + byteToString(bf.array(), lengthData + 9));
			return true;
		} catch (IndexOutOfBoundsException ioobe) {
			ioobe.printStackTrace();
			return false;
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Methode de creation et d'envoie des donnees de connexion
	 * 
	 * @param login
	 * @param password
	 */
	public boolean sendDataAcceptation() {
		int lengthData = 0;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(id);
		bf.putInt(lengthData);
		bf.put((byte) typeAcceptation);

		return sendData(bf, lengthData);
	}


	/**
	 * Methode de creation et d'envoie des donnees de demande de deplacement
	 * 
	 * @param direction
	 */
	public boolean sendDataRefus(Direction direction) {
		int lengthData = 0;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(id);
		bf.putInt(lengthData);
		bf.put((byte) typeRefus);

		return sendData(bf, lengthData);
	}

	/**
	 * Methode de creation et d'envoie des donnees de deconnection
	 * 
	 * @throws IOException
	 */
	public boolean sendDataContexte() {
		String loginLocal = "moi", login1 = "login1", login2 = "login2", login3 = "login3";
		int lengthData = (login1.length() + 6) + (login2.length() + 6) + (login3.length() + 6) + (loginLocal.length() + 6);
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(id);
		bf.putInt(lengthData);
		bf.put((byte) typeContexte);

		bf.put((loginLocal + '\0').getBytes());
		bf.put((byte) 3);
		bf.putShort((short) 5);
		bf.putShort((short) 5);

		bf.put((login1 + '\0').getBytes());
		bf.put((byte) 0);
		bf.putShort((short) 11);
		bf.putShort((short) 11);

		bf.put((login2 + '\0').getBytes());
		bf.put((byte) 1);
		bf.putShort((short) 9);
		bf.putShort((short) 9);

		bf.put((login3 + '\0').getBytes());
		bf.put((byte) 2);
		bf.putShort((short) 8);
		bf.putShort((short) 8);
		return sendData(bf, lengthData);
	}

	/**
	 * Methode de creation et d'envoie de requete ping
	 */
	public boolean sendDataMiseAJourJoueur(short coordNewX, short coordNewY, short coordOldX, short coordOldY) {
		byte index = 3;
		int lengthData = 9;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(id);
		bf.putInt(lengthData);
		bf.put((byte) typeMiseAJourJoueur);
		bf.put(index);
		bf.putShort(coordNewX);
		bf.putShort(coordNewY);
		bf.putShort(coordOldX);
		bf.putShort(coordOldY);

		return sendData(bf, lengthData);
	}

	/**
	 * Methode de creation et d'envoie de requete ping
	 */
	public boolean sendDataRefusDeplacement() {
		int lengthData = 0;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(id);
		bf.putInt(lengthData);
		bf.put((byte) typeRefusDeplacement);

		return sendData(bf, lengthData);
	}

	/**
	 * Methode de creation et d'envoie de requete ping
	 */
	public boolean sendDataAjoutJoueur() {
		int lengthData = 0;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(id);
		bf.putInt(lengthData);
		bf.put((byte) typeAjoutJoueur);

		return sendData(bf, lengthData);
	}

	/**
	 * Methode de creation et d'envoie de requete ping
	 */
	public boolean sendDataSuprpessionJoueur() {
		int lengthData = 0;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(id);
		bf.putInt(lengthData);
		bf.put((byte) typeSuprpessionJoueur);

		return sendData(bf, lengthData);
	}

	/**
	 * Methode de creation et d'envoie de requete ping
	 */
	public boolean sendDataPing() {
		int lengthData = 0;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(id);
		bf.putInt(lengthData);
		bf.put((byte) typePing);

		return sendData(bf, lengthData);
	}

	/**
	 * Methode affichant les valeurs des byte recus
	 * 
	 * @param b
	 *            : le tableau de byte a afficher
	 * @param length
	 *            : la taille des donnees recues
	 * @return
	 */
	public String byteToString(byte[] b, int length) {
		String str = "";
		int i = 0;
		while (i < length) {
			str += b[i];
			i++;
			if (i != length)
				str += ",";
		}
		return str;
	}

	/**
	 * Methode affichant les valeurs des byte recus (seulement le champ donnee)
	 * 
	 * @param b
	 *            : le tableau de byte a afficher
	 * @param length
	 *            : la taille des donnees recues
	 * @param offset
	 *            : a partir de ou regarder
	 * @return une chaine de caractere des bytes recus, separes par des ","
	 */
	public String byteToString(byte[] b, int length, int offset) {
		if (b[offset] == -1)
			return "pas de donnees";
		String str = "";
		int i = offset;
		while (b[i] != -1) {
			str += b[i];
			i++;
			if (b[i] != -1)
				str += ",";
		}
		return str;
	}

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Server serv = new Server();
		serv.startListening();
	}


}