package gui;

import game.Cell;
import game.Coordinate;
import game.Direction;
import game.Manager;
import game.Player;
import game.Set;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import network.Client;

public class Gui extends JFrame implements Observer {
	private static final long serialVersionUID = 9085015462770927626L;

	//Relatif au manager
	private Manager manager;

	//Relatif a la page de connexion;
	private JPanel connexion, formulaire = new JPanel();
	private JLabel loginLabel = new JLabel("Enter Login : "), passwordLabel = new JLabel("Enter password : ");
	private JTextField loginField = new JTextField();
	private JPasswordField passwordField = new JPasswordField();
	private JButton loginButton = new JButton("PLAY !");



	//Relatif a la construction de la gille
	private int nbCaseL, nbCaseH, coteCase, hauteurGrid, largeurGrid, hauteurEcran, largeurEcran, diffHauteur, diffLargeur, posX, posY;

	//Relatif a la fenetre
	private JFrame window;
	private boolean isInvertTexturePanel = false;

	//Relatif au bouton de déconnexion
	private JButton decoButton = new JButton();
	private boolean isExistingDecoButton = false;

	/* constante pour la taille des fichiers de textures. */
	private static final int FORMAT = 64;

	/* panels des couches textures */
	private JLayeredPane texture;
	private JPanel tacticPanel, texturePanel, playerTexture;

	/*
	 * Relatif aux joueurs
	 */
	private HashMap<Integer, Player> hmPlayers;

	/**
	 * Constructeur de la Interface utilisateur
	 * 
	 * @param isConnected
	 *            : boolean, Permet d'initialiser la fenetre pour un jeux local
	 */
	public Gui() {
		super("World of TrollCraft");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window = new JFrame();

		//On appelle cette methode pour récuperer la grille
		initManager();
		window.setLocationRelativeTo(null);
		this.setResizable(false);

		/*
		 * On attache notre keyListener
		 */
		this.setFocusable(true);
		this.addKeyListener(new Controleur());


	}

	/**
	 * Cette Methode nous permet de construire les deux JPanel d'affichage de la
	 * grille
	 */
	public void initManager() {
		//On récupère la map et ses arguments  
		manager = new Manager();
		manager.addObserver(this);
		nbCaseH = manager.getMap().getHeight();
		nbCaseL = manager.getMap().getWidth();
		//On appelle cette methode pour construire les grilles
		initWindow();
		/*
		 * Une fois ces grilles construites, on les remplies
		 * On récupere la map que l'on parcours
		 * Pour chaques case rencontrées, on y affectue une texture.
		 */
		Cell[][] grilleMapTemp = manager.getMap().getMapMatrix();
		for (int i = 0; i < nbCaseH; i++) {
			for (int j = 0; j < nbCaseL; j++) {
				if (grilleMapTemp[j][i] == null) {
					addFreeTacticTexture();
					addGrassTexture();
				} else {
					byte cellType = grilleMapTemp[j][i].getID();
					if (cellType == 1) {
						Set setTemp = (Set) grilleMapTemp[j][i];
						switch (setTemp.getSetType()) {
						case TREE:
							addSetsTacticTexture();
							addTreeTexture();
							break;
						case ROCK:
							addSetsTacticTexture();
							addRockTexture();
							break;
						case WATER:
							addSetsTacticTexture();
							addWaterTexture();
							break;
						default:
							System.err.println("Erreur dans initMap ! " + i);
							System.exit(-5);
							break;

						}
					}
				}
			}
		}

		/*
		 * ici on ajoute les deux panels de textures dans le meme JLayeredPane
		 * avec un ordre d'importance
		 * Cet ordre est un Integer allans de 1 à -(n+1)
		 */
		tacticPanel.setBounds(posX, posY, largeurGrid, hauteurGrid);
		texture.add(tacticPanel, new Integer(-2));
		texturePanel.setBounds(posX, posY, largeurGrid, hauteurGrid);
		texture.add(texturePanel, new Integer(-1));
		/*
		 * On a ajoute la page de connexion
		 */
		initConnexionFrame();
		this.setVisible(true);
	}

