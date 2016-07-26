package gui;

import game.Coordinate;
import game.Direction;
import game.Map;
import game.Player;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import network.Client;

public class PlayerTexture extends JPanel {
	private static final long serialVersionUID = 4533691192655461957L;
	private static int action;
	private static byte index;
	private static Coordinate oldCoord;
	private int coteCase;
	private static Direction dp = null;
	private static Graphics grassGraphics;
	private HashMap<Integer, Player> hmPlayers;
	private Map map;

	private Image imgLocalPlayerFront, imgLocalPlayerBehind, imgLocalPlayerLeft, imgLocalPlayerRight, imgOtherPlayerFront, imgOtherPlayerBehind, 
	imgOtherPlayerLeft, imgOtherPlayerRight, imgGrass;

	public PlayerTexture(int coteCase, HashMap<Integer, Player> hmPlayers, Map map) {
		this.coteCase = coteCase;
		this.hmPlayers = hmPlayers;
		this.map = map;
		try {
			imgLocalPlayerFront = ImageIO.read(getClass().getResourceAsStream("/textures/character_front.png"));
			imgLocalPlayerBehind = ImageIO.read(getClass().getResourceAsStream("/textures/character_behind.png"));
			imgLocalPlayerLeft = ImageIO.read(getClass().getResourceAsStream("/textures/character_left.png"));
			imgLocalPlayerRight = ImageIO.read(getClass().getResourceAsStream("/textures/character_right.png"));

			imgOtherPlayerFront = ImageIO.read(getClass().getResourceAsStream("/textures/other_character_front.png"));
			imgOtherPlayerBehind = ImageIO.read(getClass().getResourceAsStream("/textures/other_character_behind.png"));
			imgOtherPlayerLeft = ImageIO.read(getClass().getResourceAsStream("/textures/other_character_left.png"));
			imgOtherPlayerRight = ImageIO.read(getClass().getResourceAsStream("/textures/other_character_right.png"));

			imgGrass = ImageIO.read(getClass().getResourceAsStream("/textures/grass_texture.png"));
		} catch (IOException e) {
			Logger.getLogger(PlayerTexture.class).error("Couldn't load player textures");
			System.exit(8);
		}

	}

	@Override
	public void paintComponent(Graphics g) {
		switch (action) {
		case Client.UPDATE_MAP:
			for (Player player : hmPlayers.values()) {
				if (player == map.getPlayer())
					g.drawImage(imgLocalPlayerFront, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				else
					g.drawImage(imgOtherPlayerFront, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
			}
			break;
		case Client.PLAYER_JOIN:
			g.drawImage(imgOtherPlayerFront, oldCoord.getCoordX() * coteCase, oldCoord.getCoordY() * coteCase, coteCase, coteCase, this);
			break;

		case Client.PLAYER_LEAVE:
			grassGraphics.drawImage(imgGrass, oldCoord.getCoordX() * coteCase, oldCoord.getCoordY() * coteCase, coteCase, coteCase, this);
			break;

		case Client.PLAYER_MOVE:
			Player player = hmPlayers.get((int) index);
			switch (dp) {
			case DOWN:
				grassGraphics.drawImage(imgGrass, oldCoord.getCoordX() * coteCase, oldCoord.getCoordY() * coteCase, coteCase, coteCase, this);
				if (player == map.getPlayer())
					g.drawImage(imgLocalPlayerFront, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				else
					g.drawImage(imgOtherPlayerFront, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				break;
			case UP:
				grassGraphics.drawImage(imgGrass, oldCoord.getCoordX() * coteCase, oldCoord.getCoordY() * coteCase, coteCase, coteCase, this);
				if (player == map.getPlayer())
					g.drawImage(imgLocalPlayerBehind, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				else
					g.drawImage(imgOtherPlayerBehind, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				break;
			case LEFT:
				grassGraphics.drawImage(imgGrass, oldCoord.getCoordX() * coteCase, oldCoord.getCoordY() * coteCase, coteCase, coteCase, this);
				if (player == map.getPlayer())
					g.drawImage(imgLocalPlayerLeft, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				else
					g.drawImage(imgOtherPlayerLeft, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				break;
			case RIGHT:
				grassGraphics.drawImage(imgGrass, oldCoord.getCoordX() * coteCase, oldCoord.getCoordY() * coteCase, coteCase, coteCase, this);
				if (player == map.getPlayer())
					g.drawImage(imgLocalPlayerRight, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				else
					g.drawImage(imgOtherPlayerRight, player.getCoord().getCoordX() * coteCase, player.getCoord().getCoordY() * coteCase, coteCase, coteCase, this);
				break;
			}
			break;
		}
	}

	public static void setAction(int action) {
		PlayerTexture.action = action;
	}

	public static void setIndex(byte index) {
		PlayerTexture.index = index;
	}

	public static void setOldCoord(Coordinate oldCoord) {
		PlayerTexture.oldCoord = oldCoord;
	}

	public static void setDp(Direction dp) {
		PlayerTexture.dp = dp;
	}

	public static void setGrassGraphics(Graphics grassGraphics) {
		PlayerTexture.grassGraphics = grassGraphics;
	}

	public void setHmPlayers(HashMap<Integer, Player> hmPlayers) {
		this.hmPlayers = hmPlayers;
	}
}