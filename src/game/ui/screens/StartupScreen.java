package game.ui.screens;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import game.Main;
import game.ui.BaseScreen;
import game.ui.Theme;
import game.core.util.AssetManager;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */

/**
 * The initial loading screen displayed when the game launches.
 * Responsible for preloading essential assets in a background thread
 * while displaying a status message to the user.
 */
public class StartupScreen extends BaseScreen {

    private static final long serialVersionUID = 1L;
    private JLabel lblStatus;

    public StartupScreen(Main main) {
        super(main);
        setBackground(Color.BLACK);
        setLayout(new GridBagLayout());

        JLabel lblLogo = new JLabel("SORAKIRRA SYSTEM");
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setFont(Theme.getPixelFont(32f));
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        
        lblStatus = new JLabel("INITIALIZING CORE...");
        lblStatus.setForeground(Color.GRAY);
        lblStatus.setFont(Theme.getPixelFont(14f));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(lblLogo, gbc);
        
        gbc.gridy = 1;
        gbc.insets = new java.awt.Insets(20, 0, 0, 0);
        add(lblStatus, gbc);
    }

    @Override
    public void refresh() {
        super.refresh();
        
        // Start resource preloading in a separate background thread 
        // to prevent freezing the Event Dispatch Thread (UI).
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Dramatic pause
                SwingUtilities.invokeLater(() -> lblStatus.setText("PRELOADING ASSETS..."));
                
                AssetManager.preloadEssentialAssets();
                
                Thread.sleep(500); // Let user see the completion
                SwingUtilities.invokeLater(() -> main.showScreen(Main.TITLE));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
