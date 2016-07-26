package gui;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class ConnexionTexture extends JPanel {
	private static final long serialVersionUID = -7251858975138819735L;
	private int largeur, hauteur;
	private Image img;

	public ConnexionTexture(int largeur, int hauteur){
		this.largeur = largeur;
		this.hauteur = hauteur;
		try {
			img = ImageIO.read(getClass().getResourceAsStream("/textures/accueil.png"));
		} catch (IOException e) {
			Logger.getLogger(ConnexionTexture.class).error("Couldn't load connection texture");
			System.exit(8);
		}               
	}

	public void paintComponent(Graphics g){
		g.drawImage(img, 0, 0, this.getWidth(), this.getHeight(), this);
	}
}