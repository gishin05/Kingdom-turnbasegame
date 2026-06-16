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
import game.core.input.KeyboardController;

/**
 * UI DESIGN OVERVIEW:
 * This backend/core component provides the underlying logic and data 
 * structures that support the pixelated game UI approach, ensuring 
 * seamless integration between gameplay mechanics and visual presentation.
 */

public class Main extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private CardLayout cardLayout;
	private SettingsScreen settingsScreen;
	private KeyboardController keyboardController;

	public static final String STARTUP = "STARTUP";
	public static final String TITLE = "TITLE";
	public static final String MENU = "MENU";
	public static final String SAVE_SELECTION = "SAVE_SELECTION";
	public static final String DESIGN_ROOM = "DESIGN_ROOM";
	public static final String MAP_DESIGN = "MAP_DESIGN";

	public static final String VERSUS = "VERSUS";
	public static final String VERSUS_GAMEPLAY = "VERSUS_GAMEPLAY";
	public static final String SETTINGS = "SETTINGS";

	/**
	 * Application entry point. Ensures the UI is created on the Event Dispatch Thread (EDT)
	 * for thread safety in Swing.
	 * 
	 * @param args Command-line arguments
	 */
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

		// Initialize the JavaFX toolkit. This is required because certain features 
		// (like Media/Audio playback) rely on JavaFX components.
		new JFXPanel(); // Bootstraps the JavaFX runtime environment
		Platform.setImplicitExit(false); // Prevents JavaFX from shutting down when no windows are open
		
		cardLayout = new CardLayout();
		contentPane = new JPanel(cardLayout);
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		setContentPane(contentPane);

		// Initialize KeyboardController globally
		// We attach it to the KeyboardFocusManager so that it can intercept 
		// key events system-wide, regardless of which Swing component has focus.
		// [KEYBOARD_CONTROL_MARKER] - Global Key Event Dispatcher
		keyboardController = new KeyboardController();
		java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new java.awt.KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(java.awt.event.KeyEvent e) {
				// Forward pressed and released events to our custom controller
				if (e.getID() == java.awt.event.KeyEvent.KEY_PRESSED) {
					keyboardController.keyPressed(e);
				} else if (e.getID() == java.awt.event.KeyEvent.KEY_RELEASED) {
					keyboardController.keyReleased(e);
				}
				return false; // Let the event continue to be processed by other components
			}
		});

		// Add screens
		contentPane.add(new StartupScreen(this), STARTUP);
		contentPane.add(new TitleScreen(this), TITLE);
		contentPane.add(new MenuScreen(this), MENU);
		contentPane.add(new SaveSelectionScreen(this), SAVE_SELECTION);
		contentPane.add(new DesignRoomScreen(this), DESIGN_ROOM);
		contentPane.add(new MapDesignScreen(this), MAP_DESIGN);

		contentPane.add(new VersusScreen(this), VERSUS);
		contentPane.add(new VersusGameplayScreen(this), VERSUS_GAMEPLAY);
		settingsScreen = new SettingsScreen(this);
		contentPane.add(settingsScreen, SETTINGS);

		// Retrieve and apply user settings from disk
		game.core.save.SettingsSaveData settings = game.core.save.SaveManager.loadSettings();
		game.core.util.SoundManager.setMasterBgmVolume(settings.bgmVolume);
		game.core.util.SoundManager.setBgmVolume(settings.bgmVolume);
		game.core.util.SoundManager.setSfxVolume(settings.sfxVolume);
		
		// Adjust window dimensions based on user's saved resolution preference
		if (settings.resolutionIndex >= 0 && settings.resolutionIndex < SettingsScreen.RESOLUTIONS.length) {
			String[] res = SettingsScreen.RESOLUTIONS[settings.resolutionIndex];
			applyResolution(settings.resolutionIndex, res[0], res[1], res[2]);
		}
		
		// Show initial screen
		showScreen(STARTUP);
		
		// Recursively register mouse listeners to all Swing components
		// to play sound effects on hover and click interactions.
		game.core.util.SoundManager.attachToContainer(this);
	}

	/**
	 * Applies the specified screen resolution. Supports both windowed sizes and full screen mode.
	 * 
	 * @param idx  The index of the resolution in the options array
	 * @param name The descriptive name of the resolution (e.g., "Fullscreen")
	 * @param wStr The width as a string
	 * @param hStr The height as a string
	 */
	public void applyResolution(int idx, String name, String wStr, String hStr) {
		if (name.equals("Fullscreen")) {
			dispose();
			setUndecorated(true);
			setExtendedState(JFrame.MAXIMIZED_BOTH);
			setVisible(true);
		} else {
			int w = Integer.parseInt(wStr);
			int h = Integer.parseInt(hStr);
			if (isUndecorated()) {
				dispose();
				setUndecorated(false);
				setVisible(true);
			}
			setExtendedState(JFrame.NORMAL);
			setSize(w, h);
			setLocationRelativeTo(null);
		}
	}

	/**
	 * Switches the currently visible screen using the CardLayout system and manages 
	 * the background music based on the active screen's context.
	 * 
	 * @param name The identifier of the screen to display
	 */
	public void showScreen(String name) {
		try {
			cardLayout.show(contentPane, name);
			
			// Dynamically adjust background music volume and playback state
			// based on the context of the newly activated screen.
			if (name.equals(TITLE) || name.equals(MENU) || name.equals(SAVE_SELECTION) || name.equals(VERSUS) || name.equals(SETTINGS)) {
				game.core.util.SoundManager.playBgm();
				game.core.util.SoundManager.setBgmVolume(game.core.util.SoundManager.getMasterBgmVolume()); // Full volume
			} else if (name.equals(DESIGN_ROOM) || name.equals(MAP_DESIGN)) {
				game.core.util.SoundManager.playBgm();
				game.core.util.SoundManager.setBgmVolume(game.core.util.SoundManager.getMasterBgmVolume() * 0.3); // Lowered volume in editor screens for focus
			} else {
				game.core.util.SoundManager.pauseBgm(); // Mute BGM for other screens (e.g., active gameplay)
			}

			// Trigger lifecycle methods on all screen components:
			// - Refresh the newly active screen to ensure its data is up to date
			// - Pause all hidden screens to halt background processes (like rendering loops)
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

	/**
	 * Interface implemented by screen panels to hook into the view lifecycle.
	 * Allows screens to manage their resources when shown or hidden.
	 */
	public interface Refreshable {
		/** Called immediately when the screen becomes visible. */
		void refresh();
		
		/** Called immediately when the screen is hidden. */
		void pause();
	}

	public SettingsScreen getSettingsScreen() {
		return settingsScreen;
	}

	public KeyboardController getKeyboardController() {
		return keyboardController;
	}
}
