package game;

/**
 * Represents a cell of the map.<br/>
 * It represents either a free cell or a blocked cell (<b>Set</b>).<br/>
 * If it's free, it has 2 possible states : <br/>
 * - Free<br/>
 * - Occupied by a player<br/>
 */
public abstract class Cell {
	/**
	 * Renvoie le type d'element de la cellule
	 * 
	 * @return le type d'element : 0 pour un joueur, 1 pour du decor
	 */
	/**
	 * Returns the <b>Cell</b> type :<br/>
	 * - 0 for a <b>Cell</b> occupied by a <b>Player</b>.<br/>
	 * - 1 for a <b>Cell</b> blocked by <b>Set</b>.<br/>
	 * - 2 for a free <b>Cell</b>.<br/>
	 * 
	 * @return the element type
	 */
	public abstract byte getID();
}
