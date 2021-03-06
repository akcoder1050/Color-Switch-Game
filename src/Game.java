import elements.Ball;
import elements.ColorSwitcherLogo;
import elements.ColourSwitcher;
import elements.Hand;
import global.Collideable;
import global.GameObjects;
import global.SuperController;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import javafx.util.Pair;
import obstacles.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game implements Serializable {
    private static final List<Class> map = Arrays.asList(
            Circle.class, CircleFlow.class, DoubleCircle.class, Plus.class, Square.class, DoubleCircleVertical.class
    );
    private transient final Ball ball;
    private final long id;
    private final Player player;

//    date and time when the game was last played
    private String date_time;
//     Includes Obstacles, ColourSwitcher's
    private transient final List<GameObjects> gameObjects;
//    Constants Required
    private static final double margin = 200, shift = 100, shiftDur = 300, width = 768, height = 1024, maxColorSwitcher = 3;
//     For storing the score
    int score;
//    For storing the list of all keyframes to be updated on a click
    private transient final List<DoubleProperty> objectsPosProperty;

//    It stores the number of colourSwitchers allowed to be loaded at a time on the screen
    private int colorSwitcherCount;

//    Min number of stars for which resurrection can be offered to player
    private final int resurrection_stars = 10;

    private volatile boolean gameOver;

    private List<Pair<Class, Double>> objectsPosition;

    public Game(Player player, Scene scene) {
        this.player = player;
        id = assignID();

        gameObjects = new ArrayList<>();
        objectsPosProperty = new ArrayList<>();
        score = 0;
        initialiseDateTime();
        objectsPosition = new ArrayList<>();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/scenes/game.fxml"));
            root = loader.load();
            scene.setRoot(root);
            Main.getInstance().scale(root);
            gameController = loader.getController();
            gameController.setGame(this);
            obstaclesBox = gameController.obstaclesBox;
        } catch (IOException e) {
            e.printStackTrace();
        }
        ball = new Ball();
        gameOver = false;
        initializeGame();

        if (Main.getInstance().isAutoSave()) player.saveGame(this);
    }

