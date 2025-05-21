package TetrisGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class Tetris extends JPanel implements ActionListener {
    private final java.util.List<Integer> clearingLines = new ArrayList<>();
    private float fadeProgress = 1.0f;
    private Timer timer;
    private Timer animationTimer;

    private final int ROWS = 20;
    private int COLS;
    private int BLOCK;
    private int level = 1;
    private int score = 0;
    private int highScore = 0;
    private final Font font = new Font("SansSerif", Font.BOLD, 18);

    private int coins = 0;


    private int delay;
    private int scoreMultiplier = 1;
    private int[][] board;
    private final int[][][] shapes = {
            {{1, 1, 1, 1}},
            {{1, 1}, {1, 1}},
            {{0, 1, 0}, {1, 1, 1}},
            {{1, 0, 0}, {1, 1, 1}},
            {{0, 0, 1}, {1, 1, 1}},
            {{1, 1, 0}, {0, 1, 1}},
            {{0, 1, 1}, {1, 1, 0}}
    };
    private final Color[] colors = {Color.cyan, Color.yellow, Color.magenta, Color.blue, Color.orange, Color.green, Color.red};

    private int[][] shape, nextShape;
    private int shapeX, shapeY, shapeType, nextType, holdType = -1;
    private boolean canHold = true;

    public Tetris(int cols, int blockSize, int delay, int scoreMultiplier) {
        this.COLS = cols;
        this.BLOCK = blockSize;
        this.delay = delay;
        this.scoreMultiplier = scoreMultiplier;

        board = new int[ROWS][COLS];
        setPreferredSize(new Dimension(COLS * BLOCK + 150, ROWS * BLOCK));
        setBackground(Color.black);
        setFocusable(true);
        loadHighScore();

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> move(-1);
                    case KeyEvent.VK_RIGHT -> move(1);
                    case KeyEvent.VK_DOWN -> drop();
                    case KeyEvent.VK_UP -> rotate();
                    case KeyEvent.VK_SPACE -> hardDrop();
                    case KeyEvent.VK_C -> hold();
                    case KeyEvent.VK_P -> pause();
                    case KeyEvent.VK_S -> showShop(); // Press 'S' to open shop

                }
                repaint();
            }
        });

        timer = new Timer(delay, this);

        animationTimer = new Timer(30, e -> {
            fadeProgress -= 0.1f;
            if (fadeProgress <= 0) {
                clearingLines.clear();
                fadeProgress = 1.0f;
                clearLines(true);
                timer.start();
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        nextType = new Random().nextInt(shapes.length);
        nextShape = shapes[nextType];
        spawn();
        timer.start();
    }

    void loadHighScore() {
        try {
            File f = new File("highscore.txt");
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                highScore = Integer.parseInt(br.readLine());
                br.close();
            }
        } catch (Exception ignored) {}
    }

    void saveHighScore() {
        try {
            PrintWriter pw = new PrintWriter("highscore.txt");
            pw.println(Math.max(score, highScore));
            pw.close();
        } catch (Exception ignored) {}
    }

    void spawn() {
        shapeType = nextType;
        shape = nextShape;
        shapeX = COLS / 2 - shape[0].length / 2;
        shapeY = 0;
        nextType = new Random().nextInt(shapes.length);
        nextShape = shapes[nextType];
        canHold = true;

        if (!canMove(shape, shapeX, shapeY)) {
            timer.stop();
            saveHighScore();
            int option = JOptionPane.showConfirmDialog(this, "Game Over!\nScore: " + score + "\nHigh Score: " + Math.max(score, highScore) + "\nRestart?", "Game Over", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                score = 0;
                board = new int[ROWS][COLS];
                spawn();
                timer.start();
            } else {
                System.exit(0);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (!canMove(shape, shapeX, shapeY + 1)) {
            merge();
            clearLines();
            spawn();
        } else shapeY++;
        repaint();
    }

    void move(int dx) {
        if (canMove(shape, shapeX + dx, shapeY)) shapeX += dx;
    }

    void drop() {
        if (canMove(shape, shapeX, shapeY + 1)) shapeY++;
    }

    void hardDrop() {
        while (canMove(shape, shapeX, shapeY + 1)) shapeY++;
        merge();
        clearLines();
        spawn();
    }

    void rotate() {
        int[][] rotated = new int[shape[0].length][shape.length];
        for (int y = 0; y < shape.length; y++)
            for (int x = 0; x < shape[0].length; x++)
                rotated[x][shape.length - 1 - y] = shape[y][x];
        if (canMove(rotated, shapeX, shapeY)) shape = rotated;
    }

    void hold() {
        if (!canHold) return;
        canHold = false;
        if (holdType == -1) {
            holdType = shapeType;
            spawn();
        } else {
            int temp = shapeType;
            shapeType = holdType;
            holdType = temp;
            shape = shapes[shapeType];
            shapeX = COLS / 2 - shape[0].length / 2;
            shapeY = 0;
        }
    }

    boolean canMove(int[][] s, int x, int y) {
        for (int i = 0; i < s.length; i++)
            for (int j = 0; j < s[0].length; j++)
                if (s[i][j] == 1) {
                    int nx = x + j, ny = y + i;
                    if (nx < 0 || nx >= COLS || ny >= ROWS || (ny >= 0 && board[ny][nx] > 0))
                        return false;
                }
        return true;
    }

    void merge() {
        for (int i = 0; i < shape.length; i++)
            for (int j = 0; j < shape[0].length; j++)
                if (shape[i][j] == 1 && shapeY + i >= 0)
                    board[shapeY + i][shapeX + j] = shapeType + 1;
    }

    void clearLines() {
        clearLines(false);
    }

    void clearLines(boolean skipAnimation) {
        int lines = 0;
        coins += lines * 10; // 10 coins per line
        for (int i = ROWS - 1; i >= 0; i--) {
            boolean full = true;
            for (int j = 0; j < COLS; j++) {
                if (board[i][j] == 0) full = false;
            }
            if (full) {
                if (!skipAnimation) {
                    clearingLines.add(i);
                } else {
                    lines++;
                    for (int k = i; k > 0; k--) board[k] = board[k - 1].clone();
                    board[0] = new int[COLS];
                    i++;
                }
            }
        }
       if (!skipAnimation && !clearingLines.isEmpty()) {
         timer.stop();
         animationTimer.start();
        }
        coins += lines * 10;  // <-- move here!
        score += switch (lines) {

            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 500;
            case 4 -> 800;
            default -> 0;
        } * level * scoreMultiplier;
    }

    void pause() {
        if (timer.isRunning()) timer.stop();
        else timer.start();
    }

    private void drawBlock(Graphics2D g, int x, int y, Color color) {
        GradientPaint gradient = new GradientPaint(x, y, color.brighter(), x + BLOCK, y + BLOCK, color.darker());
        g.setPaint(gradient);
        g.fillRoundRect(x, y, BLOCK, BLOCK, 6, 6);
        g.setColor(Color.black);
        g.drawRoundRect(x, y, BLOCK, BLOCK, 6, 6);
    }

    private void drawShadow(Graphics2D g, int x, int y) {
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRoundRect(x + 3, y + 3, BLOCK, BLOCK, 6, 6);
    }

    void showShop() {
    String[] options = {
        "1. Slow Down (50 coins)", 
        "2. Clear Bottom Row (100 coins)", 
        "3. Change Block Color (75 coins)"
    };

    String choice = (String) JOptionPane.showInputDialog(
            this, 
            "Select an item to buy:", 
            "Tetris Shop", 
            JOptionPane.PLAIN_MESSAGE, 
            null, 
            options, 
            options[0]
    );

    if (choice == null) return;

    switch (choice) {
        case "1. Slow Down (50 coins)" -> {
            if (coins >= 50) {
                coins -= 50;
                timer.setDelay(delay + 200); // temporary slow
                new Timer(5000, e -> timer.setDelay(delay)).start(); // restore after 5s
            }
        }
        case "2. Clear Bottom Row (100 coins)" -> {
            if (coins >= 100) {
                coins -= 100;
                System.arraycopy(board, 0, board, 1, ROWS - 1);
                board[0] = new int[COLS];
            }
        }
        case "3. Change Block Color (75 coins)" -> {
            if (coins >= 75) {
                coins -= 75;
                Random rand = new Random();
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
                }
            }
        }
    }

    repaint();
}
    

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
       

        g2d.setColor(Color.darkGray);
        for (int x = 0; x <= COLS; x++)
            g2d.drawLine(x * BLOCK, 0, x * BLOCK, ROWS * BLOCK);
        for (int y = 0; y <= ROWS; y++)
            g2d.drawLine(0, y * BLOCK, COLS * BLOCK, y * BLOCK);

        for (int y = 0; y < ROWS; y++)
            for (int x = 0; x < COLS; x++)
                if (board[y][x] > 0) {
                    int bx = x * BLOCK;
                    int by = y * BLOCK;
                    float alpha = 1.0f;
                    if (clearingLines.contains(y)) alpha = fadeProgress;
                    Color blockColor = colors[board[y][x] - 1];
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    drawShadow(g2d, bx, by);
                    drawBlock(g2d, bx, by, blockColor);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                }

        for (int i = 0; i < shape.length; i++)
            for (int j = 0; j < shape[0].length; j++)
                if (shape[i][j] == 1) {
                    int px = (shapeX + j) * BLOCK;
                    int py = (shapeY + i) * BLOCK;
                    drawShadow(g2d, px, py);
                    drawBlock(g2d, px, py, colors[shapeType]);
                }

        g2d.setColor(Color.white);
        g2d.setFont(font);
        int textX = COLS * BLOCK + 10;
        g2d.drawString("Score: " + score, textX, 30);
        g2d.drawString("High Score: " + Math.max(score, highScore), textX, 60);
        g2d.drawString("Level: " + level, textX, 90);
        g2d.drawString("Coins: " + coins, textX, 120);
        g2d.drawString("Next:", textX, 130);
        for (int i = 0; i < nextShape.length; i++)
            for (int j = 0; j < nextShape[0].length; j++)
                if (nextShape[i][j] == 1) {
                    int px = textX + j * BLOCK / 2;
                    int py = 140 + i * BLOCK / 2;
                    g2d.setColor(colors[nextType]);
                    g2d.fillRoundRect(px, py, BLOCK / 2, BLOCK / 2, 4, 4);
                    g2d.setColor(Color.black);
                    g2d.drawRoundRect(px, py, BLOCK / 2, BLOCK / 2, 4, 4);
                }

        g2d.drawString("Hold:", textX, 210);
        if (holdType != -1) {
            int[][] hs = shapes[holdType];
            for (int i = 0; i < hs.length; i++)
                for (int j = 0; j < hs[0].length; j++)
                    if (hs[i][j] == 1) {
                        int px = textX + j * BLOCK / 2;
                        int py = 220 + i * BLOCK / 2;
                        g2d.setColor(colors[holdType]);
                        g2d.fillRoundRect(px, py, BLOCK / 2, BLOCK / 2, 4, 4);
                        g2d.setColor(Color.black);
                        g2d.drawRoundRect(px, py, BLOCK / 2, BLOCK / 2, 4, 4);
                    }
        }

        g2d.setColor(Color.white);
        g2d.drawRect(0, 0, COLS * BLOCK, ROWS * BLOCK);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame settingsFrame = new JFrame("Tetris Settings");
            settingsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            settingsFrame.setSize(300, 200);
            settingsFrame.setLayout(new GridLayout(4, 1));

            JPanel widthPanel = new JPanel();
            widthPanel.add(new JLabel("Select Board Width:"));
            String[] widths = {"10", "12", "15"};
            JComboBox<String> widthBox = new JComboBox<>(widths);
            widthPanel.add(widthBox);

            JPanel speedPanel = new JPanel();
            speedPanel.add(new JLabel("Select Difficulty:"));
            String[] speeds = {"Easy", "Medium", "Hard"};
            JComboBox<String> speedBox = new JComboBox<>(speeds);
            speedPanel.add(speedBox);

            JButton startButton = new JButton("Start Game");
            startButton.addActionListener(e -> {
                int cols = Integer.parseInt((String) widthBox.getSelectedItem());
                int speed, multiplier;
                switch (speedBox.getSelectedIndex()) {
                    case 0 -> {
                        speed = 700;
                        multiplier = 1;
                    }
                    case 1 -> {
                        speed = 500;
                        multiplier = 2;
                    }
                    case 2 -> {
                        speed = 300;
                        multiplier = 3;
                    }
                    default -> {
                        speed = 500;
                        multiplier = 2;
                    }
                }

                settingsFrame.dispose();
                JFrame gameFrame = new JFrame("Tetris Pro");
                gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   
                gameFrame.add(new Tetris(cols, 30, speed, multiplier));
                gameFrame.pack();
                gameFrame.setLocationRelativeTo(null);
                gameFrame.setVisible(true);
            });

            settingsFrame.add(widthPanel);
            settingsFrame.add(speedPanel);
            settingsFrame.add(new JLabel());
            settingsFrame.add(startButton);
            settingsFrame.setLocationRelativeTo(null);
            settingsFrame.setVisible(true);
        });
    }
}
