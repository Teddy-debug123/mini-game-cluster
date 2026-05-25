
package snakegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameFrame extends JFrame {
    private StartPanel startPanel;
    private GamePanel gamePanel;
    
    public GameFrame() {
        setTitle("贪吃蛇游戏");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        startPanel = new StartPanel(this);
        gamePanel = new GamePanel(this);
        
        add(startPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    public void startGame() {
        remove(startPanel);
        add(gamePanel);
        gamePanel.startGame();
        pack();
        revalidate();
        repaint();
    }
    
    public void showStartScreen() {
        remove(gamePanel);
        gamePanel = new GamePanel(this);
        add(startPanel);
        pack();
        revalidate();
        repaint();
    }
}
