package gui;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class SetsTacticTexture extends JPanel {
	private static final long serialVersionUID = 8668571136694230359L;
	private int coteCase;
	private Image img;

	public SetsTacticTexture(int coteCase){
		this.coteCase = coteCase;
		try {
			img = ImageIO.read(getClass().getResourceAsStream("/textures/image_case_libre.png"));
		} catch (IOException e) {
			Logger.getLogger(SetsTacticTexture.class).error("Couldn't load sets tactic textures");
			System.exit(8);
		}               
	}

	public void paintComponent(Graphics g){
		g.drawImage(img, 0, 0, coteCase-1, coteCase-1, this);
	}
}
