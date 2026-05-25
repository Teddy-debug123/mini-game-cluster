package snakegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.io.*;

public class GamePanel extends JPanel implements ActionListener {
    private GameFrame frame;
    
    private static final int WIDTH = 720;
    private static final int HEIGHT = 720;
    private static final int UNIT_SIZE = 30;
    private static final int BASE_DELAY = 220;
    private static final int FAST_DELAY = 110;
    private static final int SLOW_DELAY = 440;
    private static final long INVINCIBLE_TIME = 8000;
    private static final int BASE_GOLDEN_FOOD_INTERVAL = 25000;
    private static final int MIN_GOLDEN_FOOD_INTERVAL = 10000;
    private static final int PILLAR_DESTROY_SCORE = 300;
    private static final int MAX_PILLARS = 15;
    private static final int SAFE_TIME = 5000;
    private static final int SPEED_INCREMENT = 5;
    private static final int SPEED_INCREMENT_INTERVAL = 5;
    private static final int COMBO_TIMEOUT = 2000;

    private ArrayList<Point> snake;
    private Point food;
    private Point goldenFood;
    private ArrayList<Point> pillars;
    private char direction = 'R';
    private boolean running = false;
    private boolean paused = false;
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
    private int currentDelay;
    private int comboCount;
    private long lastComboTime;
    private int highScore;
    private boolean flashEffect = false;
    private boolean pillarFlash = false;
    private Point flashPillar = null;

    public GamePanel(GameFrame frame) {
        this.frame = frame;
        random = new Random();
        snake = new ArrayList<>();
        pillars = new ArrayList<>();
        loadHighScore();
        initPanel();
    }

