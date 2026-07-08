package game.ui;

import javax.swing.JPanel;
import game.Main;
import game.Main.Refreshable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.net.URL;
import java.io.File;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import game.core.util.GameLoop;
import game.core.util.GamePaths;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */

/**
 * Base class for all full-screen views with optional video backgrounds.
 */
public abstract class BaseScreen extends JPanel implements Refreshable, GameLoop.Updateable {
    private static final long serialVersionUID = 1L;
    
    protected final Main main;
    protected JFXPanel jfxPanel;
    protected MediaPlayer mediaPlayer;
    protected GameLoop gameLoop;

    /**
     * Constructs the BaseScreen, setting up the primary layout and initializing 
     * the game loop that drives logic updates and rendering for this screen.
     * 
     * @param main The root application window (Main JFrame)
     */
    public BaseScreen(Main main) {
        this.main = main;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK); // Default to black to prevent flashing between screens
        this.gameLoop = new GameLoop(this);
    }

    /**
     * Initializes an animated video background using JavaFX's media player.
     * Maps the given resource path to an internal bundled file if necessary, 
     * and handles embedding the JavaFX scene into the Swing hierarchy.
     * 
     * @param resourcePath The internal path to the video file (e.g., .mp4)
     */
    protected void initVideoBackground(String resourcePath) {
        jfxPanel = new JFXPanel();
        jfxPanel.setBackground(Color.BLACK);
        
        Platform.runLater(() -> {
            try {
                URL resource = getClass().getResource(resourcePath);
                if (resource == null) {
                    String rel = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
                    File file = GamePaths.bundledFile(rel);
                    if (file.exists()) resource = file.toURI().toURL();
                }
                
                if (resource != null) {
                    Media media = new Media(resource.toExternalForm());
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    mediaPlayer.setMute(true);
                    
                    MediaView mediaView = new MediaView(mediaPlayer);
                    mediaView.setSmooth(true);
                    
                    StackPane root = new StackPane(mediaView);
                    root.setStyle("-fx-background-color: black;");
                    Scene scene = new Scene(root, javafx.scene.paint.Color.BLACK);
                    jfxPanel.setScene(scene);
                    
                    mediaView.fitWidthProperty().bind(scene.widthProperty());
                    mediaView.fitHeightProperty().bind(scene.heightProperty());
                    mediaView.setPreserveRatio(false);
                    
                    mediaPlayer.setOnReady(() -> mediaPlayer.play());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Called when this screen becomes the active visible view.
     * Resumes background video playback and starts the game loop.
     */
    @Override
    public void refresh() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
                    mediaPlayer.play();
                }
            });
        }
        
        if (useGameLoop()) {
            gameLoop.start();
        }
        
        revalidate();
        repaint();
    }

    /**
     * Called when this screen is hidden or replaced by another screen.
     * Pauses the video background and halts the game loop to conserve system resources.
     */
    @Override
    public void pause() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> mediaPlayer.pause());
        }
        
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    /**
     * Override to return true if the screen requires a high-frequency game loop.
     */
    protected boolean useGameLoop() {
        return false;
    }

    /**
     * Called by the GameLoop for logic updates (off the EDT).
     */
    @Override
    public void update() {
        // Logic updates go here
    }

    /**
     * Called by the GameLoop to trigger rendering (on the EDT).
     */
    @Override
    public void render() {
        repaint();
    }
}
