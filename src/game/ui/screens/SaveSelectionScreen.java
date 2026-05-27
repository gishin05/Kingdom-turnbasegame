package game.ui.screens;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import game.Main;
import game.core.save.SaveManager;
import game.ui.BaseScreen;
import game.ui.Theme;
import game.ui.components.StyledButton;

public class SaveSelectionScreen extends BaseScreen {

    private static final long serialVersionUID = 1L;
    private JPanel slotsPanel;

    public SaveSelectionScreen(Main main) {
        super(main);
        initVideoBackground(game.core.util.GamePaths.bundledResource("graphics/backgrounds/Menu_bg.mp4"));

        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Layer 0: Video background (BaseScreen)
        layeredPane.add(jfxPanel, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: Dark Overlay
        JPanel overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        overlayPanel.setOpaque(false);
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);

        // Layer 2: UI Content
        JPanel uiPanel = new JPanel(new BorderLayout());
        uiPanel.setOpaque(false);
        uiPanel.setBorder(new EmptyBorder(60, 100, 60, 100));
        layeredPane.add(uiPanel, JLayeredPane.MODAL_LAYER);

        JLabel lblTitle = new JLabel("SELECT SAVE SLOT");
        lblTitle.setForeground(Theme.GOLD);
        lblTitle.setFont(Theme.getTitleFont());
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setBorder(new EmptyBorder(0, 0, 40, 0));
        uiPanel.add(lblTitle, BorderLayout.NORTH);

        slotsPanel = new JPanel(new GridLayout(3, 1, 0, 30));
        slotsPanel.setOpaque(false);
        uiPanel.add(slotsPanel, BorderLayout.CENTER);

        StyledButton btnBack = new StyledButton("BACK", Theme.getSmallFont());
        btnBack.setPreferredSize(new Dimension(280, 60));
        btnBack.addActionListener(e -> main.showScreen(Main.MENU));
        
        JPanel southPanel = new JPanel();
        southPanel.setOpaque(false);
        southPanel.setBorder(new EmptyBorder(30, 0, 0, 0));
        southPanel.add(btnBack);
        uiPanel.add(southPanel, BorderLayout.SOUTH);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                jfxPanel.setBounds(0, 0, w, h);
                overlayPanel.setBounds(0, 0, w, h);
                uiPanel.setBounds(0, 0, w, h);
            }
        });
    }

    @Override
    public void refresh() {
        super.refresh();
        slotsPanel.removeAll();
        for (int i = 1; i <= 3; i++) {
            final int slot = i;
            String text = SaveManager.hasSave(slot) ? "SAVE SLOT " + slot + " (OCCUPIED)" : "SAVE SLOT " + slot + " (EMPTY)";
            StyledButton btnSlot = new StyledButton(text, Theme.getMenuFont());
            btnSlot.setPreferredSize(new Dimension(280, 60));
            
            btnSlot.addActionListener(e -> {
                if (SaveManager.hasSave(slot)) {
                    int choice = JOptionPane.showConfirmDialog(this, 
                        "Existing save found in Slot " + slot + ". Overwrite?", 
                        "Confirm New Game", 
                        JOptionPane.YES_NO_OPTION);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        SaveManager.deleteSave(slot);
                        startNewGame(slot);
                    }
                } else {
                    startNewGame(slot);
                }
            });
            slotsPanel.add(btnSlot);
        }
        slotsPanel.revalidate();
        slotsPanel.repaint();
    }

    private void startNewGame(int slot) {
        SaveManager.createNewSave(slot);
        JOptionPane.showMessageDialog(this, "New Game started in Slot " + slot + "!");
    }
}
