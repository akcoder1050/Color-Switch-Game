import elements.Ball;
import global.GameObjects;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import obstacles.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game implements Serializable {
    private static final List<Class> map = Arrays.asList(
            Circle.class, CircleFlow.class, DoubleCircle.class, Plus.class, Square.class
    );
    private final Ball ball;
    private final long id;
    private final Player player;
//     Includes Obstacles, ColourSwitcher's
    private final List<GameObjects> gameObjects;
//    Constants Required
    private static final double margin = 75, shift = 100, shiftDur = 30, width = 768, height = 1024;
//     For storing the score
    int score;
//    For storing the list of all keyframes to be updated on a click
    private final List<DoubleProperty> objectsPosProperty;

//     Controller With this Class
    private GameController gameController;
    @FXML
    private Pane obstaclesBox;

    public Game(Player player, Scene scene) {
        this.player = player;
        id = assignID();
        gameObjects = new ArrayList<>();
        objectsPosProperty = new ArrayList<>();
        ball = new Ball();
        score = 0;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/scenes/game.fxml"));
            Pane root = loader.load();
            scene.setRoot(root);
            gameController = loader.getController();
            gameController.setGame(this);
            obstaclesBox = gameController.obstaclesBox;
        } catch (IOException e) {
            e.printStackTrace();
        }

        initializeGame();
    }

    private void rememberGameObject(GameObjects object) {
        gameObjects.add(object);
        objectsPosProperty.add(object.getPosY());
    }

    private void newObstacle() {
        try {
            double pos = height;
            if(gameObjects.size() != 0)
                pos = gameObjects.get(gameObjects.size()-1).getPosY().getValue();
            Obstacle ob = (Obstacle) (map.get((int)(Math.random()*map.size()))).getDeclaredConstructor().newInstance();
            pos -= margin + ob.getHeight();
            ob.attachToPane(obstaclesBox, (width-ob.getWidth())/2, pos);
            rememberGameObject(ob);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeGame() {
        double pos = 1024 - margin;
        double x = 768;
        for(int i=0; i<3; ++i){
            newObstacle();
        }
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

    public void shiftObstacles() {
        Timeline timeline = new Timeline();
        timeline.setCycleCount(1);
        timeline.setRate(0.1);
        for(DoubleProperty property : objectsPosProperty) {
            KeyValue keyValue = new KeyValue(property, property.getValue() + shift);
            KeyFrame keyFrame = new KeyFrame(Duration.millis(shiftDur), keyValue);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(shiftDur), keyValue));
        }
        timeline.play();
    }
}