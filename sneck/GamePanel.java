
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;
    private static final int UNIT_SIZE = 25;
    private static final int DELAY = 100;

    private ArrayList snake;
    private Point food;
    private char direction = 'R';
    private boolean running = false;
    private Timer timer;
    private Random random;

    public GamePanel() {
        random = new Random();
        snake = new ArrayList();
        initGame();
    }

    private void initGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new MyKeyAdapter());
        startGame();
    }

    public void startGame() {
        snake.clear();
        int i;
        for (i = 0; i &lt; 3; i++) {
            snake.add(new Point(UNIT_SIZE * 2 - i * UNIT_SIZE, UNIT_SIZE * 2));
        }
        newFood();
        direction = 'R';
        running = true;
        timer = new Timer(DELAY, this);
        timer.start();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    private void draw(Graphics g) {
        if (running) {
            g.setColor(Color.RED);
            g.fillOval(food.x, food.y, UNIT_SIZE, UNIT_SIZE);

            int i;
            for (i = 0; i &lt; snake.size(); i++) {
                if (i == 0) {
                    g.setColor(Color.GREEN);
                } else {
                    g.setColor(new Color(45, 180, 0));
                }
                Point p = (Point) snake.get(i);
                g.fillRect(p.x, p.y, UNIT_SIZE, UNIT_SIZE);
            }

            drawScore(g);
        } else {
            gameOver(g);
        }
    }

    private void newFood() {
        int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
        int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
        food = new Point(x, y);
    }

    private void move() {
        Point head = (Point) snake.get(0);
        Point newHead = new Point(head.x, head.y);

        if (direction == 'U') {
            newHead.y -= UNIT_SIZE;
        } else if (direction == 'D') {
            newHead.y += UNIT_SIZE;
        } else if (direction == 'L') {
            newHead.x -= UNIT_SIZE;
        } else if (direction == 'R') {
            newHead.x += UNIT_SIZE;
        }

        snake.add(0, newHead);

        if (head.equals(food)) {
            newFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private void checkCollisions() {
        Point head = (Point) snake.get(0);
        int i;

        for (i = snake.size() - 1; i &gt; 0; i--) {
            if (head.equals(snake.get(i))) {
                running = false;
            }
        }

        if (head.x &lt; 0 || head.x &gt;= WIDTH) {
            running = false;
        }

        if (head.y &lt; 0 || head.y &gt;= HEIGHT) {
            running = false;
        }

        if (!running) {
            timer.stop();
        }
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics metrics = getFontMetrics(g.getFont());
        String scoreText = "Score: " + (snake.size() - 3);
        g.drawString(scoreText, (WIDTH - metrics.stringWidth(scoreText)) / 2, g.getFont().getSize());
    }

    private void gameOver(Graphics g) {
        drawScore(g);
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 75));
        FontMetrics metrics1 = getFontMetrics(g.getFont());
        g.drawString("Game Over", (WIDTH - metrics1.stringWidth("Game Over")) / 2, HEIGHT / 2);
        
        g.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        g.drawString("Press Space to Restart", (WIDTH - metrics2.stringWidth("Press Space to Restart")) / 2, HEIGHT / 2 + 60);
    }

    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkCollisions();
        }
        repaint();
    }

    public class MyKeyAdapter extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            
            if (key == KeyEvent.VK_LEFT) {
                if (direction != 'R') {
                    direction = 'L';
                }
            } else if (key == KeyEvent.VK_RIGHT) {
                if (direction != 'L') {
                    direction = 'R';
                }
            } else if (key == KeyEvent.VK_UP) {
                if (direction != 'D') {
                    direction = 'U';
                }
            } else if (key == KeyEvent.VK_DOWN) {
                if (direction != 'U') {
                    direction = 'D';
                }
            } else if (key == KeyEvent.VK_SPACE) {
                if (!running) {
                    startGame();
                }
            }
        }
    }
}
