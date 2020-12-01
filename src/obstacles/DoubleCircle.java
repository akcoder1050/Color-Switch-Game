package obstacles;

import elements.Ball;
import obstacles.controllers.DoubleCircleController;

public class DoubleCircle extends Obstacle {
    private DoubleCircleController doubleCircleController;

    DoubleCircle(double posY) {
        // Load the FXML and set 'pane' in Parent
        loadObstaclePane("/obstacles/fxml/doubleCircle.fxml");

        doubleCircleController = (DoubleCircleController) controller;
    }

    @Override
    public int hasCollided(Ball b) {
        return 0;
    }
}
