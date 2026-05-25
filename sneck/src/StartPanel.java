
package snakegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StartPanel extends JPanel {
    private GameFrame frame;
    private static final int WIDTH = 720;
    private static final int HEIGHT = 720;
    
    public StartPanel(GameFrame frame) {
        this.frame = frame;
        initPanel();
    }
    
    private void initPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setLayout(null);
        
        addContent();
    }
    
    private void addContent() {
        Font font = new Font("微软雅黑", Font.PLAIN, 18);
        
        JTextArea content = new JTextArea();
        content.setFont(font);
        content.setBackground(Color.BLACK);
        content.setForeground(Color.GREEN);
        content.setEditable(false);
        content.setLineWrap(false);
        content.setWrapStyleWord(false);
        
        String text = 
            "                        SNAKE GAME\n\n" +
            "        ------------------------------\n\n" +
            "  [操作方式]\n" +
            "    WASD 或 方向键 : 控制蛇的移动方向\n\n" +
            "  [道具说明]\n" +
            "    红色豆子 : 吃了增加分数并生成蓝色柱子\n" +
            "    金色豆子 : 吃了获得5秒无敌时间\n" +
            "    蓝色柱子 : 无敌期间撞击会摧毁并加300分\n\n" +
            "  [快捷键]\n" +
            "    ESC     : 暂停/继续游戏\n" +
            "    空格键   : 加速移动\n" +
            "    Shift   : 减速移动\n\n" +
            "  [计分规则]\n" +
            "    吃豆       : +100分\n" +
            "    摧毁蓝柱   : +300分\n" +
            "    时间惩罚   : -5分/秒\n\n" +
            "  [游戏提示]\n" +
            "    金色豆子每30秒出现一次\n" +
            "    无敌期间可以安全摧毁蓝色柱子\n" +
            "    蛇碰到自己身体不会死亡\n\n" +
            "        ------------------------------\n\n";
        
        content.setText(text);
        content.setBounds(50, 50, WIDTH - 100, 550);
        add(content);
        
        JButton startButton = new JButton("按 ENTER 开始游戏");
        startButton.setFont(new Font("微软雅黑", Font.BOLD, 20));
        startButton.setBackground(Color.BLACK);
        startButton.setForeground(Color.GREEN);
        startButton.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        startButton.setFocusPainted(false);
        startButton.setBounds((WIDTH - 280) / 2, 600, 280, 45);
        startButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.startGame();
            }
        });
        
        startButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startButton.setForeground(Color.WHITE);
                startButton.setBorder(BorderFactory.createLineBorder(Color.WHITE));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startButton.setForeground(Color.GREEN);
                startButton.setBorder(BorderFactory.createLineBorder(Color.GREEN));
            }
        });
        
        add(startButton);
        
        JLabel copyright = new JLabel("2024 Snake Game");
        copyright.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        copyright.setForeground(Color.DARK_GRAY);
        copyright.setBounds((WIDTH - 150) / 2, 670, 150, 30);
        copyright.setHorizontalAlignment(SwingConstants.CENTER);
        add(copyright);
        
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "start");
        getActionMap().put("start", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                frame.startGame();
            }
        });
    }
}
