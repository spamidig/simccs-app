package simccs.gui;

import java.util.ArrayList;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import simccs.dataStore.Sink;
import simccs.dataStore.Source;

/**
 *
 * @author yaw
 */
class NetworkDisplay extends Pane {

    private DoubleProperty scale = new SimpleDoubleProperty(1.0);
    private ControlActions controlActions;

    public NetworkDisplay() {
        // add scale transform
        scaleXProperty().bind(scale);
        scaleYProperty().bind(scale);
    }

    public double getScale() {
        return scale.get();
    }

    public void setScale(double scale) {
        this.scale.set(scale);
    }

    public void setPivot(double x, double y) {
        setTranslateX(getTranslateX() - x);
        setTranslateY(getTranslateY() - y);
    }

    public void setControlActions(ControlActions controlActions) {
        this.controlActions = controlActions;
    }
    
    public ControlActions getControlActions() {
        return controlActions;
    }
}

class DragContext {

    double mouseAnchorX;
    double mouseAnchorY;

    double translateAnchorX;
    double translateAnchorY;
}

class SceneGestures {

    private static final double MAX_SCALE = 100.0d;
    private static final double MIN_SCALE = 1d;

    private DragContext sceneDragContext = new DragContext();
    private boolean mousePrimaryDown;

    NetworkDisplay canvas;

    private ArrayList<Pane> entitiesToResize = new ArrayList<>();
    private int sinkLayerIndex;
    private int sourceLayerIndex;
    private int candidateNetworkLayerIndex;
    private int solutionLayerIndex;
    private int ensembleSolutionLayerIndex;
    private double radius = 5;
    private double fontSize = 13;

    public SceneGestures(NetworkDisplay canvas) {
        this.canvas = canvas;
    }

    public void addEntityToResize(Pane p, String label) {
        entitiesToResize.add(p);
        if (label.matches("Sinks"))
            sinkLayerIndex = entitiesToResize.indexOf(p);
        else if (label.matches("Sources"))
            sourceLayerIndex = entitiesToResize.indexOf(p);
        else if (label.matches("CandidateNetwork"))
            candidateNetworkLayerIndex = entitiesToResize.indexOf(p);
        else if (label.matches("Solution"))
            solutionLayerIndex = entitiesToResize.indexOf(p);
        else if (label.matches("EnsembleSolution"))
            ensembleSolutionLayerIndex = entitiesToResize.indexOf(p);
    }

    public void addEntityToResize(Pane p) {
        entitiesToResize.add(p);
    }

    public EventHandler<MouseEvent> getOnMousePressedEventHandler() {
        return onMousePressedEventHandler;
    }

    public EventHandler<MouseEvent> getOnMouseDraggedEventHandler() {
        return onMouseDraggedEventHandler;
    }

    public EventHandler<MouseEvent> getOnMouseReleasedEventHandler() {
        return onMouseReleasedEventHandler;
    }

    public EventHandler<ScrollEvent> getOnScrollEventHandler() {
        return onScrollEventHandler;
    }

    // For testing.  Feel free to remove.
    public EventHandler<MouseEvent> getOnMouseMovedEventHandler() {
        return onMouseMovedEventHandler;
    }

