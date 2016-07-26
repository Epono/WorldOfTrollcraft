package gui;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class FreeTacticTexture extends JPanel {
	private static final long serialVersionUID = -1390973286334413264L;
	private int coteCase;
	private Image img;

	public FreeTacticTexture(int coteCase){
		this.coteCase = coteCase;
		try {
			Image img = ImageIO.read(getClass().getResourceAsStream("/textures/image_decor.png"));
		} catch (IOException e) {
			Logger.getLogger(FreeTacticTexture.class).error("Couldn't load free tactic texture");
			System.exit(8);
		}               
	}

	public void paintComponent(Graphics g){
		g.drawImage(img, 0, 0, coteCase-1, coteCase-1, this);
	}
}
