package game;

/**
 * Enumeration representing the different types of move and their associated
 * values.
 */
public enum Direction {
	LEFT(1), UP(2), RIGHT(3), DOWN(4);

	private final int value;

	/**
	 * Direction enumeration constructor.
	 * 
	 * @param value
	 *            : the value associated with the <b>Direction</b> you wish to
	 *            create.
	 */
	Direction(int value) {
		this.value = value;
	}

	/**
	 * Enumeration getter.
	 * 
	 * @return the value associated with the <b>Direction</b>.<br/>
	 *         - 1 for LEFT.<br/>
	 *         - 2 for UP.<br/>
	 *         - 3 for RIGHT.<br/>
	 *         - 4 for DOWN.<br/>
	 */
	public int Value() {
		return value;
	}

	/**
	 * Returns the type of <b>Direction</b> represented by the given byte.
	 * 
	 * @param b
	 *            : the byte to determine the <b>Direction</b> associated.
	 * @return the <b>Direction</b> represented by the given byte.
	 */
	public static Direction fromByte(byte b) {
		switch (b) {
			case 1:
				return LEFT;
			case 2:
				return UP;
			case 3:
				return RIGHT;
			case 4:
				return DOWN;
		}
		return null;
	}


}