    private EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {

        public void handle(MouseEvent event) {
            if (event.isPrimaryButtonDown()) {
                sceneDragContext.mouseAnchorX = event.getSceneX();
                sceneDragContext.mouseAnchorY = event.getSceneY();

                sceneDragContext.translateAnchorX = canvas.getTranslateX();
                sceneDragContext.translateAnchorY = canvas.getTranslateY();

                mousePrimaryDown = true;

                boolean found = false;
                for (Node node : entitiesToResize.get(sinkLayerIndex).getChildren()) {
                    if (node.contains(event.getX(), event.getY())) {
                        node.toFront();
                        if (((Circle) node).getFill().equals(Color.CORNFLOWERBLUE)) {
                            ((Circle) node).setFill(Color.GRAY);
                            ((Circle) node).setStroke(Color.GRAY);
                            ((Sink) ((DataCircle) node).getData()).disable();
                            found = true;
                            break;
                        }
                        else {
                            ((Circle) node).setFill(Color.CORNFLOWERBLUE);
                            ((Circle) node).setStroke(Color.CORNFLOWERBLUE);
                            ((Sink) ((DataCircle) node).getData()).enable();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    for (Node node : entitiesToResize.get(sourceLayerIndex).getChildren()) {
                        if (node.contains(event.getX(), event.getY())) {
                            node.toFront();
                            if (((Circle) node).getFill().equals(Color.SALMON)) {
                                ((Circle) node).setFill(Color.GRAY);
                                ((Circle) node).setStroke(Color.GRAY);
                                ((Source) ((DataCircle) node).getData()).disable();
                                break;
                            } else {
                                ((Circle) node).setFill(Color.SALMON);
                                ((Circle) node).setStroke(Color.SALMON);
                                ((Source) ((DataCircle) node).getData()).enable();
                                break;
                            }
                        }
                    }
                }
            } else if (event.isSecondaryButtonDown()) {
                /*int cellNum = canvas.controlActions.displayXYToVectorized(event.getX(), event.getY());
                HashMap<Integer, HashSet<Integer>> neighbors = canvas.controlActions.getData().getGraphNeighbors();
                String n = "";
                if (neighbors.containsKey(cellNum)) {
                    HashSet<Integer> neighborCells = neighbors.get(cellNum);
                    for (int cell : neighborCells) {
                        n += Integer.toString(cell) + " ";
                    }
                } else {
                    n = "None.";
                }
                canvas.controlActions.getMessenger().setText(cellNum + " Neighbors: " + n);*/
                boolean found = false;
                for (Node node : entitiesToResize.get(solutionLayerIndex).getChildren()) {
                    if (node instanceof DataCircle && node.contains(event.getX(), event.getY())) {
                        //The following ensures the integrity of the pie
                        int circleIndex = entitiesToResize.get(solutionLayerIndex).getChildren().indexOf(node);
                        node.toFront();
                        entitiesToResize.get(solutionLayerIndex).getChildren().get(circleIndex).toFront();

                        DataViewPopOver popOver = new DataViewPopOver(node, ((DataCircle) node).getData());
                        popOver.show();
                        found = true;
                        break;
                    }
                }
                /*FIXME: Have to do the same loop twice for the solutions as Path has to occur early in the list to be set in the back in visualization.
                * There might be better solutions, but I don't have one yet.*/
                if (!found) {
                    for (Node node : entitiesToResize.get(solutionLayerIndex).getChildren()) {
                        if (node instanceof Path && node.contains(event.getX(), event.getY())) {
                            DataViewPopOver popOver = new DataViewPopOver(node, node.getId());
                            popOver.show();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    for (Node node : entitiesToResize.get(ensembleSolutionLayerIndex).getChildren()) {
                        if (node instanceof DataCircle && node.contains(event.getX(), event.getY())) {
                            node.toFront();
                            DataViewPopOver popOver = new DataViewPopOver(node, ((DataCircle) node).getData());
                            popOver.show();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    for (Node node : entitiesToResize.get(ensembleSolutionLayerIndex).getChildren()) {
                        if (node instanceof Path && node.contains(event.getX(), event.getY())) {
                            DataViewPopOver popOver = new DataViewPopOver(node, node.getId());
                            popOver.show();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    for (Node node : entitiesToResize.get(sinkLayerIndex).getChildren()) {
                        if (node.contains(event.getX(), event.getY())) {
                            node.toFront();
                            DataViewPopOver popOver = new DataViewPopOver(node, ((DataCircle) node).getData());
                            popOver.show();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    for (Node node : entitiesToResize.get(sourceLayerIndex).getChildren()) {
                        if (node.contains(event.getX(), event.getY())) {
                            node.toFront();
                            DataViewPopOver popOver = new DataViewPopOver(node, ((DataCircle) node).getData());
                            popOver.show();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    for (Node node : entitiesToResize.get(candidateNetworkLayerIndex).getChildren()) {
                        if (node.contains(event.getX(), event.getY())) {
                            DataViewPopOver popOver = new DataViewPopOver(node, node.getId());
                            popOver.show();
                            break;
                        }
                    }
                }
            }
        }
    };

    private EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {
        public void handle(MouseEvent event) {
            if (event.getButton().equals(MouseButton.PRIMARY) && mousePrimaryDown) {
                canvas.setTranslateX(sceneDragContext.translateAnchorX + event.getSceneX() - sceneDragContext.mouseAnchorX);
                canvas.setTranslateY(sceneDragContext.translateAnchorY + event.getSceneY() - sceneDragContext.mouseAnchorY);
            }
            event.consume();
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>() {
        public void handle(MouseEvent event) {
            if (event.getButton().equals(MouseButton.PRIMARY) && mousePrimaryDown) {
                mousePrimaryDown = false;
            }
        }
    };

    private EventHandler<MouseEvent> onMouseMovedEventHandler = event -> {
        // For testing.  Feel free to remove.
//        System.out.println(event.getX() + ", " + event.getY() + ", " + event.getSceneX() + "," + event.getSceneY());
        if (canvas.getControlActions().getDataStorer() != null && canvas.contains(event.getX(), event.getY())) {
            double[] latlon = canvas.getControlActions().getDataStorer().cellToLatLon(canvas.getControlActions().displayXYToVectorized(event.getX(), event.getY()));
            canvas.getControlActions().getMessenger().setText("Lat: " + latlon[0] + "\nLon: " + latlon[1]);
        }
        event.consume();
    };

    private EventHandler<ScrollEvent> onScrollEventHandler = new EventHandler<ScrollEvent>() {

        @Override
        public void handle(ScrollEvent event) {

            double delta = 1.2;

            double scale = canvas.getScale();
            double oldScale = scale;

            if (event.getDeltaY() < 0) {
                scale /= delta;
            } else {
                scale *= delta;
            }

            scale = clamp(scale, MIN_SCALE, MAX_SCALE);

            double f = (scale / oldScale) - 1;

            double dx = (event.getSceneX() - (canvas.getBoundsInParent().getWidth() / 2 + canvas.getBoundsInParent().getMinX()));
            double dy = (event.getSceneY() - (canvas.getBoundsInParent().getHeight() / 2 + canvas.getBoundsInParent().getMinY()));

            canvas.setScale(scale);

            canvas.setPivot(f * dx, f * dy);

            // Resize components based on zoom level.
            resizeComponents(scale, oldScale);

            event.consume();
        }

        private void resizeComponents(double newScale, double oldScale) {
            // Resize entities.
            for (Pane p : entitiesToResize) {
                for (Node n : p.getChildren()) {
                    if (n instanceof Circle) {
                        Circle c = (Circle) n;
                        double radius = c.getRadius() * oldScale;
                        c.setRadius(radius / newScale);
                    } else if (n instanceof javafx.scene.shape.Arc) {
                        Arc arc = (Arc) n;
                        double radius = arc.getRadiusX() * oldScale;
                        arc.setRadiusX(radius / newScale);
                        arc.setRadiusY(radius / newScale);
                    } else if (n instanceof Label) {
                        Label l = (Label) n;
                        l.setFont(new Font("System Regular", fontSize / Math.max(newScale / 4, 1)));
                        // TODO: Going to have to be more clever to shift labels...
                    } else if (n instanceof Line) {
                        Line l = (Line) n;
                        double radius = l.getStrokeWidth() * oldScale;
                        l.setStrokeWidth(radius / newScale);
                    } else if (n instanceof Path) {
                        Path l = (Path) n;
                        double radius = l.getStrokeWidth() * oldScale;
                        l.setStrokeWidth(radius / newScale);
                    }
                }
            }
        }
    };

    public static double clamp(double value, double min, double max) {
        if (Double.compare(value, min) < 0) {
            return min;
        }

        if (Double.compare(value, max) > 0) {
            return max;
        }

        return value;
    }
}
