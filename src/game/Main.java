package game;

import java.awt.CardLayout;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import game.ui.screens.*;
import game.ui.editors.*;

public class Main extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private CardLayout cardLayout;

	public static final String STARTUP = "STARTUP";
	public static final String TITLE = "TITLE";
	public static final String MENU = "MENU";
	public static final String SAVE_SELECTION = "SAVE_SELECTION";
	public static final String DESIGN_ROOM = "DESIGN_ROOM";
	public static final String MAP_DESIGN = "MAP_DESIGN";

	public static final String VERSUS = "VERSUS";
	public static final String VERSUS_GAMEPLAY = "VERSUS_GAMEPLAY";

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main frame = new Main();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Main() {
		setTitle("KINGDOMS: For The WORLD PIECE");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1920, 1080);
		setLocationRelativeTo(null); // Center on screen

		// Initialize JavaFX toolkit and prevent it from auto-shutting down
		new JFXPanel(); // bootstraps JavaFX runtime
		Platform.setImplicitExit(false);
		
		cardLayout = new CardLayout();
		contentPane = new JPanel(cardLayout);
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		setContentPane(contentPane);

		// Add screens
		contentPane.add(new StartupScreen(this), STARTUP);
		contentPane.add(new TitleScreen(this), TITLE);
		contentPane.add(new MenuScreen(this), MENU);
		contentPane.add(new SaveSelectionScreen(this), SAVE_SELECTION);
		contentPane.add(new DesignRoomScreen(this), DESIGN_ROOM);
		contentPane.add(new MapDesignScreen(this), MAP_DESIGN);

		contentPane.add(new VersusScreen(this), VERSUS);
		contentPane.add(new VersusGameplayScreen(this), VERSUS_GAMEPLAY);

		// Show initial screen
		showScreen(STARTUP);
		
		// Attach UI sounds recursively to all Swing interactive components!
		game.core.util.SoundManager.attachToContainer(this);
	}

	public void showScreen(String name) {
		try {
			cardLayout.show(contentPane, name);
			
			// Adjust background music based on active screen
			if (name.equals(TITLE) || name.equals(MENU) || name.equals(SAVE_SELECTION) || name.equals(VERSUS)) {
				game.core.util.SoundManager.playBgm();
				game.core.util.SoundManager.setBgmVolume(1.0);
			} else if (name.equals(DESIGN_ROOM) || name.equals(MAP_DESIGN)) {
				game.core.util.SoundManager.playBgm();
				game.core.util.SoundManager.setBgmVolume(0.3); // 30% volume in editor screens
			} else {
				game.core.util.SoundManager.pauseBgm();
			}

			// Pause all inactive screens and refresh the active one
			for (java.awt.Component comp : contentPane.getComponents()) {
				if (comp instanceof Refreshable) {
					Refreshable r = (Refreshable) comp;
					if (comp.isVisible()) {
						r.refresh();
					} else {
						r.pause();
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error showing screen: " + name);
			e.printStackTrace();
		}
	}

	public interface Refreshable {
		void refresh();
		void pause();
	}
}
