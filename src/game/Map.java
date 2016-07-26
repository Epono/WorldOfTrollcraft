package game;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * Represents the <b>Map</b> of the game.<br/>
 * It's a 2D array of <b>Cells</b>.<br/>
 */
public class Map {
	private static final int MAX_WIDTH = 100;
	private static final int MAX_HIGHT = 75;
	private HashMap<Integer, Player> players;
	private Player player;
	private Cell[][] mapMatrix;
	private int height, width;

	/**
	 * Constructs the <b>Map</b> and initializes the <b>Players</b> HashMap.
	 * 
	 * @param isConnected
	 */
	public Map() {
		Logger.getLogger(Map.class).info("Map creation");
		players = new HashMap<Integer, Player>();
		initMap();
	}

	/**
	 * Initializes the <b>Map</b> by reading a file named "standard_map.txt",
	 * checking its content, and rejecting the user if the <b>Map</b> file is
	 * malformed.
	 */
	public void initMap() {
		Logger.getLogger(Map.class).info("Map initialization");
		String mapFilePath = "/standard_map.txt";
		height = -1;
		width = -1;
		String chaine = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(mapFilePath)));
			String ligne;
			while ((ligne = br.readLine()) != null) {
				chaine += ligne + "\n";
			}
			br.close();
		} catch (FileNotFoundException fnfe) {
			Logger.getLogger(Map.class).fatal("Map file not found");
		} catch (SecurityException se) {
			Logger.getLogger(Map.class).fatal("Map file - Security Exception");
		} catch (IOException ioe) {
			Logger.getLogger(Map.class).fatal("Map file - IO Exception");
		}

		if (!chaine.matches("(\\d{1,3})[;](\\d{1,2})[;][\\n]((([rwtf][,])+)[\\n])+([rwtf][,])+[rwtf][;][\n]")) {				//pour verifier que le fichier de la map est bien forme
			Logger.getLogger(Map.class).fatal("Map file malformed (general pattern)");
			System.exit(-1);
		}

		String[] str = chaine.split(";");												//on coupe la chaine en plusieurs morceaux, separes par des ";"

		try {
			width = Integer.valueOf(str[0]);											//le 1er champ correspond a la hauteur
		} catch (NumberFormatException nfe) {
			Logger.getLogger(Map.class).fatal("Map file malformed (width not a number)");
			System.exit(-1);
		}
		if (width > MAX_WIDTH || width < 1) {
			Logger.getLogger(Map.class).fatal("Map file malformed (width out of bounds)");
			System.exit(-1);
		}

		try {
			height = Integer.valueOf(str[1]);											//le 2em champ correspond a la largeur
		} catch (NumberFormatException nfe) {
			Logger.getLogger(Map.class).fatal("Map file malformed (height not a number)");
			System.exit(-1);
		}
		if (height > MAX_HIGHT || height < 1) {
			Logger.getLogger(Map.class).fatal("Map file malformed (height out of bounds)");
			System.exit(-1);
		}

		String str2[] = str[2].split(",");												//on coupe le 3em champ, qui correspond a la map, separes par des ","
		if (str2.length != height * width) {
			Logger.getLogger(Map.class).fatal("Map file malformed (wrong number of cells)");
			System.exit(-1);
		}

		mapMatrix = new Cell[width][height];

		for (int i = 0; i < str2.length; i++) {
			if (i % width == 0) {							//if(i%largeur == 0) => doit y avoir un \n
				if (str2[i].charAt(0) == '\n') {				//si \n => good
					str2[i] = str2[i].replaceAll("\n", "");
				} else {										//si pas de \n
					Logger.getLogger(Map.class).fatal("Map file malformed (lack of carriage return )");
					System.exit(-1);
				}
			} else {											//if(i%largeur != 0) => pas de \n
				if (str2[i].charAt(0) == '\n') {				//si  \n => pas bien
					Logger.getLogger(Map.class).fatal("Map file malformed (carriage return where there shouldn't be)");
					System.exit(-1);
				}
			}
		}

		for (int i = 0; i < str2.length; i++) {												//on remplit le tableau de ce qui va bien
			String strTemp = str2[i];
			char a = strTemp.charAt(0);
			Logger.getLogger(Map.class).trace("Index : " + i + " - Set : " + strTemp + " - (" + i % width + "," + i / width + ")");
			switch (a) {
				case 'f':
					mapMatrix[i % width][i / width] = null;
					break;
				case 't':
					mapMatrix[i % width][i / width] = Set.getTreeInstance();
					break;
				case 'r':
					mapMatrix[i % width][i / width] = Set.getRockInstance();
					break;
				case 'w':
					mapMatrix[i % width][i / width] = Set.getWaterInstance();
					break;
				default:
					Logger.getLogger(Map.class).fatal("Map file malformed (bad character : (i = " + i + " char : " + a);
					System.exit(-1);
					break;

			}
		}
		Logger.getLogger(Map.class).info("Map initialization finished");
	}

	/**
	 * Checks if the given <b>Direction</b> is an available movement.
	 * 
	 * @param direction
	 *            : the <b>Direction<b> to be tested.
	 * @return <code><b>true</b></code> if the displacement is possible,
	 *         <code><b>false</b></code> otherwise.
	 */
	public boolean isValidDirection(Direction direction) {
		short coordX = player.getCoord().getCoordX();
		short coordY = player.getCoord().getCoordY();
		switch (direction) {
			case DOWN:
				if (coordY < height - 1 && mapMatrix[coordX][coordY + 1] == null) {
					Logger.getLogger(Map.class).debug("Displacement possible, asking the server");
					return true;
				}
				break;
			case LEFT:
				if (coordX > 0 && mapMatrix[coordX - 1][coordY] == null) {
					Logger.getLogger(Map.class).debug("Displacement possible, asking the server");
					return true;
				}
				break;
			case RIGHT:
				if (coordX < width - 1 && mapMatrix[coordX + 1][coordY] == null) {
					Logger.getLogger(Map.class).debug("Displacement possible, asking the server");
					return true;
				}
				break;
			case UP:
				if (coordY > 0 && mapMatrix[coordX][coordY - 1] == null) {
					Logger.getLogger(Map.class).debug("Displacement possible, asking the server");
					return true;
				}
				break;
			default:
				Logger.getLogger(Map.class).error("Invalid displacement, abort mission !");
				return false;
		}
		Logger.getLogger(Map.class).debug("Impossible displacement");
		return false;
	}

	/**
	 * Returns the 2D array of <b>Cells</b> reprensenting the <b>Map</b>.
	 * 
	 * @return a 2D <b>Cell</b> array.
	 */
	public Cell[][] getMapMatrix() {
		return mapMatrix;
	}

	/**
	 * Returns the height of the <b>Map</b>.
	 * 
	 * @return the height of the <b>Map</b>.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Returns the width of the <b>Map</b>.
	 * 
	 * @return the width of the <b>Map</b>.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Returns the HashMap containing all the <b>Players</b> (local
	 * <b>Player</b> included).
	 * 
	 * @return the HashMap containing all the <b>Players</b> (local
	 *         <b>Player</b> included).
	 */
	public HashMap<Integer, Player> getPlayers() {
		return players;
	}

	/**
	 * Returns the local <b>Player</b>.
	 * 
	 * @return the local <b>Player</b>.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Sets the local <b>Player</b>.
	 * 
	 * @param player
	 *            : the local <b>Player</b>.
	 */
	public void setPlayer(Player player) {
		this.player = player;
	}

	/**
	 * Overrode toString() of <b>Map</b>.</br>
	 * Prints a matrix of dimension (width*height) :</br>
	 * - the index of the <b>Player</b> if the <b>Cell</b> is occupied by
	 * one.</br>
	 * - f is the <b>Cell</b> is free.</br>
	 * - r if there is a <b>Set</b> of <b>Rock</b> type.</br>
	 * - t if there is a <b>Set</b> of <b>Tree</b> type.</br>
	 * - w if there is a <b>Set</b> of <b>Water</b> type.</br>
	 */
	public String toString() {
		Cell[][] grilleMapTemp = mapMatrix.clone();

		String strMap = "";
		int indexJoueur;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (grilleMapTemp[j][i] == null) {
					indexJoueur = -1;
					for (Player playerTemp : players.values()) {
						if (j == playerTemp.getCoord().getCoordX() && i == playerTemp.getCoord().getCoordY()) {
							indexJoueur = playerTemp.getIndex();
							break;
						}
					}
					if (indexJoueur != -1)
						strMap += indexJoueur;
					else
						strMap += "f";
				} else {
					Set setTemp = (Set) grilleMapTemp[j][i];
					switch (setTemp.getSetType()) {
						case ROCK:
							strMap += "r";
							break;
						case TREE:
							strMap += "t";
							break;
						case WATER:
							strMap += "w";
							break;
						default:
							System.out.println("Erreur !");
							break;
					}
				}
				if (j != width - 1)
					strMap += " ";
			}
			if (i != height - 1)
				strMap += "\n";
		}
		return strMap;
	}

}
