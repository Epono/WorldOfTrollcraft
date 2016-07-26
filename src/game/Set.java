package game;

/**
 * Represents a set on the map.<br/>
 * All sets are barriers to movements.<br/>
 */
public class Set extends Cell {
	private SetType setType;
	private static Set water;
	private static Set rock;
	private static Set tree;

	/**
	 * Private constructor in order to singletonize the different kinds of sets. <br/>
	 * 
	 * @param setType
	 *            : the <b>SetType</b> of the object to construct.
	 */
	private Set(SetType setType) {
		this.setType = setType;
	}

	/**
	 * Returns the only instance of <b>Water</b>.
	 * 
	 * @return the only instance of the object <b>Water</b>, inheriting from
	 *         Cell.
	 */
	public static Set getWaterInstance() {
		if (water == null)
			water = new Set(SetType.WATER);
		return water;
	}

	/**
	 * Returns the only instance of <b>Rock</b>.
	 * 
	 * @return the only instance of the object <b>Rock</b>, inheriting from
	 *         Cell.
	 */
	public static Set getRockInstance() {
		if (rock == null)
			rock = new Set(SetType.ROCK);
		return rock;
	}

	/**
	 * Returns the only instance of <b>Tree</b>.
	 * 
	 * @return the only instance of the object <b>Tree</b>, inheriting from
	 *         Cell.
	 */
	public static Set getTreeInstance() {
		if (tree == null)
			tree = new Set(SetType.TREE);
		return tree;
	}

	/**
	 * Returns the <b>SetType</b> of the given <b>Set</b>.
	 * 
	 * @return the <b>SetType</b> of the given <b>Set</b>.
	 */
	public SetType getSetType() {
		return setType;
	}

	/**
	 * Returns a <b>String</b> representing the given <b>Set</b>.
	 * 
	 * @return a <b>String</b> representing the given <b>Set</b>.
	 */
	public String toString() {
		switch (setType) {
			case ROCK:
				return "rock";
			case TREE:
				return "tree";
			case WATER:
				return "water";
			default:
				return "default";
		}
	}

	@Override
	public byte getID() {
		return 1;
	}
}