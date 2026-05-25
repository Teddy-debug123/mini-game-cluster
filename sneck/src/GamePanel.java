package snakegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.io.*;
import javax.imageio.ImageIO;

public class GamePanel extends JPanel implements ActionListener {
    private GameFrame frame;
    
    private static final int WIDTH = 720;
    private static final int HEIGHT = 720;
    private static final int UNIT_SIZE = 30;
    private static final int BASE_DELAY = 220;
    private static final int FAST_DELAY = 110;
    private static final int SLOW_DELAY = 440;
    private static final int INVINCIBLE_DELAY = 110;
    private static final long INVINCIBLE_TIME = 8000;
    private static final int BASE_GOLDEN_FOOD_INTERVAL = 25000;
    private static final int MIN_GOLDEN_FOOD_INTERVAL = 10000;
    private static final int PURPLE_FOOD_INTERVAL = 120000;
    private static final int CYAN_FOOD_INTERVAL = 180000;
    private static final int PILLAR_DESTROY_SCORE = 300;
    private static final int MAX_PILLARS = 15;
    private static final int SAFE_TIME = 5000;
    private static final int SPEED_INCREMENT = 5;
    private static final int SPEED_INCREMENT_INTERVAL = 5;
    private static final int COMBO_TIMEOUT = 4000;
    
    private static final int NEGATIVE_BUFF_START_SCORE = 3000;
    private static final long NEGATIVE_BUFF_DURATION = 60000;
    private static final long NEGATIVE_BUFF_INTERVAL = 120000;
    private static final int RED_WALL_SECTION_DURATION = 20000;
    private static final int SPEED_BUFF_FOOD_MIN = 40;
    private static final int SPEED_BUFF_FOOD_MAX = 50;
    private static final long SPEED_BUFF_DURATION = 10000;
    private static final int RED_WALL_SPAWN_INTERVAL = 5000;
    private static final long RED_WALL_LIFETIME = 5000;

    private ArrayList<Point> snake;
    private Point food;
    private ArrayList<Point> goldenFoods;
    private Point purpleFood;
    private Point cyanFood;
    private ArrayList<Point> pillars;
    private ArrayList<RedWall> redWalls;
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
    private boolean purpleFoodActive = false;
    private boolean cyanFoodActive = false;
    private long lastGoldenFoodTime;
    private long lastPurpleFoodTime;
    private long lastCyanFoodTime;
    private int currentDelay;
    private int baseDelay;
    private int comboCount;
    private long lastComboTime;
    private boolean isFoodCombo;
    private String[] inventory = new String[3];
    private int selectedItem = -1;
    private boolean hasRespawnMarker = false;
    private int highScore;
    private boolean pillarFlash = false;
    private Point flashPillar = null;
    
    private boolean negativeBuffActive = false;
    private int negativeBuffEventCount = 0;
    private boolean negativeBuffUnlocked = false;
    private long lastNegativeBuffEndTime = 0;
    private boolean speedBuffActive = false;
    private long speedBuffEndTime = 0;
    private boolean speedBuffTriggered = false;
    private long negativeBuffEndTime;
    private long negativeBuffStartTime;
    private long lastRedWallSpawnTime;
    private long lastPausedTime;
    private long accumulatedPauseTime;
    
    private Image headSkin;
    private Image bodySkin;
    private boolean useSkin = false;
    private String currentSkin = "default";

    public GamePanel(GameFrame frame) {
        this.frame = frame;
        random = new Random();
        snake = new ArrayList<>();
        pillars = new ArrayList<>();
        redWalls = new ArrayList<>();
        loadHighScore();
        loadDefaultSkin();
        initPanel();
    }

