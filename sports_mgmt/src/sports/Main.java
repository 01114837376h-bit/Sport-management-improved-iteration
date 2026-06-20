package sports;

import sports.gui.MainWindow;

import javax.swing.*;


public class Main {
    public static void main(String[] args) {
        // Always start Swing on the Event Dispatch Thread
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
