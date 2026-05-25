
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class Snake extends JFrame {
    
    public Snake() {
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
                Snake game = new Snake();
                game.setVisible(true);
            }
        });
    }

    class GamePanel extends JPanel implements ActionListener {
        private static final int WIDTH = 600;
        private static final int HEIGHT = 600;
        private static final int UNIT_SIZE = 25;
        private static final int DELAY = 150;
        private static final int FAST_DELAY = 75;
        private static final int SLOW_DELAY = 300;
        private static final long INVINCIBLE_TIME = 5000;
        private static final int GOLDEN_FOOD_INTERVAL = 30000;
        private static final int PILLAR_DESTROY_SCORE = 300;

        private ArrayList snake;
        private Point food;
        private Point goldenFood;
        private ArrayList pillars;
        private char direction = 'R';
        private boolean running = false;
        private Timer timer;
        private Random random;
        private int score;
        private int foodCount;
        private int pillarDestroyCount;
        private long startTime;
        private boolean isFast = false;
        private boolean isSlow = false;
        private boolean isInvincible = false;
        private long invincibleEndTime;
        private boolean goldenFoodActive = false;
        private long lastGoldenFoodTime;

        public GamePanel() {
            random = new Random();
            snake = new ArrayList();
            pillars = new ArrayList();
            initGame();
        }

        private void initGame() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(Color.DARK_GRAY);
            setFocusable(true);
            addKeyListener(new MyKeyAdapter());
            startGame();
        }

        public void startGame() {
            snake.clear();
            pillars.clear();
            int i;
            for (i = 0; i < 3; i++) {
                snake.add(new Point(UNIT_SIZE * 2 - i * UNIT_SIZE, UNIT_SIZE * 2));
            }
            newFood();
            goldenFood = null;
            goldenFoodActive = false;
            lastGoldenFoodTime = System.currentTimeMillis();
            direction = 'R';
            running = true;
            score = 0;
            foodCount = 0;
            pillarDestroyCount = 0;
            startTime = System.currentTimeMillis();
            isFast = false;
            isSlow = false;
            isInvincible = false;
            invincibleEndTime = 0;
            timer = new Timer(DELAY, this);
            timer.start();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawGrid(g);
            draw(g);
        }

        private void drawGrid(Graphics g) {
            g.setColor(new Color(50, 50, 50));
            int i;
            for (i = 0; i <= WIDTH / UNIT_SIZE; i++) {
                g.drawLine(i * UNIT_SIZE, 0, i * UNIT_SIZE, HEIGHT);
            }
            for (i = 0; i <= HEIGHT / UNIT_SIZE; i++) {
                g.drawLine(0, i * UNIT_SIZE, WIDTH, i * UNIT_SIZE);
            }
        }

        private void draw(Graphics g) {
            if (running) {
                g.setColor(Color.RED);
                g.fillOval(food.x, food.y, UNIT_SIZE, UNIT_SIZE);

                if (goldenFoodActive && goldenFood != null) {
                    g.setColor(Color.YELLOW);
                    g.fillOval(goldenFood.x, goldenFood.y, UNIT_SIZE, UNIT_SIZE);
                    g.setColor(Color.ORANGE);
                    g.drawOval(goldenFood.x, goldenFood.y, UNIT_SIZE, UNIT_SIZE);
                }

                int i;
                for (i = 0; i < pillars.size(); i++) {
                    Point p = (Point) pillars.get(i);
                    g.setColor(Color.BLUE);
                    g.fillRect(p.x, p.y, UNIT_SIZE, UNIT_SIZE);
                }

                for (i = 0; i < snake.size(); i++) {
                    if (i == 0) {
                        if (isInvincible) {
                            g.setColor(Color.CYAN);
                        } else {
                            g.setColor(Color.GREEN);
                        }
                    } else {
                        if (isInvincible) {
                            g.setColor(new Color(100, 200, 255));
                        } else {
                            g.setColor(new Color(45, 180, 0));
                        }
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
            
            int i;
            for (i = 0; i < snake.size(); i++) {
                if (food.equals(snake.get(i))) {
                    newFood();
                    return;
                }
            }
            for (i = 0; i < pillars.size(); i++) {
                if (food.equals(pillars.get(i))) {
                    newFood();
                    return;
                }
            }
        }

        private void spawnGoldenFood() {
            if (!goldenFoodActive && running) {
                int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
                int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
                goldenFood = new Point(x, y);
                
                int i;
                for (i = 0; i < snake.size(); i++) {
                    if (goldenFood.equals(snake.get(i))) {
                        spawnGoldenFood();
                        return;
                    }
                }
                for (i = 0; i < pillars.size(); i++) {
                    if (goldenFood.equals(pillars.get(i))) {
                        spawnGoldenFood();
                        return;
                    }
                }
                if (goldenFood.equals(food)) {
                    spawnGoldenFood();
                    return;
                }
                
                goldenFoodActive = true;
            }
        }

        private void move() {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastGoldenFoodTime >= GOLDEN_FOOD_INTERVAL) {
                spawnGoldenFood();
                lastGoldenFoodTime = currentTime;
            }

            if (isInvincible && currentTime >= invincibleEndTime) {
                isInvincible = false;
            }

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

            if (newHead.x < 0) {
                newHead.x = WIDTH - UNIT_SIZE;
            } else if (newHead.x >= WIDTH) {
                newHead.x = 0;
            }
            if (newHead.y < 0) {
                newHead.y = HEIGHT - UNIT_SIZE;
            } else if (newHead.y >= HEIGHT) {
                newHead.y = 0;
            }

            snake.add(0, newHead);

            if (newHead.equals(food)) {
                pillars.add(new Point(head.x, head.y));
                foodCount++;
                newFood();
            } else if (goldenFoodActive && newHead.equals(goldenFood)) {
                isInvincible = true;
                invincibleEndTime = currentTime + INVINCIBLE_TIME;
                goldenFoodActive = false;
                goldenFood = null;
            } else {
                snake.remove(snake.size() - 1);
            }
        }

        private void checkCollisions() {
            if (!running) return;
            
            Point head = (Point) snake.get(0);
            int i;

            for (i = 0; i < pillars.size(); i++) {
                if (head.equals(pillars.get(i))) {
                    if (isInvincible) {
                        pillars.remove(i);
                        pillarDestroyCount++;
                    } else {
                        running = false;
                    }
                    break;
                }
            }

            if (!running) {
                timer.stop();
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                score = foodCount * 100 + pillarDestroyCount * PILLAR_DESTROY_SCORE - (int) elapsedTime * 5;
                if (score < 0) score = 0;
            }
        }

        private void drawScore(Graphics g) {
            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            int currentScore = foodCount * 100 + pillarDestroyCount * PILLAR_DESTROY_SCORE - (int) elapsedTime * 5;
            if (currentScore < 0) currentScore = 0;
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Score: " + currentScore, 10, 30);
            g.drawString("Food: " + foodCount, 10, 55);
            g.drawString("Time: " + elapsedTime + "s", WIDTH - 120, 30);
            
            if (isFast) {
                g.setColor(Color.YELLOW);
                g.drawString("FAST!", WIDTH - 80, 55);
            } else if (isSlow) {
                g.setColor(Color.BLUE);
                g.drawString("SLOW!", WIDTH - 80, 55);
            }
            
            if (isInvincible) {
                long remainingTime = (invincibleEndTime - System.currentTimeMillis()) / 1000;
                g.setColor(Color.CYAN);
                g.drawString("INVINCIBLE: " + remainingTime + "s", 10, 80);
            }
            
            if (goldenFoodActive) {
                g.setColor(Color.YELLOW);
                g.drawString("GOLDEN BEAN!", WIDTH - 150, 55);
            }
            
            g.drawString("Pillars: " + pillarDestroyCount, WIDTH - 130, 55);
        }

        private void gameOver(Graphics g) {
            drawGrid(g);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 60));
            FontMetrics metrics1 = getFontMetrics(g.getFont());
            g.drawString("Game Over", (WIDTH - metrics1.stringWidth("Game Over")) / 2, HEIGHT / 2 - 80);
            
            g.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics metrics2 = getFontMetrics(g.getFont());
            g.drawString("Final Score: " + score, (WIDTH - metrics2.stringWidth("Final Score: " + score)) / 2, HEIGHT / 2);
            
            g.setFont(new Font("Arial", Font.BOLD, 25));
            FontMetrics metrics3 = getFontMetrics(g.getFont());
            g.drawString("Press Space to Restart", (WIDTH - metrics3.stringWidth("Press Space to Restart")) / 2, HEIGHT / 2 + 60);
        }

        public void actionPerformed(ActionEvent e) {
            if (running) {
                move();
                checkCollisions();
            }
            repaint();
        }

        class MyKeyAdapter extends KeyAdapter {
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
                    if (direction != 'R') {
                        direction = 'L';
                    }
                } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
                    if (direction != 'L') {
                        direction = 'R';
                    }
                } else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
                    if (direction != 'D') {
                        direction = 'U';
                    }
                } else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                    if (direction != 'U') {
                        direction = 'D';
                    }
                } else if (key == KeyEvent.VK_SHIFT) {
                    if (!running) {
                        startGame();
                    } else {
                        isSlow = !isSlow;
                        if (isSlow) {
                            isFast = false;
                            timer.setDelay(SLOW_DELAY);
                        } else {
                            timer.setDelay(isFast ? FAST_DELAY : DELAY);
                        }
                    }
                } else if (key == KeyEvent.VK_SPACE) {
                    if (!running) {
                        startGame();
                    } else {
                        isFast = !isFast;
                        if (isFast) {
                            isSlow = false;
                            timer.setDelay(FAST_DELAY);
                        } else {
                            timer.setDelay(isSlow ? SLOW_DELAY : DELAY);
                        }
                    }
                }
            }
        }
    }
}
