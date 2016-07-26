package game;

import java.nio.ByteBuffer;
import java.util.Observable;

import network.Client;

import org.apache.log4j.Logger;

/**
 * Represents the link between the <b>Client</b> and the <b>GUI</b> (between the
 * network engine and the GUI).<br/>
 */
public class Manager extends Observable {
	private Map map;
	private Client client;

	/**
	 * Constructs the <b>Manager</b>.
	 * 
	 * @param isConnected
	 */
	public Manager() {
		Logger.getLogger(Manager.class).info("Manager creation");
		map = new Map();
	}

	/**
	 * Creates and Initializes the <b>Client</b>.
	 */
	public void initManagerClient() {
		client = new Client(this);
		client.initClient();
	}

	/**
	 * Creates login request packet and calls sendData() to send it.
	 * 
	 * @param login
	 *            : the login of the player, as a <b>String</b>.
	 * @param password
	 *            : the password of the player, as a <b>String</b>.
	 */
	public boolean sendDataLogin(String login, String password) {
		if (login.length() > 10 || password.length() > 10 || login.length() == 0 || password.length() == 0)			//rajouter de la securite pour eviter les caracteres de merde
			return false;

		int lengthData = login.length() + password.length() + 1;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(Client.ID);
		bf.putInt(lengthData);
		bf.put((byte) Client.PLAYER_LOGIN);
		bf.put(login.getBytes());
		bf.put((byte) '\0');
		bf.put(password.getBytes());

		if (client.sendData(bf.array(), lengthData)) {
			//			setData(LOGIN_SENT);
			setData(dataObserverLoginSent());
			Logger.getLogger(Manager.class).info("Sent interpretation => Authentication request => login [" + login + "] - password : [" + password + "]");
			return true;
		} else
			return false;
	}

	/**
	 * Creates move request packet and calls sendData() to send it.
	 * 
	 * @param direction
	 *            : the <b>Direction</b> of the move.
	 */
	public boolean sendDataMove(Direction direction) {
		int lengthData = 1;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(Client.ID);
		bf.putInt(lengthData);
		bf.put((byte) Client.PLAYER_DISPLACEMENT);
		String displacementType;

		switch (direction) {
			case LEFT:
				bf.put((byte) 1);
				displacementType = "LEFT";
				break;
			case UP:
				bf.put((byte) 2);
				displacementType = "UP";
				break;
			case RIGHT:
				bf.put((byte) 3);
				displacementType = "RIGHT";
				break;
			case DOWN:
				bf.put((byte) 4);
				displacementType = "DOWN";
				break;
			default:
				Logger.getLogger(Manager.class).error("Displacement request not valid => displacementType : " + direction);
				return false;
		}

		if (client.sendData(bf.array(), lengthData)) {
			Logger.getLogger(Manager.class).info("Sent interpretation => Displacement request - type : " + displacementType);
			return true;
		} else
			return false;
	}

	/**
	 * Creates logout packet and calls sendData() to send it.
	 * 
	 * @return <code>true</code> if the packet was sent, <code>false</code>
	 *         oherwise.
	 */
	public boolean sendDataLogout() {
		int lengthData = 0;
		ByteBuffer bf = ByteBuffer.wrap(new byte[lengthData + 9]);

		bf.putInt(Client.ID);
		bf.putInt(lengthData);
		bf.put((byte) Client.LOCAL_PLAYER_LOGOUT);

		if (client.sendData(bf.array(), lengthData)) {
			Logger.getLogger(Manager.class).info("Sent interpretation => Logout notification");
			return true;
		} else
			return false;
	}

	/**
	 * Notifies the server that this client logged out and closes streams and
	 * socket.
	 * 
	 * @return <code>true</code> if the packet was sent, <code>false</code>
	 *         oherwise.
	 */
	public boolean logout() {
		if (sendDataLogout()) {
			if (client.closeClient()) {
				Logger.getLogger(Manager.class).info("Client logged out");
				return true;
			} else
				return false;
		} else
			return false;
	}