    private void initPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(new MyKeyAdapter());
    }
    
    private void loadDefaultSkin() {
        headSkin = null;
        bodySkin = null;
        useSkin = false;
    }
    
    public void loadSkin(String skinName) {
        currentSkin = skinName;
        if ("default".equals(skinName)) {
            loadDefaultSkin();
            return;
        }
        
        try {
            File headFile = new File("skins/" + skinName + "_head.png");
            File bodyFile = new File("skins/" + skinName + "_body.png");
            
            if (headFile.exists() && bodyFile.exists()) {
                headSkin = ImageIO.read(headFile).getScaledInstance(UNIT_SIZE, UNIT_SIZE, Image.SCALE_SMOOTH);
                bodySkin = ImageIO.read(bodyFile).getScaledInstance(UNIT_SIZE, UNIT_SIZE, Image.SCALE_SMOOTH);
                useSkin = true;
            } else {
                loadDefaultSkin();
            }
        } catch (Exception e) {
            loadDefaultSkin();
        }
    }

    public void startGame() {
        snake.clear();
        pillars.clear();
        redWalls.clear();
        goldenFoods = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            snake.add(new Point(UNIT_SIZE * 2 - i * UNIT_SIZE, UNIT_SIZE * 2));
        }
        newFood();
        purpleFood = null;
        cyanFood = null;
        purpleFoodActive = false;
        cyanFoodActive = false;
        lastGoldenFoodTime = System.currentTimeMillis();
        lastPurpleFoodTime = System.currentTimeMillis();
        lastCyanFoodTime = System.currentTimeMillis();
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
        baseDelay = BASE_DELAY;
        currentDelay = BASE_DELAY;
        comboCount = 0;
        lastComboTime = 0;
        isFoodCombo = false;
        inventory = new String[3];
        selectedItem = -1;
        hasRespawnMarker = false;
        pillarFlash = false;
        flashPillar = null;
        negativeBuffActive = false;
        negativeBuffEventCount = 0;
        negativeBuffUnlocked = false;
        lastNegativeBuffEndTime = 0;
        speedBuffActive = false;
        speedBuffEndTime = 0;
        speedBuffTriggered = false;
        negativeBuffEndTime = 0;
        negativeBuffStartTime = 0;
        lastRedWallSpawnTime = 0;
        accumulatedPauseTime = 0;
        timer = new Timer(currentDelay, this);
        timer.start();
        requestFocus();
    }
    
    private void respawnSnake() {
        if (!running || paused) return;
        
        snake.clear();
        for (int i = 0; i < 3; i++) {
            snake.add(new Point(UNIT_SIZE * 2 - i * UNIT_SIZE, UNIT_SIZE * 2));
        }
        direction = 'R';
        isInvincible = true;
        invincibleEndTime = System.currentTimeMillis() + 3000;
        
        updateSpeed();
        requestFocus();
    }
    
    private void respawnAtOpenArea() {
        snake.clear();
        
        Point spawnPoint = findOpenArea();
        if (spawnPoint == null) {
            spawnPoint = new Point(UNIT_SIZE * 2, UNIT_SIZE * 2);
        }
        
        for (int i = 0; i < 3; i++) {
            snake.add(new Point(spawnPoint.x - i * UNIT_SIZE, spawnPoint.y));
        }
        direction = 'R';
        isInvincible = true;
        invincibleEndTime = System.currentTimeMillis() + 10000;
        hasRespawnMarker = false;
        updateSpeed();
    }
    
    private Point findOpenArea() {
        int maxAttempts = 100;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
            int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
            Point candidate = new Point(x, y);
            
            boolean isOpen = true;
            
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    Point checkPoint = new Point(x + i * UNIT_SIZE, y + j * UNIT_SIZE);
                    
                    if (checkPoint.x < 0 || checkPoint.x >= WIDTH || checkPoint.y < 0 || checkPoint.y >= HEIGHT) {
                        isOpen = false;
                        break;
                    }
                    
                    for (Point p : pillars) {
                        if (checkPoint.equals(p)) {
                            isOpen = false;
                            break;
                        }
                    }
                    if (!isOpen) break;
                    
                    for (RedWall wall : redWalls) {
                        if (checkPoint.equals(wall.position)) {
                            isOpen = false;
                            break;
                        }
                    }
                    if (!isOpen) break;
                }
                if (!isOpen) break;
            }
            
            if (isOpen) {
                return candidate;
            }
        }
        return null;
    }
    
    private void clearPillars() {
        if (!running || paused) return;
        pillars.clear();
    }

    private void pauseGame() {
        paused = !paused;
        if (paused) {
            lastPausedTime = System.currentTimeMillis();
            timer.stop();
        } else {
            accumulatedPauseTime += System.currentTimeMillis() - lastPausedTime;
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

            for (Point goldenFood : goldenFoods) {
                g.setColor(Color.YELLOW);
                g.fillOval(goldenFood.x, goldenFood.y, UNIT_SIZE, UNIT_SIZE);
                g.setColor(Color.ORANGE);
                g.drawOval(goldenFood.x, goldenFood.y, UNIT_SIZE, UNIT_SIZE);
                if (System.currentTimeMillis() % 500 < 250) {
                    g.drawOval(goldenFood.x - 2, goldenFood.y - 2, UNIT_SIZE + 4, UNIT_SIZE + 4);
                }
            }
            
            if (purpleFoodActive && purpleFood != null) {
                g.setColor(new Color(160, 32, 240));
                g.fillOval(purpleFood.x, purpleFood.y, UNIT_SIZE, UNIT_SIZE);
                g.setColor(new Color(128, 0, 128));
                g.drawOval(purpleFood.x, purpleFood.y, UNIT_SIZE, UNIT_SIZE);
            }
            
            if (cyanFoodActive && cyanFood != null) {
                g.setColor(new Color(0, 255, 255));
                g.fillOval(cyanFood.x, cyanFood.y, UNIT_SIZE, UNIT_SIZE);
                g.setColor(new Color(0, 128, 128));
                g.drawOval(cyanFood.x, cyanFood.y, UNIT_SIZE, UNIT_SIZE);
            }

            for (Point p : pillars) {
                if (pillarFlash && flashPillar != null && flashPillar.equals(p)) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Color.BLUE);
                }
                g.fillRect(p.x, p.y, UNIT_SIZE, UNIT_SIZE);
            }
            
            for (RedWall wall : redWalls) {
                float alpha = (float)(RED_WALL_LIFETIME - (System.currentTimeMillis() - wall.spawnTime)) / RED_WALL_LIFETIME;
                if (alpha < 0.3f) alpha = 0.3f;
                g.setColor(new Color(255, 0, 0, (int)(alpha * 255)));
                g.fillRect(wall.position.x, wall.position.y, UNIT_SIZE, UNIT_SIZE);
                g.setColor(Color.RED);
                g.drawRect(wall.position.x, wall.position.y, UNIT_SIZE, UNIT_SIZE);
            }
            
            if (pillarFlash) {
                pillarFlash = false;
                flashPillar = null;
            }

            boolean shouldFlash = isInvincible && (System.currentTimeMillis() % 200 < 100);
            
            for (int i = 0; i < snake.size(); i++) {
                Point p = snake.get(i);
                
                if (useSkin && headSkin != null && bodySkin != null) {
                    if (i == 0) {
                        if (isInvincible && shouldFlash) {
                            g.setColor(Color.WHITE);
                            g.fillRect(p.x, p.y, UNIT_SIZE, UNIT_SIZE);
                        }
                        g.drawImage(headSkin, p.x, p.y, this);
                    } else {
                        if (isInvincible && shouldFlash) {
                            g.setColor(Color.WHITE);
                            g.fillRect(p.x, p.y, UNIT_SIZE, UNIT_SIZE);
                        }
                        g.drawImage(bodySkin, p.x, p.y, this);
                    }
                } else {
                    if (i == 0) {
                        if (isInvincible && shouldFlash) {
                            g.setColor(Color.WHITE);
                        } else {
                            g.setColor(isInvincible ? Color.CYAN : new Color(0, 150, 0));
                        }
                    } else {
                        if (isInvincible && shouldFlash) {
                            g.setColor(new Color(200, 200, 255));
                        } else {
                            g.setColor(isInvincible ? new Color(100, 200, 255) : new Color(0, 120, 0));
                        }
                    }
                    g.fillRect(p.x, p.y, UNIT_SIZE, UNIT_SIZE);
                }
            }

            if (isFast) {
                g.setColor(Color.YELLOW);
                g.fillRect(0, HEIGHT - 5, WIDTH, 5);
            } else if (isInvincible) {
                g.setColor(Color.CYAN);
                g.fillRect(0, HEIGHT - 3, WIDTH, 3);
            }

            drawScore(g);
        } else {
            gameOver(g);
        }
    }

    private void drawScore(Graphics g) {
        long elapsedTime = (System.currentTimeMillis() - startTime - accumulatedPauseTime) / 1000;
        int timePenalty = 0;
        if (elapsedTime * 1000 > SAFE_TIME) {
            timePenalty = (int)(elapsedTime - SAFE_TIME / 1000) * 5;
        }
        int currentScore = score - timePenalty;
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

        if (!goldenFoods.isEmpty()) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g.drawString("⭐⭐ 金色豆子 x" + goldenFoods.size() + "!", WIDTH - 180, 55);
        }
        
        if (comboCount > 0) {
            long elapsedSinceCombo = System.currentTimeMillis() - lastComboTime;
            float progress = 1.0f - (float)elapsedSinceCombo / COMBO_TIMEOUT;
            if (progress < 0) progress = 0;
            
            int barWidth = 150;
            int barHeight = 8;
            int barX = 15;
            int barY = 100;
            
            g.setColor(new Color(100, 100, 100));
            g.fillRect(barX, barY, barWidth, barHeight);
            
            Color comboColor = isFoodCombo ? Color.RED : Color.BLUE;
            g.setColor(comboColor);
            g.fillRect(barX, barY, (int)(barWidth * progress), barHeight);
            
            g.setColor(Color.WHITE);
            g.drawRect(barX, barY, barWidth, barHeight);
            
            g.setFont(new Font("微软雅黑", Font.BOLD, 18));
            String comboType = isFoodCombo ? "吃豆" : "碰撞";
            g.drawString(comboType + "连击 x" + comboCount, 15, 125);
        }
        
        if (hasRespawnMarker) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("微软雅黑", Font.BOLD, 22));
            g.drawString("🔄 重生", 15, 150);
        }
        
        if (speedBuffActive) {
            long remainingTime = (speedBuffEndTime - System.currentTimeMillis()) / 1000;
            g.setColor(new Color(255, 165, 0));
            g.setFont(new Font("微软雅黑", Font.BOLD, 20));
            g.drawString("⚡ 加速 x3! " + remainingTime + "s", WIDTH - 180, 105);
        }
        
        if (negativeBuffActive) {
            long remainingTime = (negativeBuffEndTime - System.currentTimeMillis()) / 1000;
            int difficultyLevel = Math.min(negativeBuffEventCount + 1, 3);
            g.setColor(Color.RED);
            g.setFont(new Font("微软雅黑", Font.BOLD, 20));
            g.drawString("⚠️ 危险 Lv" + difficultyLevel + "! " + remainingTime + "s", WIDTH - 220, 130);
        }
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("微软雅黑", Font.BOLD, 14));
        g.drawString("道具栏:", 15, HEIGHT - 30);
        
        for (int i = 0; i < inventory.length; i++) {
            int x = 75 + i * 50;
            int y = HEIGHT - 35;
            
            if (selectedItem == i) {
                g.setColor(Color.YELLOW);
                g.drawRect(x - 2, y - 2, 34, 24);
            }
            
            if (inventory[i] != null) {
                g.setColor(new Color(100, 100, 100));
                g.fillRect(x, y, 30, 20);
                
                g.setColor(Color.WHITE);
                g.drawRect(x, y, 30, 20);
                
                int beanX = x + 5;
                int beanY = y + 2;
                int beanSize = 18;
                
                if ("clear".equals(inventory[i])) {
                    g.setColor(new Color(160, 32, 240));
                } else {
                    g.setColor(Color.CYAN);
                }
                g.fillOval(beanX, beanY, beanSize, beanSize);
                
                g.setColor(Color.WHITE);
                g.drawOval(beanX, beanY, beanSize, beanSize);
            }
        }
        
        g.setColor(Color.GRAY);
        g.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        g.drawString("1/2/3 选择 | E 使用", 15, HEIGHT - 10);
        
        if (purpleFoodActive) {
            g.setColor(new Color(160, 32, 240));
            g.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g.drawString("💜 紫色豆子!", WIDTH - 180, 80);
        }
        
        if (cyanFoodActive) {
            g.setColor(new Color(0, 255, 255));
            g.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g.drawString("💎 青色豆子!", WIDTH - 180, 80);
        }
        
        if (negativeBuffActive) {
            long remainingTime = (negativeBuffEndTime - System.currentTimeMillis()) / 1000;
            g.setColor(Color.RED);
            g.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g.drawString("⚠️ 危险! " + remainingTime + "s", WIDTH - 180, 105);
        }

        g.drawString("摧毁: " + pillarDestroyCount, WIDTH - 140, 55);
        
        if (comboCount > 1) {
            g.setColor(Color.ORANGE);
            g.drawString("连击 x" + comboCount, 15, 105);
        }
        
        g.drawString("最高分: " + highScore, WIDTH - 140, 80);
        
        if (!"default".equals(currentSkin)) {
            g.setColor(Color.MAGENTA);
            g.drawString("皮肤: " + currentSkin, 15, 130);
        }
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
        for (RedWall wall : redWalls) {
            if (food.equals(wall.position)) {
                newFood();
                return;
            }
        }
        for (Point goldenFood : goldenFoods) {
            if (food.equals(goldenFood)) {
                newFood();
                return;
            }
        }
        if (purpleFood != null && food.equals(purpleFood)) {
            newFood();
            return;
        }
        if (cyanFood != null && food.equals(cyanFood)) {
            newFood();
            return;
        }
    }

    private void spawnGoldenFood() {
        if (!running) return;
        
        int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
        int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
        Point goldenFood = new Point(x, y);

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
        for (RedWall wall : redWalls) {
            if (goldenFood.equals(wall.position)) {
                spawnGoldenFood();
                return;
            }
        }
        for (Point existingGolden : goldenFoods) {
            if (goldenFood.equals(existingGolden)) {
                spawnGoldenFood();
                return;
            }
        }
        if (goldenFood.equals(food)) {
            spawnGoldenFood();
            return;
        }
        if (purpleFood != null && goldenFood.equals(purpleFood)) {
            spawnGoldenFood();
            return;
        }
        if (cyanFood != null && goldenFood.equals(cyanFood)) {
            spawnGoldenFood();
            return;
        }

        goldenFoods.add(goldenFood);
    }
    
    private void spawnPurpleFood() {
        if (!purpleFoodActive && running) {
            int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
            int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
            purpleFood = new Point(x, y);

            for (Point p : snake) {
                if (purpleFood.equals(p)) {
                    spawnPurpleFood();
                    return;
                }
            }
            for (Point p : pillars) {
                if (purpleFood.equals(p)) {
                    spawnPurpleFood();
                    return;
                }
            }
            for (RedWall wall : redWalls) {
                if (purpleFood.equals(wall.position)) {
                    spawnPurpleFood();
                    return;
                }
            }
            if (purpleFood.equals(food)) {
                spawnPurpleFood();
                return;
            }
            for (Point goldenFood : goldenFoods) {
                if (purpleFood.equals(goldenFood)) {
                    spawnPurpleFood();
                    return;
                }
            }
            if (cyanFood != null && purpleFood.equals(cyanFood)) {
                spawnPurpleFood();
                return;
            }

            purpleFoodActive = true;
        }
    }
    
    private void spawnCyanFood() {
        if (!cyanFoodActive && running) {
            int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
            int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
            cyanFood = new Point(x, y);

            for (Point p : snake) {
                if (cyanFood.equals(p)) {
                    spawnCyanFood();
                    return;
                }
            }
            for (Point p : pillars) {
                if (cyanFood.equals(p)) {
                    spawnCyanFood();
                    return;
                }
            }
            for (RedWall wall : redWalls) {
                if (cyanFood.equals(wall.position)) {
                    spawnCyanFood();
                    return;
                }
            }
            if (cyanFood.equals(food)) {
                spawnCyanFood();
                return;
            }
            for (Point goldenFood : goldenFoods) {
                if (cyanFood.equals(goldenFood)) {
                    spawnCyanFood();
                    return;
                }
            }
            if (purpleFood != null && cyanFood.equals(purpleFood)) {
                spawnCyanFood();
                return;
            }

            cyanFoodActive = true;
        }
    }
    
    private boolean isValidWallPosition(Point pos) {
        for (Point p : snake) {
            if (pos.equals(p)) return false;
        }
        for (Point p : pillars) {
            if (pos.equals(p)) return false;
        }
        for (RedWall wall : redWalls) {
            if (pos.equals(wall.position)) return false;
        }
        if (pos.equals(food)) return false;
        for (Point goldenFood : goldenFoods) {
            if (pos.equals(goldenFood)) return false;
        }
        if (purpleFood != null && pos.equals(purpleFood)) return false;
        if (cyanFood != null && pos.equals(cyanFood)) return false;
        return true;
    }
    
    private void spawnSingleWall(long spawnTime) {
        int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
        int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
        Point pos = new Point(x, y);
        
        if (isValidWallPosition(pos)) {
            redWalls.add(new RedWall(pos, spawnTime));
        }
    }
    
    private void spawnHorizontalWall(int startX, int y, long spawnTime) {
        for (int i = 0; i < 6; i++) {
            Point pos = new Point(startX + i * UNIT_SIZE, y);
            if (!isValidWallPosition(pos)) return;
        }
        for (int i = 0; i < 6; i++) {
            Point pos = new Point(startX + i * UNIT_SIZE, y);
            redWalls.add(new RedWall(pos, spawnTime));
        }
    }
    
    private void spawnVerticalWall(int x, int startY, long spawnTime) {
        for (int i = 0; i < 6; i++) {
            Point pos = new Point(x, startY + i * UNIT_SIZE);
            if (!isValidWallPosition(pos)) return;
        }
        for (int i = 0; i < 6; i++) {
            Point pos = new Point(x, startY + i * UNIT_SIZE);
            redWalls.add(new RedWall(pos, spawnTime));
        }
    }
    
    private void spawn6CellWall(long spawnTime) {
        boolean horizontal = random.nextBoolean();
        
        if (horizontal) {
            int maxStartX = WIDTH / UNIT_SIZE - 6;
            int startX = random.nextInt(maxStartX) * UNIT_SIZE;
            int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
            spawnHorizontalWall(startX, y, spawnTime);
        } else {
            int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
            int maxStartY = HEIGHT / UNIT_SIZE - 6;
            int startY = random.nextInt(maxStartY) * UNIT_SIZE;
            spawnVerticalWall(x, startY, spawnTime);
        }
        
        if (redWalls.isEmpty() || redWalls.get(redWalls.size() - 1).spawnTime != spawnTime) {
            spawn6CellWall(spawnTime);
        }
    }
    
    private void spawnTwoWalls(long spawnTime) {
        boolean firstHorizontal = random.nextBoolean();
        
        if (firstHorizontal) {
            int maxStartX = WIDTH / UNIT_SIZE - 6;
            int startX = random.nextInt(maxStartX) * UNIT_SIZE;
            int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
            spawnHorizontalWall(startX, y, spawnTime);
        } else {
            int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
            int maxStartY = HEIGHT / UNIT_SIZE - 6;
            int startY = random.nextInt(maxStartY) * UNIT_SIZE;
            spawnVerticalWall(x, startY, spawnTime);
        }
        
        if (redWalls.isEmpty() || redWalls.get(redWalls.size() - 1).spawnTime != spawnTime) {
            spawnTwoWalls(spawnTime);
            return;
        }
        
        boolean secondHorizontal = !firstHorizontal;
        
        if (secondHorizontal) {
            int maxStartX = WIDTH / UNIT_SIZE - 6;
            int startX = random.nextInt(maxStartX) * UNIT_SIZE;
            int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
            spawnHorizontalWall(startX, y, spawnTime);
        } else {
            int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
            int maxStartY = HEIGHT / UNIT_SIZE - 6;
            int startY = random.nextInt(maxStartY) * UNIT_SIZE;
            spawnVerticalWall(x, startY, spawnTime);
        }
        
        if (redWalls.size() < 12) {
            spawnTwoWalls(spawnTime);
        }
    }
    
    private void spawnThreeBlockWalls(long spawnTime) {
        for (int block = 0; block < 3; block++) {
            int startX = random.nextInt(WIDTH / UNIT_SIZE - 2) * UNIT_SIZE;
            int startY = random.nextInt(HEIGHT / UNIT_SIZE - 2) * UNIT_SIZE;
            
            boolean valid = true;
            for (int i = 0; i < 3 && valid; i++) {
                for (int j = 0; j < 3 && valid; j++) {
                    Point pos = new Point(startX + i * UNIT_SIZE, startY + j * UNIT_SIZE);
                    if (!isValidWallPosition(pos)) {
                        valid = false;
                    }
                }
            }
            
            if (valid) {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        Point pos = new Point(startX + i * UNIT_SIZE, startY + j * UNIT_SIZE);
                        redWalls.add(new RedWall(pos, spawnTime));
                    }
                }
            } else {
                block--;
            }
        }
    }
    
    private void updateRedWalls() {
        long currentTime = System.currentTimeMillis();
        
        for (int i = redWalls.size() - 1; i >= 0; i--) {
            RedWall wall = redWalls.get(i);
            if (currentTime - wall.spawnTime >= RED_WALL_LIFETIME) {
                redWalls.remove(i);
            }
        }
    }
    
    private void checkNegativeBuff(int currentScore) {
        if (!negativeBuffUnlocked && currentScore >= NEGATIVE_BUFF_START_SCORE) {
            negativeBuffUnlocked = true;
            startNegativeBuffEvent();
            return;
        }
        
        if (negativeBuffUnlocked && !negativeBuffActive) {
            if (lastNegativeBuffEndTime == 0 || 
                System.currentTimeMillis() - lastNegativeBuffEndTime >= NEGATIVE_BUFF_INTERVAL) {
                startNegativeBuffEvent();
            }
        }
        
        if (negativeBuffActive && System.currentTimeMillis() >= negativeBuffEndTime) {
            negativeBuffActive = false;
            negativeBuffEventCount++;
            lastNegativeBuffEndTime = System.currentTimeMillis();
            redWalls.clear();
        }
    }
    
    private void startNegativeBuffEvent() {
        negativeBuffActive = true;
        negativeBuffEndTime = System.currentTimeMillis() + NEGATIVE_BUFF_DURATION;
        negativeBuffStartTime = System.currentTimeMillis();
        lastRedWallSpawnTime = System.currentTimeMillis();
    }
    
    private void checkSpeedBuff() {
        if (!speedBuffTriggered && foodCount >= SPEED_BUFF_FOOD_MIN && foodCount <= SPEED_BUFF_FOOD_MAX) {
            if (random.nextDouble() < 0.1) {
                speedBuffActive = true;
                speedBuffEndTime = System.currentTimeMillis() + SPEED_BUFF_DURATION;
                speedBuffTriggered = true;
                updateSpeed();
            }
        }
        
        if (speedBuffActive && System.currentTimeMillis() >= speedBuffEndTime) {
            speedBuffActive = false;
            updateSpeed();
        }
    }
    
    private void addToInventory(String item) {
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null) {
                inventory[i] = item;
                break;
            }
        }
    }
    
    private void useItem() {
        if (selectedItem >= 0 && selectedItem < inventory.length && inventory[selectedItem] != null) {
            String item = inventory[selectedItem];
            if ("clear".equals(item)) {
                pillars.clear();
                redWalls.clear();
                inventory[selectedItem] = null;
            } else if ("respawn".equals(item)) {
                respawnSnake();
                inventory[selectedItem] = null;
            }
            selectedItem = -1;
        }
    }
    
    private void spawnWallsByPhase(long currentTime) {
        if (!negativeBuffActive) return;
        
        long elapsed = currentTime - negativeBuffStartTime;
        long spawnInterval = RED_WALL_SPAWN_INTERVAL;
        
        int difficultyLevel = Math.min(negativeBuffEventCount + 1, 3);
        
        if (difficultyLevel >= 2) {
            spawnInterval = RED_WALL_SPAWN_INTERVAL * 4 / 5;
        }
        if (difficultyLevel >= 3) {
            spawnInterval = RED_WALL_SPAWN_INTERVAL * 3 / 5;
        }
        
        if (currentTime - lastRedWallSpawnTime < spawnInterval) return;
        
        redWalls.clear();
        
        if (difficultyLevel == 1) {
            if (elapsed < RED_WALL_SECTION_DURATION) {
                spawn6CellWall(currentTime);
            } else if (elapsed < RED_WALL_SECTION_DURATION * 2) {
                spawnTwoWalls(currentTime);
            } else {
                spawnThreeBlockWalls(currentTime);
            }
        } else if (difficultyLevel == 2) {
            if (elapsed < RED_WALL_SECTION_DURATION) {
                spawnTwoWalls(currentTime);
            } else if (elapsed < RED_WALL_SECTION_DURATION * 2) {
                spawnThreeBlockWalls(currentTime);
                spawn6CellWall(currentTime);
            } else {
                spawnThreeBlockWalls(currentTime);
                spawnTwoWalls(currentTime);
            }
        } else if (difficultyLevel >= 3) {
            if (elapsed < RED_WALL_SECTION_DURATION) {
                spawnThreeBlockWalls(currentTime);
            } else if (elapsed < RED_WALL_SECTION_DURATION * 2) {
                spawnThreeBlockWalls(currentTime);
                spawnThreeBlockWalls(currentTime);
            } else {
                spawnThreeBlockWalls(currentTime);
                spawnThreeBlockWalls(currentTime);
                spawn6CellWall(currentTime);
            }
        }
        
        lastRedWallSpawnTime = currentTime;
    }
    
    private void updateSpeed() {
        int targetDelay;
        
        if (speedBuffActive) {
            targetDelay = baseDelay / 3;
            if (targetDelay < FAST_DELAY) targetDelay = FAST_DELAY;
        } else if (isFast) {
            targetDelay = FAST_DELAY;
        } else if (isSlow) {
            targetDelay = SLOW_DELAY;
        } else if (isInvincible) {
            targetDelay = INVINCIBLE_DELAY;
        } else {
            targetDelay = baseDelay;
        }
        
        if (targetDelay != currentDelay) {
            currentDelay = targetDelay;
            timer.setDelay(currentDelay);
        }
    }

    private void move() {
        long currentTime = System.currentTimeMillis();

        int goldenInterval = Math.max(MIN_GOLDEN_FOOD_INTERVAL, BASE_GOLDEN_FOOD_INTERVAL - foodCount * 500);
        if (currentTime - lastGoldenFoodTime >= goldenInterval) {
            spawnGoldenFood();
            lastGoldenFoodTime = currentTime;
        }
        
        if (currentTime - lastPurpleFoodTime >= PURPLE_FOOD_INTERVAL) {
            spawnPurpleFood();
            lastPurpleFoodTime = currentTime;
        }
        
        if (currentTime - lastCyanFoodTime >= CYAN_FOOD_INTERVAL) {
            spawnCyanFood();
            lastCyanFoodTime = currentTime;
        }

        if (isInvincible && currentTime >= invincibleEndTime) {
            isInvincible = false;
            updateSpeed();
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
            isFoodCombo = true;
            
            int foodBonus = comboCount * 20;
            score += foodBonus;
            
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
            if (newDelay != baseDelay) {
                baseDelay = newDelay;
                if (!isFast && !isSlow && !isInvincible) {
                    currentDelay = baseDelay;
                    timer.setDelay(currentDelay);
                }
            }
            
            newFood();
        } else {
            boolean ateGolden = false;
            for (int i = 0; i < goldenFoods.size(); i++) {
                Point goldenFood = goldenFoods.get(i);
                if (newHead.equals(goldenFood)) {
                    isInvincible = true;
                    invincibleEndTime = currentTime + INVINCIBLE_TIME;
                    goldenFoods.remove(i);
                    updateSpeed();
                    ateGolden = true;
                    break;
                }
            }
            if (ateGolden) {
                snake.remove(snake.size() - 1);
            } else if (purpleFoodActive && newHead.equals(purpleFood)) {
                addToInventory("clear");
                purpleFoodActive = false;
                purpleFood = null;
            } else if (cyanFoodActive && newHead.equals(cyanFood)) {
                hasRespawnMarker = true;
                cyanFoodActive = false;
                cyanFood = null;
            } else {
                snake.remove(snake.size() - 1);
            }
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
                    
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastComboTime < COMBO_TIMEOUT) {
                        comboCount++;
                    } else {
                        comboCount = 1;
                    }
                    lastComboTime = currentTime;
                    isFoodCombo = false;
                    
                    int pillarBonus = comboCount * 100;
                    score += pillarBonus;
                } else {
                    if (hasRespawnMarker) {
                        respawnAtOpenArea();
                    } else {
                        running = false;
                    }
                }
                break;
            }
        }

        for (int i = 0; i < redWalls.size(); i++) {
            RedWall wall = redWalls.get(i);
            if (head.equals(wall.position)) {
                if (isInvincible) {
                    redWalls.clear();
                    isInvincible = false;
                    invincibleEndTime = 0;
                    updateSpeed();
                } else {
                    if (hasRespawnMarker) {
                        respawnAtOpenArea();
                    } else {
                        running = false;
                    }
                }
                break;
            }
        }

        if (!running) {
            timer.stop();
            long elapsedTime = (System.currentTimeMillis() - startTime - accumulatedPauseTime) / 1000;
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
        
        long elapsedTime = (System.currentTimeMillis() - startTime - accumulatedPauseTime) / 1000;
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
            long elapsedTime = (System.currentTimeMillis() - startTime - accumulatedPauseTime) / 1000;
            int timePenalty = 0;
            if (elapsedTime * 1000 > SAFE_TIME) {
                timePenalty = (int)(elapsedTime - SAFE_TIME / 1000) * 5;
            }
            int currentScore = score - timePenalty;
            if (currentScore < 0) currentScore = 0;
            
            checkNegativeBuff(currentScore);
            checkSpeedBuff();
            
            if (negativeBuffActive) {
                updateRedWalls();
                spawnWallsByPhase(System.currentTimeMillis());
            }
            
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
            
            if (key == KeyEvent.VK_F5) {
                if (hasRespawnMarker) {
                    respawnSnake();
                    hasRespawnMarker = false;
                }
                return;
            }
            
            if (key == KeyEvent.VK_F6) {
                pillars.clear();
                redWalls.clear();
                return;
            }
            
            if (key == KeyEvent.VK_1) {
                selectedItem = 0;
                return;
            }
            if (key == KeyEvent.VK_2) {
                selectedItem = 1;
                return;
            }
            if (key == KeyEvent.VK_3) {
                selectedItem = 2;
                return;
            }
            if (key == KeyEvent.VK_E) {
                useItem();
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
                updateSpeed();
            } else if (key == KeyEvent.VK_SPACE) {
                isFast = true;
                isSlow = false;
                updateSpeed();
            }
        }

        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();

            if (!running || paused) return;

            if (key == KeyEvent.VK_SHIFT) {
                isSlow = false;
                updateSpeed();
            } else if (key == KeyEvent.VK_SPACE) {
                isFast = false;
                updateSpeed();
            }
        }
    }
    
    class RedWall {
        Point position;
        long spawnTime;
        
        RedWall(Point position, long spawnTime) {
            this.position = position;
            this.spawnTime = spawnTime;
        }
    }
}