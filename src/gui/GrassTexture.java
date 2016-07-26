package gui;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class GrassTexture extends JPanel {
	private static final long serialVersionUID = 2098952614471901989L;
	private int coteCase;
	private Image img;

	public GrassTexture(int coteCase){
		this.coteCase = coteCase;
		try {
			img = ImageIO.read(getClass().getResourceAsStream("/textures/grass_texture.png"));
		} catch (IOException e) {
			Logger.getLogger(GrassTexture.class).error("Couldn't load grass texture");
		}  
	}

	public void paintComponent(Graphics g){
		g.drawImage(img, 0, 0, coteCase, coteCase, this);
	}
}
