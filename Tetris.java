
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;

public class Tetris extends JPanel implements ActionListener {
    private final int ROWS = 20;
    private int COLS;
    private int BLOCK;
    private int level = 1;
    private int score = 0;
    private int highScore = 0;
    private final Font font = new Font("Arial", Font.BOLD, 18);

    private Timer timer;
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
    private final Color[] colors = { Color.cyan, Color.yellow, Color.magenta, Color.blue, Color.orange, Color.green, Color.red };

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
                }
                repaint();
            }
        });

        timer = new Timer(delay, this);
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
            JOptionPane.showMessageDialog(this, "Game Over!\nScore: " + score + "\nHigh Score: " + Math.max(score, highScore));
            System.exit(0);
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
        int lines = 0;
        for (int i = ROWS - 1; i >= 0; i--) {
            boolean full = true;
            for (int j = 0; j < COLS; j++)
                if (board[i][j] == 0) full = false;
            if (full) {
                lines++;
                for (int k = i; k > 0; k--) board[k] = board[k - 1].clone();
                board[0] = new int[COLS];
                i++;
            }
        }
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

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.darkGray);
        for (int x = 0; x <= COLS; x++)
            g.drawLine(x * BLOCK, 0, x * BLOCK, ROWS * BLOCK);
        for (int y = 0; y <= ROWS; y++)
            g.drawLine(0, y * BLOCK, COLS * BLOCK, y * BLOCK);

        for (int y = 0; y < ROWS; y++)
            for (int x = 0; x < COLS; x++)
                if (board[y][x] > 0) {
                    g.setColor(colors[board[y][x] - 1]);
                    g.fillRect(x * BLOCK, y * BLOCK, BLOCK, BLOCK);
                    g.setColor(Color.black);
                    g.drawRect(x * BLOCK, y * BLOCK, BLOCK, BLOCK);
                }

        g.setColor(colors[shapeType]);
        for (int i = 0; i < shape.length; i++)
            for (int j = 0; j < shape[0].length; j++)
                if (shape[i][j] == 1) {
                    int px = (shapeX + j) * BLOCK;
                    int py = (shapeY + i) * BLOCK;
                    g.fillRect(px, py, BLOCK, BLOCK);
                    g.setColor(Color.black);
                    g.drawRect(px, py, BLOCK, BLOCK);
                    g.setColor(colors[shapeType]);
                }

        g.setColor(Color.white);
        g.setFont(font);
        g.drawString("Score: " + score, COLS * BLOCK + 10, 30);
        g.drawString("High Score:  " + Math.max(score, highScore), COLS * BLOCK + 10, 60);
        g.drawString("Level: " + level, COLS * BLOCK + 10, 90);

        g.drawString("Next:", COLS * BLOCK + 10, 130);
        for (int i = 0; i < nextShape.length; i++)
            for (int j = 0; j < nextShape[0].length; j++)
                if (nextShape[i][j] == 1) {
                    int px = COLS * BLOCK + 10 + j * BLOCK / 2;
                    int py = 140 + i * BLOCK / 2;
                    g.setColor(colors[nextType]);
                    g.fillRect(px, py, BLOCK / 2, BLOCK / 2);
                    g.setColor(Color.black);
                    g.drawRect(px, py, BLOCK / 2, BLOCK / 2);
                }

        g.drawString("Hold:", COLS * BLOCK + 10, 210);
        if (holdType != -1) {
            int[][] hs = shapes[holdType];
            for (int i = 0; i < hs.length; i++)
                for (int j = 0; j < hs[0].length; j++)
                    if (hs[i][j] == 1) {
                        int px = COLS * BLOCK + 10 + j * BLOCK / 2;
                        int py = 220 + i * BLOCK / 2;
                        g.setColor(colors[holdType]);
                        g.fillRect(px, py, BLOCK / 2, BLOCK / 2);
                        g.setColor(Color.black);
                        g.drawRect(px, py, BLOCK / 2, BLOCK / 2);
                    }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame settingsFrame = new JFrame("Tetris Settings");
            settingsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            settingsFrame.setSize(300, 200);
            settingsFrame.setLayout(new GridLayout(4, 1));
    
            // Width selection
            JPanel widthPanel = new JPanel();
            widthPanel.add(new JLabel("Select Board Width:"));
            String[] widths = { "10", "12", "15" };
            JComboBox<String> widthBox = new JComboBox<>(widths);
            widthPanel.add(widthBox);
    
            // Speed (difficulty) selection
            JPanel speedPanel = new JPanel();
            speedPanel.add(new JLabel("Select Difficulty:"));
            String[] speeds = { "Easy (700)", "Medium (500)", "Hard (300)" };
            JComboBox<String> speedBox = new JComboBox<>(speeds);
            speedPanel.add(speedBox);
    
            // Start button
            JButton startButton = new JButton("Start Game");
            startButton.addActionListener(e -> {
                int cols = Integer.parseInt((String) widthBox.getSelectedItem());
                int speed;
                int multiplier;
    
                switch (speedBox.getSelectedIndex()) {
                    case 0 -> { speed = 700; multiplier = 1; }  // Easy
                    case 1 -> { speed = 500; multiplier = 2; }  // Medium
                    case 2 -> { speed = 300; multiplier = 3; }  // Hard
                    default -> { speed = 500; multiplier = 2; }
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
            settingsFrame.add(new JLabel()); // Spacer
            settingsFrame.add(startButton);
            settingsFrame.setLocationRelativeTo(null);
            settingsFrame.setVisible(true);
        });
    }
    
    }
    
    