	/**
	 * Receives packets and interprets them.
	 * 
	 * @param b
	 *            : the <b>byte</b> array to interpret.
	 * @param lengthb
	 *            : the length of the packet.
	 * @param typeb
	 *            : the type of the packet.
	 */
	public void packetOpeningFactory(byte[] b, int lengthb, byte typeb) {
		ByteBuffer bf = ByteBuffer.wrap(b);
		//		String donneesb = byteToString(b, lengthb);
		//		System.out.println(lengthb + "donnees :"+donneesb);
		Logger.getLogger(Manager.class).trace("Received => Length : " + lengthb + " - Type : " + typeb + " - Datas : " + byteToString(b, lengthb));


		int i;
		String nickname;
		byte index;
		Coordinate coordTemp;
		Player playerTemp;
		boolean localPlayer;

		switch (typeb) {
			case Client.LOGIN_AUTHORIZATION:		//acceptation de connexion
				setData(dataObserverLoginAuthorization());
				Logger.getLogger(Manager.class).info("Received interpretation : Authentification succeeded");
				client.timerPing.restart();
				break;

			case Client.LOGIN_REJECTION:		//refus de connexion
				setData(dataObserverLoginRejection());
				Logger.getLogger(Manager.class).info("Received interpretation : Authentification failed");
				client.timerPing.restart();
				break;

			case Client.UPDATE_MAP:	//contexte
				i = 0;
				localPlayer = true;
				while (i < lengthb) {
					nickname = "";
					while (b[i] != '\0') {
						nickname += (char) b[i];
						i++;
					}
					bf.position(i + 1);
					index = bf.get();
					coordTemp = new Coordinate(bf.getShort(), bf.getShort());
					playerTemp = new Player(nickname, index, coordTemp);

					if (localPlayer) {
						map.setPlayer(playerTemp);
						localPlayer = false;
						Logger.getLogger(Manager.class).info("Received interpretation : Local player (index " + index + ") added at " + coordTemp);
					} else
						Logger.getLogger(Manager.class).info("Received interpretation : Player " + nickname + " (index " + index + ") added at " + coordTemp);

					map.getPlayers().put((int) index, playerTemp);

					i += 6;
				}
				setData(dataObserverUpdateMap());
				client.timerPing.restart();
				break;

			case Client.PLAYER_MOVE:	//mise_a_jour_joueur
				index = bf.get();

				Coordinate coordNew = new Coordinate(bf.getShort(), bf.getShort());
				Coordinate coordOld = new Coordinate(bf.getShort(), bf.getShort());

				map.getPlayers().get((int) index).setCoord(coordNew);

				setData(dataObserverPlayerMove(index, Coordinate.relativePosition(coordOld, coordNew)));
				if (map.getPlayers().get((int) index).equals(map.getPlayer()))
					Logger.getLogger(Manager.class).info("Received interpretation : Displacement executed");
				else
					Logger.getLogger(Manager.class).info("Received interpretation : Player " + index + " moved from " + coordOld + " to " + coordNew);
				client.timerPing.restart();
				break;

			case Client.MOVE_REJECTION:	//refus_deplacement
				Logger.getLogger(Manager.class).info("Received interpretation : Move refused");
				client.timerPing.restart();
				break;

			case Client.PLAYER_JOIN:	//ajout_joueur
				nickname = "";
				i = 0;
				while (b[i] != '\0') {
					nickname += (char) b[i];
					i++;
				}
				bf.position(i + 1);
				index = bf.get();
				coordTemp = new Coordinate(bf.getShort(), bf.getShort());

				map.getPlayers().put((int) index, new Player(nickname, index, coordTemp));
				setData(dataObserverPlayerJoin(index, coordTemp));
				Logger.getLogger(Manager.class).info("Received interpretation : Player " + nickname + "(index " + index + ") added at " + coordTemp);
				client.timerPing.restart();
				break;

			case Client.PLAYER_LEAVE:	//suppresion_joueur
				index = bf.get();
				coordTemp = new Coordinate(bf.getShort(), bf.getShort());

				map.getPlayers().put((int) index, null);
				map.getPlayers().remove((int) index);
				setData(dataObserverPlayerLeave(index, coordTemp));
				Logger.getLogger(Manager.class).info("Received interpretation : Player (index " + index + ") disconnected (last position : " + coordTemp);
				client.timerPing.restart();
				break;
			case Client.PING:
				Logger.getLogger(Manager.class).info("Received interpretation : Ping received");
				client.timerPing.restart();
				break;
			default:
				Logger.getLogger(Manager.class).info("Received interpretation : Wrong type of service");
				break;
		}
	}

	/**
	 * Creates a <b>byte</b> array with datas to notify the GUI that a login
	 * request has been sent.
	 * 
	 * @return a <b>byte</b> array containing datas to send to the GUI.
	 */
	public byte[] dataObserverLoginSent() {
		ByteBuffer bbTemp = ByteBuffer.allocate(1);
		bbTemp.put(Client.PLAYER_LOGIN);
		return bbTemp.array();
	}

	/**
	 * Creates a <b>byte</b> array with datas to notify the GUI that the server
	 * approved the authentification.
	 * 
	 * @return a <b>byte</b> array containing datas to send to the GUI.
	 */
	private byte[] dataObserverLoginAuthorization() {
		ByteBuffer bbTemp = ByteBuffer.allocate(1);
		bbTemp.put(Client.LOGIN_AUTHORIZATION);
		return bbTemp.array();
	}

	/**
	 * Creates a <b>byte</b> array with datas to notify the GUI that the server
	 * rejected the authentification.
	 * 
	 * @return a <b>byte</b> array containing datas to send to the GUI.
	 */
	private byte[] dataObserverLoginRejection() {
		ByteBuffer bbTemp = ByteBuffer.allocate(1);
		bbTemp.put(Client.LOGIN_REJECTION);
		return bbTemp.array();
	}

