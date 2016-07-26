package gui;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class WaterTexture extends JPanel{
	private static final long serialVersionUID = 8058164137203871932L;
	private int coteCase;
	private Image img;

	public WaterTexture(int coteCase){
		this.coteCase = coteCase;
		try {
			img = ImageIO.read(getClass().getResourceAsStream("/textures/water_texture.png"));
		} catch (IOException e) {
			Logger.getLogger(WaterTexture.class).error("Couldn't load water texture");
			System.exit(8);
		}               
	}

	public void paintComponent(Graphics g){
		g.drawImage(img, 0, 0, coteCase, coteCase, this);
	}
}
