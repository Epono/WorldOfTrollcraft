package game;

/**
 * Represents a player on the map.
 */
public class Player extends Cell {
	private String nickname;
	private int index;
	private Coordinate coord;

	/**
	 * Constructor of <b>Player</b>.
	 * 
	 * @param nickname
	 *            : the nickname of the player, as a <b>String</b>.
	 * @param index
	 *            : the index of the player, as an <b>int</b>.
	 * @param coord
	 *            : the current position of the player, as a <b>Coordinate</b>.
	 */
	public Player(String nickname, int index, Coordinate coord) {
		this.nickname = nickname;
		this.index = index;
		this.coord = coord;
	}

	@Override
	public byte getID() {
		return 0;
	}

	/**
	 * Returns the nickname of the <b>Player</b>.
	 * 
	 * @return a <b>String</b> representing the nickname of the <b>Player</b>.
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * Sets the nickname of the <b>Player</b>.
	 * 
	 * @param nickname
	 *            : a <b>String</b> representing the new nickname of the
	 *            <b>Player</b>.
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	/**
	 * Returns the index of the <b>Player</b> in the HashMap.
	 * 
	 * @return the index of the <b>Player</b>.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the current position of the <b>Player</b>.
	 * 
	 * @return the current position of the <b>Player</b>, as a
	 *         <b>Coordinate</b>.
	 */
	public Coordinate getCoord() {
		return coord;
	}

	/**
	 * Sets the position of the <b>Player</b>.
	 * 
	 * @param coord
	 *            : the new position of the <b>Player</b>, as a
	 *            <b>Coordinate</b>.
	 */
	public void setCoord(Coordinate coord) {
		this.coord = coord;
	}
}