	/**
	 * Creates a <b>byte</b> array with datas to notify the GUI to update the
	 * map.
	 * 
	 * @return a <b>byte</b> array containing datas to send to the GUI.
	 */
	private byte[] dataObserverUpdateMap() {
		ByteBuffer bbTemp = ByteBuffer.allocate(1);
		bbTemp.put(Client.UPDATE_MAP);
		return bbTemp.array();
	}

	/**
	 * Creates a <b>byte</b> array with datas to notify the GUI that a player
	 * joined.
	 * 
	 * @param index
	 *            : the index of the player who moved.
	 * @param coord
	 *            : the <b>Coordinate</b> of the player who joined.
	 * @return a <b>byte</b> array containing datas to send to the GUI.
	 */
	private byte[] dataObserverPlayerJoin(byte index, Coordinate coord) {
		ByteBuffer bbTemp = ByteBuffer.allocate(6);
		bbTemp.put(Client.PLAYER_JOIN);
		bbTemp.put(index);
		bbTemp.putShort(coord.getCoordX());
		bbTemp.putShort(coord.getCoordY());
		return bbTemp.array();
	}

	/**
	 * Creates a <b>byte</b> array with datas to notify the GUI that a player
	 * moved.
	 * 
	 * @param index
	 *            : the index of the player who moved.
	 * @param direction
	 *            : the <b>Direction</b> of the movement.
	 * @return a <b>byte</b> array containing datas to send to the GUI.
	 */
	private byte[] dataObserverPlayerMove(byte index, Direction direction) {
		ByteBuffer bbTemp = ByteBuffer.allocate(3);
		bbTemp.put(Client.PLAYER_MOVE);
		bbTemp.put(index);
		bbTemp.put((byte) direction.Value());
		return bbTemp.array();
	}

	/**
	 * Creates a <b>byte</b> array with datas to notify the GUI that a player
	 * leaved.
	 * 
	 * @param index
	 *            : the index of the player who leaved.
	 * @param coord
	 *            : the <b>Coordinate</b> of the player who leaved.
	 * @return a <b>byte</b> array containing datas to send to the GUI.
	 */
	private byte[] dataObserverPlayerLeave(byte index, Coordinate coord) {
		ByteBuffer bbTemp = ByteBuffer.allocate(6);
		bbTemp.put(Client.PLAYER_LEAVE);
		bbTemp.put(index);
		bbTemp.putShort(coord.getCoordX());
		bbTemp.putShort(coord.getCoordY());
		return bbTemp.array();
	}

	/**
	 * Creates a <b>byte</b> array with datas to notify the GUI that a
	 * connection with the server has been lost.
	 * 
	 * @return a <b>byte</b> array containing datas to send to the GUI.
	 */
	public byte[] dataObserverConnectionLost() {
		ByteBuffer bbTemp = ByteBuffer.allocate(1);
		bbTemp.put(Client.CONNECTION_LOST);
		return bbTemp.array();
	}

	/**
	 * Converts the given array of <b>bytes</b> in a <b>String</b>.
	 * 
	 * @param b
	 *            : the <b>byte</b> array to convert.
	 * @param length
	 *            : the length to convert.
	 * @return
	 */
	public static String byteToString(byte[] b, int length) {
		if (length == 0)
			return "no datas";
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
	 * Notify the GUI there has been modifications.
	 */
	public void setData(byte[] data) {
		setChanged(); 						// Positionne son indicateur de changement
		notifyObservers(data); 				//rajouter/changer CSTE
	}

	/**
	 * If the displacement is possible, calls the sendDataMove() method of
	 * <b>Client</b> to send a move request to the server.
	 * 
	 * @param direction
	 *            : the <b>Direction</b> the <b>Player</b> wants to move toward.
	 */
	public boolean moveRequest(Direction direction) {
		switch (direction) {
			case LEFT:
				if (map.isValidDirection(Direction.LEFT))
					return sendDataMove(Direction.LEFT);
				break;
			case UP:
				if (map.isValidDirection(Direction.UP))
					return sendDataMove(Direction.UP);
				break;
			case RIGHT:
				if (map.isValidDirection(Direction.RIGHT))
					return sendDataMove(Direction.RIGHT);
				break;
			case DOWN:
				if (map.isValidDirection(Direction.DOWN))
					return sendDataMove(Direction.DOWN);
				break;
			default:
				break;
		}
		return false;
	}

	/**
	 * Returns the <b>Map</b>.
	 * 
	 * @return a reference of the <b>Map</b>.
	 */
	public Map getMap() {
		return map;
	}

	/**
	 * Returns the <b>Client</b>.
	 * 
	 * @return a reference of the <b>Client</b>.
	 */
	public Client getClient() {
		return client;
	}
}