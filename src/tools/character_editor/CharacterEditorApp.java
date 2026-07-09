package tools.character_editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

public class CharacterEditorApp extends JFrame {

    private static final long serialVersionUID = 1L;

    public static void main(String[] args) {
        // Set Look and Feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            try {
                CharacterEditorApp frame = new CharacterEditorApp();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public CharacterEditorApp() {
        setTitle("Character & Animation Editor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(new Dimension(1600, 900));
        setLocationRelativeTo(null); // Center on screen

        // Initialize JavaFX toolkit since some editors use JavaFX components
        new JFXPanel(); 
        Platform.setImplicitExit(false);

        // Add Window listener to exit properly when closed
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("Character Design", new CharacterDesignScreen());
        tabbedPane.addTab("Battle Animation", new AnimationEditorScreen());
        tabbedPane.addTab("Map Unit Animation", new MapUnitAnimationScreen());
        
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }
}
