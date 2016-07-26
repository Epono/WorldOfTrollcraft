package gui;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class RockTexture extends JPanel {
	private static final long serialVersionUID = -1636344624292729515L;
	private int coteCase;
	private Image img;

	public RockTexture(int coteCase){
		this.coteCase = coteCase;
		try {
			img = ImageIO.read(getClass().getResourceAsStream("/textures/rock_texture.png"));
		} catch (IOException e) {
			Logger.getLogger(RockTexture.class).error("Couldn't load rock texture");
			System.exit(8);
		}               
	}

	public void paintComponent(Graphics g){
		g.drawImage(img, 0, 0, coteCase, coteCase, this);
	}
}
