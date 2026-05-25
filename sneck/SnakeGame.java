
import javax.swing.*;

public class SnakeGame extends JFrame {
    public SnakeGame() {
        add(new GamePanel());
        setResizable(false);
        pack();
        setTitle("Snake Game");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SnakeGame game = new SnakeGame();
                game.setVisible(true);
            }
        });
    }
}