	/**
	 * Initialise la fenetre et créer les deux grilles de textures
	 */
	public void initWindow() {

		/*
		 * On désire pouvoir afficher un jeux de texture complet
		 * On définit un JLayeredPane pour contenir les diffrents JPanel qui
		 * constituent la fenetre.
		 * On a alors la possibilité de de superposer différentes couches pour
		 * afficher soit deux textues différentes
		 * ou encore divers éléments comme une console un bouton paramètre
		 */
		texture = new JLayeredPane();
		this.setContentPane(texture);
		this.setSize(900, 600);
		texture.setSize(900, 570);
		hauteurEcran = texture.getHeight();
		largeurEcran = texture.getWidth();

		/*
		 * L'objectif est d'avoir un plein écran qui s'adapte à tout type
		 * d'écran
		 * Si la map est estmié trop grande, On calcule une nouvelle taille pour
		 * les cellules
		 * Si on prend la taille par défaut : FORMAT
		 */
		//Si la largeur est plus gande que cele de la fenetre
		if (nbCaseL * FORMAT > largeurEcran) {
			//Si la heuteur est plus grande que celle de la fenetre
			if (nbCaseH * FORMAT > hauteurEcran) {
				//La grille va dpasser sur la hauteur et la largeur
				//On cherche a savoir sur quel dimension l'écart entre la taille estimé et la taille de l'ecran est la plus grande 
				diffHauteur = (nbCaseH * FORMAT) - hauteurEcran;
				diffLargeur = (nbCaseL * FORMAT) - largeurEcran;
				int diff = diffLargeur - diffHauteur;
				//si la différence de largeure est plus grande ue la différence de hauteur
				if (diff >= 0) {
					//On fait dépendre le coté de la case en fonction de la largeur pou que la hauteur rentre
					coteCase = largeurEcran / nbCaseL;
					hauteurGrid = nbCaseH * coteCase;
					largeurGrid = coteCase * nbCaseL;
					//Verification car il se peut que la hauteur dépasse encore
					//Ceci est du à une différence de ratio entre la map et la fenetre 
					diffHauteur = (nbCaseH * coteCase) - hauteurEcran;
					//Si la hauteur dépase encore
					if (diffHauteur > 0) {
						//On recalcule le coté de la case mais cette fois ci en fonction de la hauteur
						coteCase = hauteurEcran / nbCaseH;
						hauteurGrid = nbCaseH * coteCase;
						largeurGrid = coteCase * nbCaseL;
					}
				}
				//En revanche pour une fenetre trop petite, il se peut aussi quece soit la hauteur qui soit la plus grande
				else {
					//On recalcule le coté de la casse en fonction de la hauteur 
					coteCase = hauteurEcran / nbCaseH;
					largeurGrid = nbCaseL * coteCase;
					hauteurGrid = (nbCaseH * coteCase);
					//On effecte la vérification à partir es nouvelles valeurs
					diffLargeur = (nbCaseL * coteCase) - largeurEcran;
					//Comme precedement, il se peut que la largeur dépasse encore 
					if (diffLargeur > 0) {
						coteCase = largeurEcran / nbCaseL;
						hauteurGrid = nbCaseH * coteCase;
						largeurGrid = coteCase * nbCaseL;
					}
				}

			}

			//Si la largeur est trop grande mais que la hauteur rentre
			else {
				//On recalcule le coté de la case en fonction de la largeur
				//Ici on sait que la hauteur rentre des le début, il 'est pas necessaire de verifier celle-ci
				coteCase = largeurEcran / nbCaseL;
				largeurGrid = nbCaseL * coteCase;
				hauteurGrid = nbCaseH * coteCase;

			}
		}
		//Si la largeur est plus petite que la fenetre
		else {
			//Mais si la hauteur est plus grande
			if (nbCaseH * FORMAT > hauteurEcran) {
				//On recalcule le coté de la case en fonction de la hauteur
				coteCase = hauteurEcran / nbCaseH;
				hauteurGrid = nbCaseH * coteCase;
				largeurGrid = coteCase * nbCaseL;
			}
			//Si la haueur et la largeur sont plus petite que la fenetre 
			else {
				//On prend les valeurs par defaut
				coteCase = FORMAT;
				hauteurGrid = nbCaseH * FORMAT;
				largeurGrid = nbCaseL * FORMAT;
			}
		}


		/*
		 * Une fois le oté de la case déterminé, on souhaite centrer notre
		 * map au milieu de la fenetre
		 * On calcule dons la position de la grille dans la fenetre
		 */
		posX = (largeurEcran - largeurGrid) / 2;
		posY = (hauteurEcran - hauteurGrid) / 2;

		/*
		 * On construit ici la grille repésentant la map avec un jeux de
		 * texture simplifié appellé : TACTIC
		 * Cette grille sera contenue dans un JPanel
		 */
		tacticPanel = new JPanel();
		tacticPanel.setLayout(new GridLayout(nbCaseH, nbCaseL));
		tacticPanel.setDoubleBuffered(true);


		/*
		 * Idem mais pou des textures avancées
		 */
		texturePanel = new JPanel();
		texturePanel.setLayout(new GridLayout(nbCaseH, nbCaseL));
		texturePanel.setDoubleBuffered(true);
	}