    private void initPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(new MyKeyAdapter());
    }

    public void startGame() {
        snake.clear();
        pillars.clear();
        for (int i = 0; i < 3; i++) {
            snake.add(new Point(UNIT_SIZE * 2 - i * UNIT_SIZE, UNIT_SIZE * 2));
        }
        newFood();
        goldenFood = null;
        goldenFoodActive = false;
        lastGoldenFoodTime = System.currentTimeMillis();
        direction = 'R';
        running = true;
        paused = false;
        score = 0;
        foodCount = 0;
        pillarDestroyCount = 0;
        startTime = System.currentTimeMillis();
        isFast = false;
        isSlow = false;
        isInvincible = false;
        invincibleEndTime = 0;
        currentDelay = BASE_DELAY;
        comboCount = 0;
        lastComboTime = 0;
        flashEffect = false;
        pillarFlash = false;
        flashPillar = null;
        timer = new Timer(currentDelay, this);
        timer.start();
        requestFocus();
    }

    private void pauseGame() {
        paused = !paused;
        if (paused) {
            timer.stop();
        } else {
            timer.start();
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGrid(g);
        draw(g);
        if (paused) {
            drawPauseScreen(g);
        }
    }

    private void drawGrid(Graphics g) {
        g.setColor(new Color(50, 50, 50));
        for (int i = 0; i <= WIDTH / UNIT_SIZE; i++) {
            g.drawLine(i * UNIT_SIZE, 0, i * UNIT_SIZE, HEIGHT);
        }
        for (int i = 0; i <= HEIGHT / UNIT_SIZE; i++) {
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
                if (System.currentTimeMillis() % 500 < 250) {
                    g.drawOval(goldenFood.x - 2, goldenFood.y - 2, UNIT_SIZE + 4, UNIT_SIZE + 4);
                }
            }

            for (Point p : pillars) {
                if (pillarFlash && flashPillar != null && flashPillar.equals(p)) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Color.BLUE);
                }
                g.fillRect(p.x, p.y, UNIT_SIZE, UNIT_SIZE);
            }
            if (pillarFlash) {
                pillarFlash = false;
                flashPillar = null;
            }

            boolean shouldFlash = isInvincible && (System.currentTimeMillis() % 200 < 100);
            for (int i = 0; i < snake.size(); i++) {
                Point p = snake.get(i);
                if (i == 0) {
                    if (isInvincible && shouldFlash) {
                        g.setColor(Color.WHITE);
                    } else {
                        g.setColor(isInvincible ? Color.CYAN : Color.GREEN);
                    }
                } else {
                    if (isInvincible && shouldFlash) {
                        g.setColor(new Color(200, 200, 255));
                    } else {
                        g.setColor(isInvincible ? new Color(100, 200, 255) : new Color(45, 180, 0));
                    }
                }
                g.fillRect(p.x, p.y, UNIT_SIZE, UNIT_SIZE);
            }

            if (isFast) {
                g.setColor(Color.YELLOW);
                g.fillRect(0, HEIGHT - 5, WIDTH, 5);
            }

            drawScore(g);
        } else {
            gameOver(g);
        }
    }

    private void drawScore(Graphics g) {
        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        int baseScore = foodCount * 100 + pillarDestroyCount * PILLAR_DESTROY_SCORE;
        int timePenalty = 0;
        if (elapsedTime * 1000 > SAFE_TIME) {
            timePenalty = (int)(elapsedTime - SAFE_TIME / 1000) * 5;
        }
        int comboBonus = comboCount * 10;
        int currentScore = baseScore - timePenalty + comboBonus;
        if (currentScore < 0) currentScore = 0;

        g.setColor(Color.WHITE);
        g.setFont(new Font("微软雅黑", Font.BOLD, 20));
        g.drawString("得分: " + currentScore, 15, 30);
        g.drawString("豆子: " + foodCount, 15, 55);
        g.drawString("时间: " + elapsedTime + "s", WIDTH - 140, 30);

        if (isFast) {
            g.setColor(Color.YELLOW);
            g.drawString("快速!", WIDTH - 85, 55);
        } else if (isSlow) {
            g.setColor(Color.BLUE);
            g.drawString("慢速!", WIDTH - 85, 55);
        }

        if (isInvincible) {
            long remainingTime = (invincibleEndTime - System.currentTimeMillis()) / 1000;
            g.setColor(Color.CYAN);
            g.drawString("无敌: " + remainingTime + "s", 15, 80);
        }

        if (goldenFoodActive) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g.drawString("⭐⭐ 金色豆子出现!", WIDTH - 180, 55);
        }

        g.drawString("摧毁: " + pillarDestroyCount, WIDTH - 140, 55);
        
        if (comboCount > 1) {
            g.setColor(Color.ORANGE);
            g.drawString("连击 x" + comboCount, 15, 105);
        }
        
        g.drawString("最高分: " + highScore, WIDTH - 140, 80);
    }

    private void drawPauseScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("微软雅黑", Font.BOLD, 60));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("游戏暂停", (WIDTH - metrics.stringWidth("游戏暂停")) / 2, HEIGHT / 2 - 50);

        g.setFont(new Font("微软雅黑", Font.BOLD, 25));
        metrics = getFontMetrics(g.getFont());
        g.drawString("按 ESC 键继续游戏", (WIDTH - metrics.stringWidth("按 ESC 键继续游戏")) / 2, HEIGHT / 2 + 20);
        g.drawString("按 R 键重新开始", (WIDTH - metrics.stringWidth("按 R 键重新开始")) / 2, HEIGHT / 2 + 55);
        g.drawString("按 Q 键返回主菜单", (WIDTH - metrics.stringWidth("按 Q 键返回主菜单")) / 2, HEIGHT / 2 + 90);
    }

    private void newFood() {
        int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
        int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
        food = new Point(x, y);

        for (Point p : snake) {
            if (food.equals(p)) {
                newFood();
                return;
            }
        }
        for (Point p : pillars) {
            if (food.equals(p)) {
                newFood();
                return;
            }
        }
        if (goldenFood != null && food.equals(goldenFood)) {
            newFood();
        }
    }

    private void spawnGoldenFood() {
        if (!goldenFoodActive && running) {
            int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
            int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
            goldenFood = new Point(x, y);

            for (Point p : snake) {
                if (goldenFood.equals(p)) {
                    spawnGoldenFood();
                    return;
                }
            }
            for (Point p : pillars) {
                if (goldenFood.equals(p)) {
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

        int goldenInterval = Math.max(MIN_GOLDEN_FOOD_INTERVAL, BASE_GOLDEN_FOOD_INTERVAL - foodCount * 500);
        if (currentTime - lastGoldenFoodTime >= goldenInterval) {
            spawnGoldenFood();
            lastGoldenFoodTime = currentTime;
        }

        if (isInvincible && currentTime >= invincibleEndTime) {
            isInvincible = false;
        }

        Point head = snake.get(0);
        Point newHead = new Point(head.x, head.y);

        switch (direction) {
            case 'U':
                newHead.y -= UNIT_SIZE;
                break;
            case 'D':
                newHead.y += UNIT_SIZE;
                break;
            case 'L':
                newHead.x -= UNIT_SIZE;
                break;
            case 'R':
                newHead.x += UNIT_SIZE;
                break;
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
            if (currentTime - lastComboTime < COMBO_TIMEOUT) {
                comboCount++;
            } else {
                comboCount = 1;
            }
            lastComboTime = currentTime;
            
            if (pillars.size() < MAX_PILLARS) {
                float spawnProbability = Math.max(0.3f, 1.0f - (float)pillars.size() / MAX_PILLARS);
                if (random.nextFloat() < spawnProbability) {
                    pillars.add(new Point(head.x, head.y));
                }
            }
            foodCount++;
            
            int speedReduction = (foodCount / SPEED_INCREMENT_INTERVAL) * SPEED_INCREMENT;
            int newDelay = BASE_DELAY - speedReduction;
            if (newDelay < FAST_DELAY) {
                newDelay = FAST_DELAY;
            }
            if (newDelay != currentDelay && !isFast && !isSlow) {
                currentDelay = newDelay;
                timer.setDelay(currentDelay);
            }
            
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
        if (!running || paused) return;

        Point head = snake.get(0);

        for (int i = 0; i < pillars.size(); i++) {
            if (head.equals(pillars.get(i))) {
                if (isInvincible) {
                    flashPillar = pillars.get(i);
                    pillarFlash = true;
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
            int baseScore = foodCount * 100 + pillarDestroyCount * PILLAR_DESTROY_SCORE;
            int timePenalty = 0;
            if (elapsedTime * 1000 > SAFE_TIME) {
                timePenalty = (int)(elapsedTime - SAFE_TIME / 1000) * 5;
            }
            score = baseScore - timePenalty;
            if (score < 0) score = 0;
            
            if (score > highScore) {
                highScore = score;
                saveHighScore();
            }
        }
    }

    private void gameOver(Graphics g) {
        drawGrid(g);

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("微软雅黑", Font.BOLD, 60));
        FontMetrics metrics1 = getFontMetrics(g.getFont());
        g.drawString("游戏结束", (WIDTH - metrics1.stringWidth("游戏结束")) / 2, HEIGHT / 2 - 120);

        g.setFont(new Font("微软雅黑", Font.BOLD, 35));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        g.drawString("最终得分: " + score, (WIDTH - metrics2.stringWidth("最终得分: " + score)) / 2, HEIGHT / 2 - 50);
        
        g.setFont(new Font("微软雅黑", Font.BOLD, 25));
        FontMetrics metrics3 = getFontMetrics(g.getFont());
        g.drawString("摧毁障碍物: " + pillarDestroyCount, (WIDTH - metrics3.stringWidth("摧毁障碍物: " + pillarDestroyCount)) / 2, HEIGHT / 2 - 10);
        
        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        g.drawString("游戏时长: " + elapsedTime + "秒", (WIDTH - metrics3.stringWidth("游戏时长: " + elapsedTime + "秒")) / 2, HEIGHT / 2 + 30);
        
        if (score >= highScore) {
            g.setColor(Color.YELLOW);
            g.drawString("🎉 新纪录!", (WIDTH - metrics3.stringWidth("🎉 新纪录!")) / 2, HEIGHT / 2 + 70);
        }

        g.setColor(Color.WHITE);
        g.drawString("按 R 键重新开始", (WIDTH - metrics3.stringWidth("按 R 键重新开始")) / 2, HEIGHT / 2 + 110);
        g.drawString("按 ESC 返回主菜单", (WIDTH - metrics3.stringWidth("按 ESC 返回主菜单")) / 2, HEIGHT / 2 + 145);
    }

    private void loadHighScore() {
        try {
            File file = new File("highscore.dat");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                if (line != null) {
                    highScore = Integer.parseInt(line);
                }
                reader.close();
            }
        } catch (Exception e) {
            highScore = 0;
        }
    }

    private void saveHighScore() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("highscore.dat"));
            writer.write(String.valueOf(highScore));
            writer.close();
        } catch (Exception e) {
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (running && !paused) {
            move();
            checkCollisions();
        }
        repaint();
    }

    class MyKeyAdapter extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_ESCAPE) {
                if (!running) {
                    frame.showStartScreen();
                } else {
                    pauseGame();
                }
                return;
            }

            if (key == KeyEvent.VK_Q) {
                if (paused || !running) {
                    frame.showStartScreen();
                }
                return;
            }

            if (key == KeyEvent.VK_R) {
                if (!running || paused) {
                    startGame();
                }
                return;
            }

            if (paused) return;

            if (!running) {
                if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_R) {
                    startGame();
                }
                return;
            }

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
                isSlow = true;
                isFast = false;
                timer.setDelay(SLOW_DELAY);
            } else if (key == KeyEvent.VK_SPACE) {
                isFast = true;
                isSlow = false;
                timer.setDelay(FAST_DELAY);
            }
        }

        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();

            if (!running || paused) return;

            if (key == KeyEvent.VK_SHIFT) {
                isSlow = false;
                timer.setDelay(isFast ? FAST_DELAY : currentDelay);
            } else if (key == KeyEvent.VK_SPACE) {
                isFast = false;
                timer.setDelay(isSlow ? SLOW_DELAY : currentDelay);
            }
        }
    }
}