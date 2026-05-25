
package snakegame;

import javax.swing.*;

public class SnakeGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new GameFrame();
            }
        });
    }
}