	/**
	 * Construit la page de connexion
	 */
	public void initConnexionFrame() {
		//On ajoute l'image de connection
		connexion = new ConnexionTexture(900, 600);
		connexion.setBounds(0, 0, 900, 600);
		texture.add(connexion, new Integer(0));

		formulaire.setLayout(new GridLayout(5, 1));
		formulaire.add(loginLabel);
		loginLabel.setForeground(new Color(193, 115, 28));
		loginLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		formulaire.add(loginField);
		formulaire.add(passwordLabel);
		passwordLabel.setForeground(new Color(193, 115, 28));
		passwordLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		formulaire.add(passwordField);
		loginButton.setForeground(new Color(193, 115, 28));
		loginButton.addActionListener(new playButton());
		formulaire.add(loginButton);
		formulaire.setBounds(334, 197, 233, 140);
		formulaire.setOpaque(false);
		texture.add(formulaire, new Integer(1));
	}

	/**
	 * Ajoute la couche d'affichage des joueurs
	 */
	public void displayPlayers() {
		playerTexture = new PlayerTexture(coteCase, hmPlayers, manager.getMap());
		playerTexture.setOpaque(false);
		playerTexture.setBounds(posX, posY, largeurGrid, hauteurGrid);
		playerTexture.setDoubleBuffered(true);
		texture.add(playerTexture, new Integer(0));
	}

	/**
	 * Information en provenance du Client , nous notifie ce qu'il se passe en termes d'actions de joueurs.
	 * Chaques actions mènent à un traitement associé.
	 */
	public void update(Observable o, Object arg) {
		byte[] b = (byte[]) arg;
		ByteBuffer byteBuffer = ByteBuffer.wrap(b);
		byte type = byteBuffer.get();
		Coordinate coordTemp;
		Direction directionTemp;
		byte indexTemp;
		PlayerTexture.setAction(type);
		PlayerTexture.setGrassGraphics(texturePanel.getGraphics());
		switch (type) {
		case Client.LOGIN_AUTHORIZATION:
			authorization();
			break;
		case Client.CONNECTION_LOST:
			//				System.out.println("connexion perdue");
			//				rajouter un traitement pour revenir au menu principal
			disconnection();
			JOptionPane.showMessageDialog(null, "Server down");
			break;
		case Client.LOGIN_REJECTION:
			JOptionPane.showMessageDialog(null, "Bad login / password");
			break;
		case Client.UPDATE_MAP:
			//afficher tous les joueurs de la HashMap
			hmPlayers = manager.getMap().getPlayers();
			displayPlayers();
			playerTexture.updateUI();

			break;
		case Client.PLAYER_JOIN:
			//index du joueur qui s'est co
			indexTemp = byteBuffer.get();
			//sa coordonne
			coordTemp = new Coordinate(byteBuffer.getShort(), byteBuffer.getShort());

			PlayerTexture.setIndex(indexTemp);
			PlayerTexture.setOldCoord(coordTemp);

			playerTexture.update(playerTexture.getGraphics());


			//PlayerTexture.setIndex( byteBuffer.get());
			//PlayerTexture.setAction(Client.PLAYER_JOIN);
			//Si la hash map de jouer est déja mise a jour alors as besoi des coordonnées
			//PlayerTexture.setOldCoord(Coordinate.oldPosition(manager.getMap().getPlayers().get(bb.get()).getCoord(), Direction.fromByte(bb.get())));
			//nouveau joueur, d'index "index" et de coordonnees "coord" 
			//playerTexture.updateUI();
			break;
		case Client.PLAYER_MOVE:
			//index du joueur qui s'est co
			indexTemp = byteBuffer.get();
			//sa direction
			directionTemp = Direction.fromByte(byteBuffer.get());

			PlayerTexture.setIndex(indexTemp);
			PlayerTexture.setDp(directionTemp);
			PlayerTexture.setOldCoord(Coordinate.oldPosition(manager.getMap().getPlayers().get((int) indexTemp).getCoord(), directionTemp));

			playerTexture.update(playerTexture.getGraphics());
			break;
		case Client.PLAYER_LEAVE:
			//index du joueur qui s'est deco
			indexTemp = byteBuffer.get();
			//sa derniere coordonne
			coordTemp = new Coordinate(byteBuffer.getShort(), byteBuffer.getShort());

			PlayerTexture.setIndex(indexTemp);
			PlayerTexture.setGrassGraphics(texturePanel.getGraphics());
			PlayerTexture.setOldCoord(coordTemp);

			playerTexture.update(playerTexture.getGraphics());

			//			PlayerTexture.setIndex( byteBuffer.get());
			//			PlayerTexture.setOldCoord(Coordinate.oldPosition(manager.getMap().getPlayers().get(byteBuffer.get()).getCoord(), Direction.fromByte(byteBuffer.get())));
			//joueur d'index "index" et de coordonnees "coord" s'est deconnecte
			//			playerTexture.updateUI();
			break;
		default:
			break;
		}
	}

