package simccs.gui;

import javafx.scene.shape.Circle;

public class DataCircle extends Circle {
    private Object data;

    DataCircle(double x, double y, double scale) {
        super(x, y, scale);
    }

    public void putData(Object data) {
        this.data = data;
    }

    public Object getData() {
        return this.data;
    }
}