//     Controller With this Class
    private transient GameController gameController;
    @FXML
    private transient Pane obstaclesBox, root;

    private synchronized void toggleGameOver() {
        gameOver = !gameOver;
    }

    @Override
    public boolean equals(Object o1){
        if (o1 != null && getClass() == o1.getClass()){
            Game g = (Game) o1;
            return (this.id == g.id);
        }
        else{
            return false;
        }
    }

    private void attachGameObject(GameObjects ob) {
        double pos = height;
        if(gameObjects.size() != 0) {
            pos = gameObjects.get(gameObjects.size() - 1).getPosY().getValue();

            // To reduce gap in case of Colour Switchers
            if(gameObjects.get(gameObjects.size() - 1) instanceof ColourSwitcher || ob instanceof ColourSwitcher)
                pos += margin/2;

            // To add some offset
            // ColourSwitcher loads faster than Obstacle, and have less excess gap due to latency
            if(ob instanceof ColourSwitcher)
                pos -= margin/5;
        }
        pos -= margin + ob.getHeight();
        if(gameObjects.isEmpty()) {
            pos += margin;
            pos -= 30;
        }
        else if(gameObjects.size() <= 2) {
            pos += margin/2;
        }

        ob.attachToPane(obstaclesBox, (width-ob.getWidth())/2, pos);
        gameObjects.add(ob);
        objectsPosProperty.add(ob.getPosY());
    }

    private void newObstacle() {
        try {
            Class obsType = null;
            // No two consecutive Obstacles Should be Same
            while(obsType == null || (gameObjects.size() != 0 && obsType == gameObjects.get(gameObjects.size()-1).getClass())){
                obsType = map.get((int)(Math.random()*map.size()));
                if(map.size() == 1)
                    break;
            }
            Obstacle ob = (Obstacle) (obsType.getDeclaredConstructor().newInstance());
            attachGameObject(ob);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void newColorSwitcher() {
        ColourSwitcher cs = new ColourSwitcher();
        attachGameObject(cs);
        colorSwitcherCount++;
    }

    private void initializeGame() {
        attachGameObject(new Hand());
        attachGameObject(new ColorSwitcherLogo());
        attachGameObject(new Circle());

        for(int i=0; i<3; ++i){
            newObstacle();
        }

        ball.attachToPane((Pane)obstaclesBox.getParent(), (width/2-ball.getWidth()/2), gameObjects.get(0).getPosY().getValue() - ball.getHeight() - 10);

        Thread collisionThread = new Thread(new Collision(), "Collision Thread");
        collisionThread.start();
    }

    public void resumeGame(Scene scene) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/scenes/game.fxml"));
            root = loader.load();
            scene.setRoot(root);
            Main.getInstance().scale(root);
            gameController = loader.getController();
            gameController.setGame(this);
            gameController.setScore(score);
            obstaclesBox = gameController.obstaclesBox;
        } catch (IOException e) {
            e.printStackTrace();
        }

        gameObjects.clear();
        objectsPosProperty.clear();

        for(Pair<Class, Double> pair : objectsPosition) {
            try {
                GameObjects go = (GameObjects) (pair.getKey().getDeclaredConstructor().newInstance());
                if(go instanceof Ball) {
                    go.attachToPane((Pane)obstaclesBox.getParent(), (width/2-ball.getWidth()/2), pair.getValue());
                    ((Ball)go).getBallController().pause();
                }
                else {
                    go.attachToPane(obstaclesBox, (width - go.getWidth())/2, pair.getValue());
                    gameObjects.add(go);
                    objectsPosProperty.add(go.getPane().layoutYProperty());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(new Collision(), "Collision Thread").start();
    }

    public void shiftObstacles() {
        double shiftExcess = Math.min(shift, ball.getBallController().moveUp());
        Timeline timeline = new Timeline();
        timeline.setCycleCount(1);
        for(DoubleProperty property : objectsPosProperty) {
            KeyValue keyValue = new KeyValue(property, property.getValue() + shiftExcess*1.5);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(shiftDur), keyValue));
        }
        timeline.play();
        // Generate new obstacle if running out of obstacles to display
        updateGameObjects();
    }

    private long assignID() {
        String strFilePath = "./data/gameId";
        long input = 1;
        try {
            FileInputStream fin = new FileInputStream(strFilePath);
            DataInputStream din = new DataInputStream(fin);
            input = din.readLong();
            din.close();
        } catch(IOException fe) {
            System.out.println(fe.toString());
        } finally {
            FileOutputStream fout;
            try {
                fout = new FileOutputStream(strFilePath, false);
                DataOutputStream dout = new DataOutputStream(fout);
                dout.writeLong(input+1);
                dout.close();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }
        return input;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    private void updateGameObjects() {
        if(objectsPosProperty == null || objectsPosProperty.size() == 0)
            return;
        if(objectsPosProperty.get(objectsPosProperty.size()-1).getValue() > -height) {
            newObstacle();
            if(colorSwitcherCount < maxColorSwitcher && Math.random()*3 > 2)
                newColorSwitcher();
        }
        while(objectsPosProperty.size() > 0 && objectsPosProperty.get(0).getValue() > height) {
            if(gameObjects.get(0) instanceof ColourSwitcher)
                colorSwitcherCount--;
            objectsPosProperty.remove(0);
            gameObjects.remove(0);
        }
    }

    public String getDateTime() {
        return this.date_time;
    }

    public void setDateTime(String date_time) {
        this.date_time = date_time;
    }

    private void initialiseDateTime(){
        DateTimeFormatter dt_format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String dt = dt_format.format(now);
        setDateTime(dt);
    }

    private int level = 0;

    private synchronized void updateGameScore(){
        this.score ++;
        if(score % 5 == 0) {
            level = Math.max(level, 5);
            SuperController.defaultRotatingDuration -= 100 * level;
        }
        gameController.setScore(this.score);
    }

    public void saveObjectPositions(){
        for (GameObjects gameObject: gameObjects){
            objectsPosition.add(new Pair<>(gameObject.getClass(), gameObject.getPosY().getValue()));
        }
        objectsPosition.add(new Pair<>(ball.getClass(), ball.getPosY().getValue()));
    }

    private boolean setGameOver(Obstacle obstacle) throws Exception {
        int player_highscore = player.getHighScore();
        if (score > player_highscore) {
            player.setHighScore(score);
        }
        player.setStarsEarned(player.getStarsEarned() + score);

        saveObjectPositions();

        Thread thread = new Thread(() -> {
            try {
                player.deleteSavedGame(this);
                Main.getInstance().playGameOverSound();
                Main.getInstance().loadGameOver(score, player.getHighScore());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        if (player.getStarsEarned() > resurrection_stars){
            System.out.println("Resurrection possible!");
            int difference = player.getStarsEarned() - resurrection_stars;
            // give option of ressurection

            boolean success = gameController.displayResurrect();

            // resurrection to be implemented
            if(!success) {
                thread.start();
                return false;
            }
            resurrectPlayer(obstacle);
            player.setStarsEarned(difference);
            return true;
        }
        thread.start();

        return false;
    }

    private void resurrectPlayer(Obstacle obstacle){
        obstacle.getPane().setVisible(false);
        gameObjects.remove(obstacle);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    class Collision implements Runnable {
        @Override
        public void run() {
            long counter = 0;
            for (int i=0; ;++i) {
                if(i == -1) {
                    System.out.println("Invalid State! i is -1!");
                    return;
                }
                if(gameOver) {
                    System.out.println("Game is now over, exiting collision thread");
                    return;
                }
                try {
                    GameObjects object = gameObjects.get(i);
                    if(object instanceof Collideable) {
                        int ret = ((Collideable) object).hasCollided(ball);
                        /*return codes:
                        1. Obstacle = 1
                        2. Colour Switcher = 2
                        3. Star = -1
                        */
                        if(ret != 0) {
                            System.out.println("Collision detected, ret: " + ret + "; it: " + counter);
                            if (ret == -1){
                                // Collision with star
                                Main.getInstance().playStarCollisionSound();
                                updateGameScore();
                            }

                            if (ret == 2) Main.getInstance().playColourSwitchSound();

                            if (ret == 1){
                                // Collision with obstacle
                                Main.getInstance().playObstacleCollisionSound();
                                ball.getBallController().pause();
                                boolean resurrect = setGameOver((Obstacle) object);
                                if(!resurrect) {
                                    toggleGameOver();
                                }
                                else
                                    ball.getBallController().resume();

                            }
                        }
                    }
                    // To increase efficiency
                    counter++;
                    if(i > 3)
                        throw new Exception("reached too far!");
                }
                catch(Exception e) {
                    i = -1;
                }

                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}