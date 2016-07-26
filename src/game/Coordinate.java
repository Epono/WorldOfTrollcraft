package game;

import java.io.Serializable;

/**
 * Represents a box of the <b>Grid</b>, used as a getter.</br>
 * Contains an X coordinate and an Y coordinate.
 */
public class Coordinate implements Serializable {
	private static final long serialVersionUID = -596835483442423362L;
	private short coordX;
	private short coordY;

	/**
	 * Constructs a <b>Coordinate</b> with the given coordinates X and Y.
	 * 
	 * @param coordX
	 *            : X coordinate.
	 * @param coordY
	 *            : Y coordinate.
	 */
	public Coordinate(short coordX, short coordY) {
		this.coordX = coordX;
		this.coordY = coordY;
	}

	/**
	 * Constructs a copy of the given <b>Coordinate</b>.
	 * 
	 * @return a copy of the <b>Coordinate</b>.
	 */
	public Coordinate copy() {
		return new Coordinate(this.coordX, this.coordY);
	}

	/**
	 * Decrements <b>coordY</b> coordinate if it is greater than 0.
	 */
	public void moveNorth() {
		if (coordY > 0) {
			coordY--;
		}
	}

	/**
	 * Increments <b>coordY</b> coordinate if it is less than
	 * (<b>dimension</b>-1).
	 * 
	 * @param dimension
	 *            : the size of the <b>Grid</b>.
	 */
	public void moveSouth(int hauteur) {
		if (coordY < hauteur - 1) {
			coordY++;
		}
	}

	/**
	 * Increments <b>coordX</b> coordinate if it is less than
	 * (<b>dimension</b>-1).
	 * 
	 * @param dimension
	 *            : the size of the <b>Grid</b>.
	 */
	public void moveEast(int largeur) {
		if (coordX < largeur - 1) {
			coordX++;
		}
	}

	/**
	 * Decrements <b>coordX</b> coordinate if it is greater than 0.
	 */
	public void moveWest() {
		if (coordX > 0) {
			coordX--;
		}
	}

	/**
	 * Returns the relative position of a <b>Coordinate</b> relative to another.<br/>
	 * Used to establish the king of displacement.
	 * 
	 * @param coordSource
	 *            : the source of the displacement.
	 * @param coordDestination
	 *            : the destination of the displacement.
	 * @return the type of <b>Direction</b>.
	 */
	public static Direction relativePosition(Coordinate coordSource, Coordinate coordDestination) {
		short coordSourceX = coordSource.getCoordX();
		short coordSourceY = coordSource.getCoordY();
		short coordDestinationX = coordDestination.getCoordX();
		short coordDestinationY = coordDestination.getCoordY();

		if (coordSourceX > coordDestinationX)
			return Direction.LEFT;
		else if (coordSourceX < coordDestinationX)
			return Direction.RIGHT;
		else if (coordSourceY > coordDestinationY)
			return Direction.UP;
		else
			return Direction.DOWN;
	}

	/**
	 * Returns the previous <b>Coordinate</b> of the <b>Player</b>.
	 * 
	 * @param coordPlayer
	 *            : the current <b>Coordinate</b> of the <b>Player</b>.
	 * @param direction
	 *            : the <b>Direction</b> the <b>Player</b> moved.
	 * @return the previous <b>Coordinate<b> of the <b>Player</b>.
	 */
	public static Coordinate oldPosition(Coordinate coordPlayer, Direction direction) {
		short coordSourceX = coordPlayer.getCoordX();
		short coordSourceY = coordPlayer.getCoordY();
		//		System.out.print("Nouvelle : " + coordPlayer);
		Coordinate coordTemp;

		switch (direction) {
			case LEFT:
				coordTemp = new Coordinate((short) (coordSourceX + 1), (short) coordSourceY);
				//			System.out.println("- " + direction.toString() + " - Ancienne : " + coordTemp);
				return coordTemp;
			case UP:
				coordTemp = new Coordinate((short) coordSourceX, (short) (coordSourceY + 1));
				//			System.out.println("- " + direction.toString() + " - Ancienne : " + coordTemp);
				return coordTemp;
			case RIGHT:
				coordTemp = new Coordinate((short) (coordSourceX - 1), (short) coordSourceY);
				//			System.out.println("- " + direction.toString() + " - Ancienne : " + coordTemp);
				return coordTemp;
			case DOWN:
				coordTemp = new Coordinate((short) coordSourceX, (short) (coordSourceY - 1));
				//			System.out.println("- " + direction.toString() + " - Ancienne : " + coordTemp);
				return coordTemp;
			default:
				break;
		}
		return null;
	}


	/**
	 * Returns the X coordinate of the <b>Coordinate</b>.
	 * 
	 * @return the X coordinate of the <b>Coordinate</b>.
	 */
	public short getCoordX() {
		return coordX;
	}

	/**
	 * Returns the Y coordinate of the <b>Coordinate</b>.
	 * 
	 * @return the Y coordinate of the <b>Coordinate</b>.
	 */
	public short getCoordY() {
		return coordY;
	}

	@Override
	public String toString() {
		return "(" + coordX + "," + coordY + ")";
	}

	/**
	 * Override of the <b>hashCode()</b> method to indicate that 2
	 * <b>Coordinate</b> are equals if they have the same X coordinate and Y
	 * coordinate.
	 */
	@Override
	public int hashCode() {
		return 1000 * coordX + coordY;
	}

	/**
	 * Override of the <b>equals()</b> method to indicate that 2
	 * <b>Coordinate</b> are equals if they have the same X coordinate and Y
	 * coordinate.
	 * 
	 * @param obj
	 *            : the <b>Coordinate</b> to be tested.
	 * @return <code>true</code> if this <b>Coordinate</b> has the same X
	 *         coordinate and Y coordinate, <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Coordinate other = (Coordinate) obj;
		if (coordX != other.coordX)
			return false;
		if (coordY != other.coordY)
			return false;
		return true;
	}
}