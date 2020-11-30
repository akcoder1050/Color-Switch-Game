package obstacles;

import elements.Ball;
import interfaces.Collideable;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import obstacles.controllers.SuperController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

abstract class Obstacle implements Collideable {
    public static int defaultRotatingDuration = 4000;
    // To store all transition objects
    private final List<Transition> transitions;
    @FXML
    protected Pane pane;
    protected SuperController controller;
    // We will center the pane
    // Movement open only in Y axis
    private double posY;

    public Obstacle(double posY) {
        this.posY = posY;
        transitions = new ArrayList<>();
    }

    // It will have a star if required
    // Stores Null if No Star is present inside the Obstacle
//    private Star star;

    // Getters and Setters

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    protected Pane getPane() {
        return pane;
    }

    protected void setPane(Pane pane) {
        this.pane = pane;
    }

    public double getWidth() {
        if(pane == null)
            return 0;
        return pane.getWidth();
    }

    protected List<Transition> getTransitions() {
        return transitions;
    }

    // To be verified, which type of bound is required
    @Override
    public Bounds getBounds() {
        if(pane == null)
            return null;
        return pane.getBoundsInParent();
    }

    @Override
    public int hasCollided(Ball c) {
        if(pane == null || c == null || c.getBounds() == null)
            return -1;
        return (getBounds().intersects(c.getBounds())) ? 1 : 0;
    }

    // Helper Functions

    // Attach itself to a particular Parent Node
    // To be modified later
    public void attachToGrid(GridPane parent, int i, int j) {
        parent.setAlignment(Pos.CENTER);
        parent.add(pane, i, j);
    }

    public void loadObstaclePane(String FXMLPath) {
        Pane temp = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXMLPath));
            temp = loader.load();
            controller = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pane = temp;
    }

    public void addRotation(Node node, int timeInMillis, boolean clockwise) {
        RotateTransition rt = new RotateTransition();
        rt.setAxis(Rotate.Z_AXIS);
        rt.setByAngle((clockwise) ? 360 : -360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.setDuration(Duration.millis(timeInMillis));
        rt.setNode(node);
        transitions.add(rt);
        rt.play();
    }

    public void addRotation(Node node) {
        addRotation(node, defaultRotatingDuration, true);
    }
}