	/**
	 * Permet d'afficher le bouton de déconnexion 
	 */
	private void displayDecoButton() {
		//Lors de déconnexions successive le listner est ajouté n fois
		//On empeche cet ajout
		if(!isExistingDecoButton){
			decoButton.addActionListener(new decoButtonListener());
			isExistingDecoButton = true;
		}
		texture.add(decoButton, new Integer(1));
		decoButton.setBounds(830, 0, 64, 32);
		try {
			decoButton.setIcon(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/textures/deco.png"))));
		} catch (IOException e) {
			decoButton.setText("Log out");
		}
	}

	/**
	 * Ajoute une texture TACTIC : Free
	 */
	public void addFreeTacticTexture() {
		/*
		 * On donne en parametre le coté de la case pour que le constructeur de
		 * la classe
		 * pour que l'image renvoyé soit aux bonne dimensions
		 */
		tacticPanel.add(new FreeTacticTexture(coteCase));
	}

	/**
	 * Ajoute une texture TACTIC : Sets
	 */
	public void addSetsTacticTexture() {
		tacticPanel.add(new SetsTacticTexture(coteCase));

	}

	/**
	 * Ajoute une texture : Grass
	 */
	public void addGrassTexture() {
		texturePanel.add(new GrassTexture(coteCase));
	}

	/**
	 * Ajoute une texture : Rock
	 */
	public void addRockTexture() {
		texturePanel.add(new RockTexture(coteCase));
	} 

	/**
	 * Ajoute une texture : Water
	 */
	public void addWaterTexture() {
		texturePanel.add(new WaterTexture(coteCase));
	}

	/**
	 * Ajoute une texture : Tree
	 */
	public void addTreeTexture() {
		texturePanel.add(new TreeTexture(coteCase));
	}

	/**
	 * Retire les panels de connexions après autorisation 
	 */
	public void authorization() {
		loginField.setText("");
		passwordField.setText("");
		texture.remove(connexion);
		texture.remove(formulaire);
		displayDecoButton();
		texture.updateUI();
	}

	/**
	 * Listener du bouton jouer
	 */
	class playButton implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			manager.initManagerClient();
			String login = loginField.getText();
			String password = String.valueOf(passwordField.getPassword());
			if (login.length() == 0 || password.length() == 0) {
				JOptionPane.showMessageDialog(null, "All fields must be filled");
				loginField.setText("");
				passwordField.setText("");
			} else if (login.length() > 10 || password.length() > 10) {
				JOptionPane.showMessageDialog(null, "Field error");
				loginField.setText("");
				passwordField.setText("");
			} else if (!manager.sendDataLogin(login, password)) {
				JOptionPane.showMessageDialog(null, "Server down");
			}
		}
	}

	/**
	 *Listener du bouton de déconexion 
	 */
	class decoButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			disconnection();
		}
	}

	public void disconnection() {
		manager.getClient().closeClient();
		manager.sendDataLogout();
		texture.remove(playerTexture);
		texture.remove(decoButton);
		//On ajoute l'image de connection
		connexion = new ConnexionTexture(900, 600);
		connexion.setBounds(0, 0, 900, 600);
		texture.add(connexion, new Integer(0));
		
		texturePanel.removeAll();
		Cell[][] grilleMapTemp = manager.getMap().getMapMatrix();
		for (int i = 0; i < nbCaseH; i++) {
			for (int j = 0; j < nbCaseL; j++) {
				if (grilleMapTemp[j][i] == null) {
					addFreeTacticTexture();
					addGrassTexture();
				} else {
					byte cellType = grilleMapTemp[j][i].getID();
					if (cellType == 1) {
						Set setTemp = (Set) grilleMapTemp[j][i];
						switch (setTemp.getSetType()) {
						case TREE:
							addSetsTacticTexture();
							addTreeTexture();
							break;
						case ROCK:
							addSetsTacticTexture();
							addRockTexture();
							break;
						case WATER:
							addSetsTacticTexture();
							addWaterTexture();
							break;
						default:
							System.err.println("Erreur dans initMap ! " + i);
							System.exit(-5);
							break;

						}
					}
				}
			}
		}

		formulaire.setLayout(new GridLayout(5, 1));
		formulaire.add(loginLabel);
		loginLabel.setForeground(new Color(193, 115, 28));
		loginLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		formulaire.add(loginField);
		formulaire.add(passwordLabel);
		passwordLabel.setForeground(new Color(193, 115, 28));
		passwordLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		formulaire.add(passwordField);
		loginButton.setForeground(new Color(193, 115, 28));
		formulaire.add(loginButton);
		formulaire.setBounds(334, 197, 233, 140);
		formulaire.setOpaque(false);
		texture.add(formulaire, new Integer(1));
	}

	/**
	 * KeyListener de la JFrame
	 * Permet d'intervertir le jeux de texture
	 * Permet D'envoyer au manager les requetes de déplacement
	 */
	class Controleur implements KeyListener {
		public void keyTyped(KeyEvent e) {
		}


		public void keyPressed(KeyEvent e) {
			if (e.getKeyChar() == 't') {
				//Si le jeux de texture est dans le bon ordre
				if (isInvertTexturePanel == false) {
					//On retire les panels
					texture.remove(tacticPanel);
					texture.remove(texturePanel);
					//puis on les rajoute dans le bon ordre
					texture.add(tacticPanel, new Integer(-1));
					texture.add(texturePanel, new Integer(-2));
					isInvertTexturePanel = true;
					texture.updateUI();
				} else {
					texture.remove(tacticPanel);
					texture.remove(texturePanel);
					texture.add(tacticPanel, new Integer(-2));
					texture.add(texturePanel, new Integer(-1));
					isInvertTexturePanel = false;
					texture.updateUI();
				}
			}

			if (e.getKeyCode() == KeyEvent.VK_UP) {
				manager.moveRequest(Direction.UP);
			}
			
			if (e.getKeyCode() == KeyEvent.VK_D) {
//				System.out.println("ll");
				System.out.println(manager.sendDataLogout());
			}

			if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				manager.moveRequest(Direction.DOWN);
			}

			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				manager.moveRequest(Direction.LEFT);
			}

			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				manager.moveRequest(Direction.RIGHT);
			}

			if (e.getKeyCode() == KeyEvent.VK_A) {
				double debut = System.currentTimeMillis();
				for(int i = 0; i<5000; i++) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if(!manager.moveRequest(Direction.RIGHT) || !manager.moveRequest(Direction.LEFT)
							|| !manager.moveRequest(Direction.UP) || !manager.moveRequest(Direction.DOWN))
						break;
				}
				System.out.println("Duree pour 1 envoie : " + ((System.currentTimeMillis() - debut))/20000 + "ms");
			}

			if (e.getKeyCode() == KeyEvent.VK_I) {
				double debut = System.currentTimeMillis();
				int i = 0;
				while(true) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if(!manager.moveRequest(Direction.RIGHT) || !manager.moveRequest(Direction.LEFT)
							|| !manager.moveRequest(Direction.UP) || !manager.moveRequest(Direction.DOWN))
						break;
					i++;
					if(i%5000 == 0) {
						System.out.println("Duree pour 1 envoie : " + ((System.currentTimeMillis() - debut))/20000 + "ms");
						debut = System.currentTimeMillis();
					}
				}
				System.out.println("envoies : " + i);
			}
		}

		public void keyReleased(KeyEvent e) {
		}
	}
}
