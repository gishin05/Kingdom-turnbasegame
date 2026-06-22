package game.ui.util;

import javax.swing.*;
import java.awt.*;

/**
 * UI DESIGN OVERVIEW:
 * This component is part of the pixelated game UI approach.
 * It integrates with the layered architecture, utilizing dynamic backgrounds
 * and semi-transparent panels to maintain a visually rich, modern aesthetic.
 */

/**
 * A reusable progress dialog for import/load operations.
 * Runs the work on a background thread via SwingWorker so the UI stays responsive.
 */
public class ImportProgressDialog {

    /**
     * Callback interface for performing the actual import work.
     */
    public interface ImportTask {
        /**
         * Called on a background thread to do the import.
         * @param updater call updater.update(current, total, message) to update progress
         */
        void run(ProgressUpdater updater) throws Exception;
    }

    public interface ProgressUpdater {
        void update(int current, int total, String message);
    }

    /**
     * Show a progress dialog and run the given task in the background.
     * @param parent     the parent component for centering
     * @param title      dialog title
     * @param task       the import task to execute
     * @param onComplete optional runnable to execute on EDT when done (can be null)
     */
    public static void run(Component parent, String title, ImportTask task, Runnable onComplete) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(window, title, Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblStatus = new JLabel("Initializing...");
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(350, 25));

        dialog.add(lblStatus, BorderLayout.NORTH);
        dialog.add(progressBar, BorderLayout.CENTER);
        dialog.setSize(400, 100);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        SwingWorker<Void, int[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run((current, total, message) -> {
                    SwingUtilities.invokeLater(() -> {
                        if (total > 0) {
                            progressBar.setMaximum(total);
                            progressBar.setValue(current);
                            progressBar.setString(current + " / " + total);
                        }
                        if (message != null) {
                            lblStatus.setText(message);
                        }
                    });
                });
                return null;
            }

            @Override
            protected void done() {
                dialog.dispose();
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
            }
        };
        worker.execute();
    }
}
