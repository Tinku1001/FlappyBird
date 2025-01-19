import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    // Images
    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    // Bird properties
    int birdX = boardWidth / 8;
    int birdY = boardWidth / 2;
    int birdWidth = 34;
    int birdHeight = 24;
    double birdAngle = 0;

    // Pipe properties
    int pipeWidth = 64;
    int pipeHeight = 512;

    // Game variables
    int velocityX = -4;
    int velocityY = 0;
    int gravity = 1;
    int backgroundX = 0;
    int pipeOpening = boardHeight / 4;
    final int FLAP_STRENGTH = -9;  // Strength of bird's flap
    final int MAX_VELOCITY = 10;  // Max downward velocity

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipeTimer;
    boolean gameOver = false;
    boolean paused = false;
    double score = 0;
    double highScore = 0;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    class Pipe {
        int x;
        int y;
        int width;
        int height;
        Image img;
        boolean passed = false;

        Pipe(Image img, int x, int y, int width, int height) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    Bird bird;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        // Load images
        backgroundImg = new ImageIcon(getClass().getResource("/flappybirdbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("/flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("/toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("/bottompipe.png")).getImage();

        bird = new Bird(birdImg);
        pipes = new ArrayList<>();

        // Timers
        placePipeTimer = new Timer(1500, e -> placePipes());
        placePipeTimer.start();

        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();
    }

    void placePipes() {
        int randomY = random.nextInt(pipeHeight / 2) - pipeHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg, boardWidth, randomY, pipeWidth, pipeHeight);
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg, boardWidth, randomY + pipeHeight + pipeOpening, pipeWidth, pipeHeight);
        pipes.add(bottomPipe);
    }

    void moveBackground() {
        backgroundX += velocityX;
        if (backgroundX <= -boardWidth) {
            backgroundX = 0;
        }
    }

    void move() {
        if (paused || gameOver) return;

        // Bird movement
        velocityY += gravity;
        velocityY = Math.min(velocityY, MAX_VELOCITY);  // Cap downward velocity

        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);  // Prevent bird from going off the top
        birdAngle = Math.toRadians(Math.min(45, Math.max(-45, velocityY * 5)));

        // Pipe movement
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            if (pipe.x + pipe.width < 0) {
                pipes.remove(i);
                i--;
            }

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                score += 0.5;
                pipe.passed = true;
            }

            if (collision(bird, pipe)) {
                gameOver = true;
            }
        }

        if (bird.y > boardHeight) {
            gameOver = true;
        }

        moveBackground();
    }

    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    void adjustDifficulty() {
        if ((int) score % 10 == 0 && score > 0) {
            velocityX--;
            pipeOpening = Math.max(pipeOpening - 5, boardHeight / 6);
        }
    }

    void drawBird(Graphics2D g2d) {
        g2d.rotate(birdAngle, bird.x + bird.width / 2, bird.y + bird.height / 2);
        g2d.drawImage(birdImg, bird.x, bird.y, bird.width, bird.height, null);
        g2d.rotate(-birdAngle, bird.x + bird.width / 2, bird.y + bird.height / 2);
    }

    void drawGameOver(Graphics g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, boardWidth, boardHeight);
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString("Game Over", boardWidth / 4, boardHeight / 2 - 50);
        g.drawString("Score: " + (int) score, boardWidth / 4, boardHeight / 2);
        g.drawString("High Score: " + (int) highScore, boardWidth / 4, boardHeight / 2 + 50);
        g.drawString("Press SPACE to Restart", boardWidth / 8, boardHeight / 2 + 100);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw background
        g.drawImage(backgroundImg, backgroundX, 0, boardWidth, boardHeight, null);
        g.drawImage(backgroundImg, backgroundX + boardWidth, 0, boardWidth, boardHeight, null);

        // Draw bird
        drawBird(g2d);

        // Draw pipes
        for (Pipe pipe : pipes) {
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // Draw score
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString("Score: " + (int) score, 10, 35);

        if (gameOver) {
            drawGameOver(g);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        adjustDifficulty();
        repaint();

        if (gameOver) {
            placePipeTimer.stop();
            gameLoop.stop();
            highScore = Math.max(score, highScore);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (gameOver) {
                // Restart game
                bird.y = birdY;
                velocityY = 0;
                pipes.clear();
                score = 0;
                gameOver = false;
                placePipeTimer.start();
                gameLoop.start();
            } else {
                velocityY = FLAP_STRENGTH;  // Bird flaps up
            }
        } else if (e.getKeyCode() == KeyEvent.VK_P) {
            paused = !paused;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}
