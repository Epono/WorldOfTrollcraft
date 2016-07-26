package game;

/**
 * Enumeration representing the different types of sets and their associated
 * values.
 */
public enum SetType {
	TREE(1), WATER(2), ROCK(4);

	private final int value;

	/**
	 * SetType enumeration constructor.
	 * 
	 * @param value
	 *            : the value associated with the <b>SetType</b> you wish to
	 *            create.
	 */
	private SetType(int value) {
		this.value = value;
	}

	/**
	 * Enumeration getter.
	 * 
	 * @return the value associated with the element.<br/>
	 *         - 1 for element TREE.<br/>
	 *         - 2 for element WATER.<br/>
	 *         - 4 for element ROCK.<br/>
	 */
	public int value() {
		return value;
	}
}