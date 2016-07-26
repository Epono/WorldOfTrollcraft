package gui;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class TreeTexture extends JPanel{
	private static final long serialVersionUID = 7638481894095102670L;
	private int coteCase;
	private Image img;

	public TreeTexture(int coteCase){
		this.coteCase = coteCase;
		try {
			img = ImageIO.read(getClass().getResourceAsStream("/textures/tree_texture.png"));
		} catch (IOException e) {
			Logger.getLogger(TreeTexture.class).error("Couldn't load tree texture");
			System.exit(8);
		}               
	}

	public void paintComponent(Graphics g){
		g.drawImage(img, 0, 0, coteCase, coteCase, this);
	}
}
