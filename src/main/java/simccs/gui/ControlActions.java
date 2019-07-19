package simccs.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.simccs.desktop.SimCCSDesktop;
import org.simccs.desktop.ui.commons.SimCCSDialogHelper;
import org.simccs.desktop.ui.home.HomeWindow;
import org.simccs.desktop.ui.login.LoginWindowSmall;
import org.simccs.desktop.util.SimCCSContext;
import org.simccs.desktop.util.messaging.SimCCSEvent;
import org.simccs.desktop.util.messaging.SimCCSEventBus;
import simccs.dataStore.*;
import simccs.solver.MPSWriter;
import simccs.solver.ProcessUncertainty;
import simccs.solver.Solver;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipFile;

import static simccs.utilities.Utilities.round;

/**
 *
 * @author yaw
 */
public class ControlActions {

    private String basePath = "";
    private String dataset = "";
    private String scenario = "";

    private DataStorer data;
    private Solver solver;
    private ImageView map;
    private Pane sourceLocationsLayer;
    private Pane sinkLocationsLayer;
    private Pane sourceLabelsLayer;
    private Pane sinkLabelsLayer;
    private Pane shortestPathsLayer;
    private Pane candidateNetworkLayer;
    private Pane rawDelaunayLayer;
    private Pane solutionLayer;
    private Pane ensembleSolutionLayer;
    private TextArea messenger;
    private Gui gui;

    public ControlActions(ImageView map, Gui gui) {
        this.map = map;
        this.gui = gui;
    }

    public void toggleCostSurface(Boolean show, Rectangle background) {
        if (show && data != null) {
//            Image img = new Image("file:" + data.getCostSurfacePath());

            double[][] adjacencyCosts = data.getAdjacencyCosts();
            Double[] costSurface = new Double[data.getWidth() * data.getHeight() + 1];
            for (int i = 0; i < adjacencyCosts.length; i++) {
                costSurface[i] = 0.0;
                for (int j = 0; j < adjacencyCosts[i].length; j++) {
                    if (adjacencyCosts[i][j] != Double.MAX_VALUE) {
                        costSurface[i] += adjacencyCosts[i][j];
                    }
                }
            }

//            for (int i = 0; i < costSurface.length; i++) { costSurface[i] = i * 1.0;}
            Image img = getImageFromArray(costSurface, data.getWidth(), data.getHeight());
            map.setImage(img);

            // Adjust background.
            background.setWidth(map.getFitWidth());
            background.setHeight(map.getFitHeight());
        } else {
            map.setImage(null);
        }
    }

    public static Image getImageFromArray(Double[] pixels, int width, int height) {
//        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//        WritableRaster raster = (WritableRaster) image.getData();
//        raster.setPixels(0,0,width,height,pixels);
//        image.setData(raster);
        double minV = Collections.min(Arrays.asList(pixels));
        double maxV = Collections.max(Arrays.asList(pixels));
        for(int i=0; i<pixels.length; i++) {pixels[i] -= minV;}
        for(int i=0; i<pixels.length; i++) {pixels[i] = 1.0 - pixels[i] / (maxV - minV);}
        BufferedImage image = new BufferedImage(width, height, 3);

        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                float c = (float) pixels[x + y * width].doubleValue();
                int rgb=new java.awt.Color(c,c,c).getRGB();
                image.setRGB(x, y, rgb);
            }
        }
        return SwingFXUtils.toFXImage(image, null);
    }

    public void selectDataset(File datasetPath, ChoiceBox scenarioChoice) {
        // Clear GUI
        gui.fullReset();

        this.dataset = datasetPath.getName();
        this.basePath = datasetPath.getParent();

        // Populate scenarios ChoiceBox.
        File f = new File(datasetPath, "Scenarios");
        if (f.listFiles() != null) {
            ArrayList<String> dirs = new ArrayList<>();
            for (File file : f.listFiles()) {
                if (file.isDirectory() && file.getName().charAt(0) != '.') {
                    dirs.add(file.getName());
                }
            }
            scenarioChoice.setItems(FXCollections.observableArrayList(dirs));
        }
    }

/*    public void initializeDatasetSelection(ChoiceBox datasetChoice) {
        // Set initial datasets.
        File f = new File(basePath);
        ArrayList<String> dirs = new ArrayList<>();
        for (File file : f.listFiles()) {
            if (file.isDirectory() && file.getName().charAt(0) != '.') {
                dirs.add(file.getName());
            }
        }
        datasetChoice.setItems(FXCollections.observableArrayList(dirs));
    }*/

    public void selectScenario(String scenario, Rectangle background, ComboBox solutionChoice, ComboBox ensembleSolutionChoice, ComboBox inEnsembleSolutionChoice) {
        if (scenario != null) {
            gui.softReset();
            this.scenario = scenario;

            //enable selection menu
            //do initial drawing
            data = new DataStorer(basePath, dataset, scenario);
            solver = new Solver(data);
            data.setSolver(solver);
            //dataStorer.loadData();
            solver.setMessenger(messenger);
            gui.displayCostSurface();

            // Load solutions.
            initializeSolutionSelection(solutionChoice);
            initializeEnsembleSolutionSelection(ensembleSolutionChoice, inEnsembleSolutionChoice);
        }
    }

    public void toggleSourceDisplay(boolean show) {
        if (show && data != null) {
            /*Set<Integer> test = data.getJunctions();
            Integer[] locations = test.toArray(new Integer[0]);
            for (int temp: locations) {
                double[] rawXYLocation = data.cellLocationToRawXY(temp);
                Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 1);
                c.setStroke(Color.BLACK);
                c.setFill(Color.BLACK);
                sinkLocationsLayer.getChildren().add(c);
            }*/

                for (Source source : data.getSources()) {
                    double[] rawXYLocation = data.cellLocationToRawXY(source.getCellNum());
                    DataCircle c = new DataCircle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 5 / gui.getScale());
                    c.putData(source);
                    if (source.isDisabled()) {
                        c.setStroke(Color.GRAY);
                        c.setFill(Color.GRAY);
                    } else {
                        c.setStroke(Color.SALMON);
                        c.setFill(Color.SALMON);
                    }
                    //c.setStroke(Color.RED);
                    //c.setFill(Color.RED);
                    sourceLocationsLayer.getChildren().add(c);
                }
        } else {
            sourceLocationsLayer.getChildren().clear();
            sourceLabelsLayer.getChildren().clear();
        }
    }

    public void toggleSourceLabels(boolean show) {
        if (show && data != null) {
                for (Source source : data.getSources()) {
                    Label l = new Label(source.getLabel());
                    double[] rawXYLocation = data.cellLocationToRawXY(source.getCellNum());
                    l.setTranslateX(rawXtoDisplayX(rawXYLocation[0]) + 1);
                    l.setTranslateY(rawXtoDisplayX(rawXYLocation[1]) + 1);
                    sourceLabelsLayer.getChildren().add(l);
                }
        } else {
            sourceLabelsLayer.getChildren().clear();
        }
    }

    public void toggleSinkDisplay(boolean show) {
        if (show && data != null) {
            //Set<Edge> test = dataStorer.getGraphEdgeRoutes().keySet();
            //for (Edge e : test) {
            //    double[] rawXYLocation = dataStorer.cellLocationToRawXY(e.v1);
            //    Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 1);
            //    c.setStroke(Color.ORANGE);
            //    c.setFill(Color.ORANGE);
            //    sinkLocationsLayer.getChildren().add(c);
            //
            //    rawXYLocation = dataStorer.cellLocationToRawXY(e.v2);
            //    Circle c2 = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 1);
            //    c2.setStroke(Color.ORANGE);
            //    c2.setFill(Color.ORANGE);
            //    sinkLocationsLayer.getChildren().add(c2);
            //}

                for (Sink sink : data.getSinks()) {
                    double[] rawXYLocation = data.cellLocationToRawXY(sink.getCellNum());
                    DataCircle c = new DataCircle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 5 / gui.getScale());
                    c.putData(sink);
                    if (sink.isDisabled()) {
                        c.setStroke(Color.GRAY);
                        c.setFill(Color.GRAY);
                    } else {
                        c.setStroke(Color.CORNFLOWERBLUE);
                        c.setFill(Color.CORNFLOWERBLUE);
                    }
                    //c.setStroke(Color.BLUE);
                    //c.setFill(Color.BLUE);
                    sinkLocationsLayer.getChildren().add(c);
                }
        } else {
            sinkLocationsLayer.getChildren().clear();
            sinkLabelsLayer.getChildren().clear();
        }
    }

    public void toggleSinkLabels(boolean show) {
        if (show && data != null) {
                for (Sink sink : data.getSinks()) {
                    Label l = new Label(sink.getLabel());
                    double[] rawXYLocation = data.cellLocationToRawXY(sink.getCellNum());
                    l.setTranslateX(rawXtoDisplayX(rawXYLocation[0]) + 1);
                    l.setTranslateY(rawXtoDisplayX(rawXYLocation[1]) + 1);
                    sinkLabelsLayer.getChildren().add(l);
                }
        } else {
            sinkLabelsLayer.getChildren().clear();
        }
    }

    public void generateShortestPathsNetwork() {
        if (!Objects.equals(scenario, "")) {

            data.generateShortestPaths();
            String file = "RawPaths.txt";
            NetworkData nwData = data.loadNetworkData(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/RawPaths/" + file, "Raw");
            //System.out.println( "Calling makeShapeFile in ControlAction for Generate Rawpaths" );
            data.makeShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/RawPaths/","Raw", nwData);
            //System.out.println( "makeShapeFile in ControlAction for Rawpaths Done" );
        }
    }

    public void generateCandidateNetwork() {
        if (!Objects.equals(scenario, "")) {
            data.generateCandidateGraph();
            String file = "CandidateNetwork.txt";
            NetworkData nwData = data.loadNetworkData(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/CandidateNetwork/" + file, "Candidate");
            //System.out.println( "Calling makeShapeFile in ControlActions to Generate Candidatepaths" );
            data.makeShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/CandidateNetwork/","Candidate", nwData);
            //System.out.println( "makeShapeFile in ControlActions for Candidatepaths done" );
        }
    }

    public void generateCandidateGraph() {
        if (!Objects.equals(scenario, "")) {
            data.generateCandidateGraph();
        }
    }

    public void saveSourceSinkState() {
        data.saveSourceSinkState(basePath + "/" + dataset + "/Scenarios/" + scenario, data);
    }

    public void generateMPSFile(String crf, String numYears, String capacityTarget) {
        if (!Objects.equals(scenario, "")) {
            System.out.println("Writing MPS File...");
            messenger.setText("Writing MPS File...");
            MPSWriter.writeMPS("mip.mps", data, Double.parseDouble(crf), Double.parseDouble(numYears), Double.parseDouble(capacityTarget), basePath, dataset, scenario);
            messenger.setText("MPS File Ready!");
        }
    }

    // Code to manage science gateway interface
    public void sendSingleJobToGateway () {
        sendJobToGateway("mip.mps", SimCCSEvent.SimCCSEventType.EXPORT_SINGLE_CPLEX_EXP);
    }

    public void loadSinkUncertainty(String sinkUncertaintyPath) {
        if (!Objects.equals(scenario, "")) {
            data.loadSinkUncertainties(sinkUncertaintyPath, data);
        }
    }

    public void loadSourceUncertainty(String sourceUncertaintyPath) {
        if (!Objects.equals(scenario, "")) {
            data.loadSourceUncertainties(sourceUncertaintyPath, data);
        }
    }

    //TODO: to be completed to generate random number from any distributions
    public void generateZippedMPSFile(String crf, String numYears, String capacityTarget, String simulationNumber) {
        if (!Objects.equals(scenario, "")) {
            if (data.getSinkUncertainties() != null || data.getSourceUncertainties() != null) {
                ProgressForm pForm = new ProgressForm(StageStyle.UTILITY, Modality.NONE);
                Task<Void> taskGenerateZippedMPSFile = new ProcessUncertainty(basePath, dataset, scenario, data, crf, numYears, capacityTarget, simulationNumber, messenger);
                pForm.setProgressTitle("Generating files...");
                pForm.activateProgressBar(new Task[]{taskGenerateZippedMPSFile});
                pForm.getDialogStage().setOnCloseRequest(Event::consume);
                taskGenerateZippedMPSFile.setOnSucceeded(event -> pForm.getDialogStage().close());
                pForm.getDialogStage().show();
//                Thread thread = new Thread(taskGenerateZippedMPSFile);
//                thread.start();
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(taskGenerateZippedMPSFile);
            } else {
//                JOptionPane.showMessageDialog(null,"Please Load Uncertainties first." , "Data not found", JOptionPane.ERROR_MESSAGE);
                SimCCSDialogHelper.showWarningDialog("Warning", "Data not found", "Please Load Uncertainties first.");
            }
        }
    }

    public String getCurrentScenarioDirectory() {
        return basePath + "/" + dataset + "/Scenarios/" + scenario;
    }

    public void sendEnsembleJobToGateway() {
        sendJobToGateway("mip.zip", SimCCSEvent.SimCCSEventType.EXPORT_ENSEMBLE_CPLEX_EXP);
    }

    private void sendJobToGateway(String fileName, SimCCSEvent.SimCCSEventType simCCSEventType) {
        //TODO: to be improved
        String mipPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/" + fileName;
        File f = new File(mipPath);
        if (f.exists()){
            try {
                File dataDir = new File(SimCCSDesktop.applicationDataDir());
                if (!dataDir.exists()) {
                    SimCCSDesktop.initApplicationDirs();
                }
                if (!SimCCSContext.getInstance().getAuthenticated()) {
                    LoginWindowSmall loginWindowSmall =  new LoginWindowSmall();
                    loginWindowSmall.displayLoginAndWait();
                    boolean isAuthenticated = SimCCSContext.getInstance().getAuthenticated();
                    if (isAuthenticated) {
                        Stage primaryStage = new Stage();
                        HomeWindow homeWindow = new HomeWindow();
                        Screen screen = Screen.getPrimary();
                        Rectangle2D bounds = screen.getVisualBounds();
                        primaryStage.setX(bounds.getMinX());
                        primaryStage.setY(bounds.getMinY());
                        primaryStage.setWidth(bounds.getWidth());
                        primaryStage.setHeight(bounds.getHeight());
                        homeWindow.start(primaryStage);
                        primaryStage.setOnCloseRequest(t -> {
//                            java.net.CookieHandler.setDefault(new com.sun.webkit.network.CookieManager());
//                            SimCCSContext.getInstance().setAuthenticated(false);
//                            SimCCSContext.getInstance().reset();
                            SimCCSEventBus.getInstance().post(new SimCCSEvent(SimCCSEvent.SimCCSEventType
                                    .LOGOUT, null));
//                            SimCCSEventBus.getInstance().unregister(HomeController);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (SimCCSContext.getInstance().getAuthenticated()) {
                Platform.runLater(() -> SimCCSEventBus.getInstance().post(new SimCCSEvent(simCCSEventType, mipPath)));
            }
        } else {
//            JOptionPane.showMessageDialog(null,"Please generate MPS file first." , "File not found", JOptionPane.ERROR_MESSAGE);
            SimCCSDialogHelper.showWarningDialog("Warning", "File not found", "Please generate MPS file first.");
        }
    }

    public void toggleShortestPathsDisplay(boolean show) {
        if (show && data != null) {
            int[][] rawPaths = data.getShortestPathEdges();
            HashSet<Edge> edges = new HashSet<>();
            for (int[] path : rawPaths) {
                for (int i = 0; i < path.length - 1; i++) {
                    Edge edge = new Edge(path[i], path[i + 1]);
                    edges.add(edge);
                }
            }
            for (Edge edge : edges) {
                double[] rawSrc = data.cellLocationToRawXY(edge.v1);
                double[] rawDest = data.cellLocationToRawXY(edge.v2);
                double sX = rawXtoDisplayX(rawSrc[0]);
                double sY = rawYtoDisplayY(rawSrc[1]);
                double dX = rawXtoDisplayX(rawDest[0]);
                double dY = rawYtoDisplayY(rawDest[1]);
                Line line = new Line(sX, sY, dX, dY);
                line.setStroke(Color.BLACK);
                line.setStrokeWidth(1.0 / gui.getScale());
                line.setStrokeLineCap(StrokeLineCap.ROUND);
                shortestPathsLayer.getChildren().add(line);
            }
        } else {
            shortestPathsLayer.getChildren().clear();
        }

        // Alternate way that may be useful.
        /*if (show && data != null) {
            int[][] rawPaths = data.getShortestPathEdges();
            for (int p = 0; p < rawPaths.length; p++) {
                int[] path = rawPaths[p];
                Path pathObj = new Path();
                double[] rawSrc = data.cellLocationToRawXY(path[0]);
                pathObj.getElements().add(new MoveTo(rawXtoDisplayX(rawSrc[0]), rawYtoDisplayY(rawSrc[1])));
                for (int dest = 1; dest < path.length; dest++) {
                    double[] rawDest = data.cellLocationToRawXY(path[dest]);
                    LineTo line = new LineTo(rawXtoDisplayX(rawDest[0]), rawYtoDisplayY(rawDest[1]));
                    pathObj.getElements().add(line);
                }
                shortestPathsLayer.getChildren().add(pathObj);
            }
        } else {
            shortestPathsLayer.getChildren().clear();
        }*/
    }

    public void toggleRawDelaunayDisplay(boolean show) {
        if (show & data != null) {
            HashSet<int[]> delaunayEdges = data.getDelaunayEdges();
            for (int[] path : delaunayEdges) {
                for (int src = 0; src < path.length - 1; src++) {
                    int dest = src + 1;
                    double[] rawSrc = data.cellLocationToRawXY(path[src]);
                    double[] rawDest = data.cellLocationToRawXY(path[dest]);
                    double sX = rawXtoDisplayX(rawSrc[0]);
                    double sY = rawYtoDisplayY(rawSrc[1]);
                    double dX = rawXtoDisplayX(rawDest[0]);
                    double dY = rawYtoDisplayY(rawDest[1]);
                    Line edge = new Line(sX, sY, dX, dY);
                    edge.setStroke(Color.BROWN);
                    edge.setStrokeWidth(1.0 / gui.getScale());
                    edge.setStrokeLineCap(StrokeLineCap.ROUND);
                    rawDelaunayLayer.getChildren().add(edge);
                }
            }
        } else {
            rawDelaunayLayer.getChildren().clear();
        }
    }

    public void toggleCandidateNetworkDisplay(boolean show) {
        if (show && data != null) {
            HashMap<Edge, int[]> sRoutes = data.getGraphEdgeRoutes();
            for (Map.Entry<Edge, int[]> route : sRoutes.entrySet()) {
                Path path = new Path();
                for (int src = 0; src < route.getValue().length - 1; src++) {
                    int dest = src + 1;
                    double[] rawSrc = data.cellLocationToRawXY(route.getValue()[src]);
                    double[] rawDest = data.cellLocationToRawXY(route.getValue()[dest]);
                    MoveTo moveTo = new MoveTo();
                    moveTo.setX(rawXtoDisplayX(rawSrc[0]));
                    moveTo.setY(rawYtoDisplayY(rawSrc[1]));
                    LineTo lineTo = new LineTo();
                    lineTo.setX(rawXtoDisplayX(rawDest[0]));
                    lineTo.setY(rawYtoDisplayY(rawDest[1]));
                    path.getElements().add(moveTo);
                    path.getElements().add(lineTo);
                }
                double totalCost = round(data.getGraphEdgeCosts().get(route.getKey()), 3);
                double constructionCost = round(data.getGraphEdgeConstructionCosts().get(route.getKey()), 3);
                double rightOfWayCost = round(data.getGraphEdgeRightOfWayCosts().get(route.getKey()), 3);
                path.setStroke(Color.PURPLE);
                path.setStrokeWidth(3.0 / gui.getScale());
                path.setStrokeLineCap(StrokeLineCap.ROUND);
                path.setId("CandidateNetwork\t" + String.valueOf(totalCost) + "\t" + String.valueOf(constructionCost) + "\t" + String.valueOf(rightOfWayCost));
                candidateNetworkLayer.getChildren().add(path);
            }
        } else {
            candidateNetworkLayer.getChildren().clear();
        }
    }

    public void initializeSolutionSelection(ComboBox solutionChoice) {
        if (!Objects.equals(basePath, "") && !Objects.equals(dataset, "") && !Objects.equals(scenario, "")) {
            // Set initial datasets.
            File f = new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results");
            ArrayList<String> solns = new ArrayList<>();
            solns.add("None");
            if (f.listFiles() != null) {
                for (File file : f.listFiles()) {
                    if (file.isDirectory() && file.getName().charAt(0) != '.') {
                        boolean sol = false;
                        boolean mps = false;
                        for (File subFile : file.listFiles()) {
                            if (subFile.getName().endsWith(".sol")) {
                                sol = true;
                            } else if (subFile.getName().endsWith(".mps")) {
                                mps = true;
                            }
                        }
                        if (sol && mps) {
                            solns.add(file.getName());
                        }
                    }
                }
            }
            solutionChoice.setItems(FXCollections.observableArrayList(solns));
        }
    }

    public void initializeEnsembleSolutionSelection(ComboBox ensembleSolutionChoice, ComboBox inEnsembleSolutionChoice) {
        if (!Objects.equals(basePath, "") && !Objects.equals(dataset, "") && !Objects.equals(scenario, "")) {
            // Set initial datasets.
            File f = new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results");
            ArrayList<String> solns = new ArrayList<>();
            ArrayList<String> insolns = new ArrayList<>();
            solns.add("None");
            insolns.add("None");
            if (f.listFiles() != null) {
                for (File file : f.listFiles()) {
                    if (file.isDirectory() && file.getName().charAt(0) != '.') {
                        boolean solZip = false;
                        boolean mpsZip = false;
                        for (File subFile : file.listFiles()) {
                            if (subFile.getName().endsWith("result.zip")) {
                                solZip = true;
                            } else if (subFile.getName().endsWith("mip.zip")) {
                                mpsZip = true;
                            }
                        }
                        if (solZip && mpsZip) {
                            solns.add(file.getName());
                        }
                    }
                }
            }
            ensembleSolutionChoice.setItems(FXCollections.observableArrayList(solns));
            inEnsembleSolutionChoice.setItems(FXCollections.observableArrayList(insolns));
        }
    }

    //TODO: Everything with the aggSolution is too awful. Need to be redesigned.
    public void selectSolution(String file, Label[] solutionValues) {
        solutionLayer.getChildren().clear();
        for (Label l : solutionValues) {
            l.setText("-");
        }

        if (file != null && !file.equals("None")) {
            Solution soln = data.loadSolution(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file);
            HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();

            for (Edge e : soln.getOpenedEdges()) {
                int[] route = graphEdgeRoutes.get(e);
                Path path = new Path();
                for (int src = 0; src < route.length - 1; src++) {
                    int dest = src + 1;
                    double[] rawSrc = data.cellLocationToRawXY(route[src]);
                    double[] rawDest = data.cellLocationToRawXY(route[dest]);
                    MoveTo moveTo = new MoveTo();
                    moveTo.setX(rawXtoDisplayX(rawSrc[0]));
                    moveTo.setY(rawYtoDisplayY(rawSrc[1]));
                    LineTo lineTo = new LineTo();
                    lineTo.setX(rawXtoDisplayX(rawDest[0]));
                    lineTo.setY(rawYtoDisplayY(rawDest[1]));
                    path.getElements().add(moveTo);
                    path.getElements().add(lineTo);
                }
                double transportAmount = round(soln.getEdgeTransportAmounts().get(e), 3);
                double transportCost = round(soln.getEdgeCosts().get(e), 3);
                double totalCost = round(data.getGraphEdgeCosts().get(e), 3);
                double constructionCost = round(data.getGraphEdgeConstructionCosts().get(e), 3);
                double rightOfWayCost = round(data.getGraphEdgeRightOfWayCosts().get(e), 3);
                path.setStroke(Color.GREEN);
                path.setStrokeWidth(5.0 / gui.getScale());
                path.setStrokeLineCap(StrokeLineCap.ROUND);
                path.setId("SolutionNetwork\t" + String.valueOf(transportAmount) + "\t" + String.valueOf(transportCost) + "\t" + String.valueOf(totalCost) + "\t" + String.valueOf(constructionCost) + "\t" + String.valueOf(rightOfWayCost));
                solutionLayer.getChildren().add(path);
            }

            for (Source source : soln.getOpenedSources()) {
                double[] rawXYLocation = data.cellLocationToRawXY(source.getCellNum());
                DataCircle c = new DataCircle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 20 / gui.getScale());
                c.setStrokeWidth(0);
                c.setStroke(Color.SALMON);
                c.setFill(Color.SALMON);
                Object[] sourceSolution = new Object[2];
                sourceSolution[0] = source;
                sourceSolution[1] = soln;
                c.putData(sourceSolution);
                solutionLayer.getChildren().add(c);

                // Pie chart nodes.
                Arc arc = new Arc();
                arc.setCenterX(rawXtoDisplayX(rawXYLocation[0]));
                arc.setCenterY(rawYtoDisplayY(rawXYLocation[1]));
                arc.setRadiusX(20 / gui.getScale());
                arc.setRadiusY(20 / gui.getScale());
                arc.setStartAngle(0);
                arc.setLength(soln.getPercentCaptured(source) * 360);
                arc.setStrokeWidth(0);
                arc.setType(ArcType.ROUND);
                arc.setStroke(Color.RED);
                arc.setFill(Color.RED);
                solutionLayer.getChildren().add(arc);
            }

            for (Sink sink : soln.getOpenedSinks()) {
                double[] rawXYLocation = data.cellLocationToRawXY(sink.getCellNum());
                DataCircle c = new DataCircle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 20 / gui.getScale());
                c.setStrokeWidth(0);
                c.setStroke(Color.CORNFLOWERBLUE);
                c.setFill(Color.CORNFLOWERBLUE);
                Object[] sinkSolution = new Object[2];
                sinkSolution[0] = sink;
                sinkSolution[1] = soln;
                c.putData(sinkSolution);
                solutionLayer.getChildren().add(c);

                // Pie chart nodes.
                Arc arc = new Arc();
                arc.setCenterX(rawXtoDisplayX(rawXYLocation[0]));
                arc.setCenterY(rawYtoDisplayY(rawXYLocation[1]));
                arc.setRadiusX(20 / gui.getScale());
                arc.setRadiusY(20 / gui.getScale());
                arc.setStartAngle(0);
                arc.setLength(soln.getPercentStored(sink) * 360);
                arc.setStrokeWidth(0);
                arc.setType(ArcType.ROUND);
                arc.setStroke(Color.BLUE);
                arc.setFill(Color.BLUE);
                solutionLayer.getChildren().add(arc);
            }

            // Update solution values.
            solutionValues[0].setText(Integer.toString(soln.getNumOpenedSources()));
            solutionValues[1].setText(Integer.toString(soln.getNumOpenedSinks()));
            solutionValues[2].setText(Double.toString(round(soln.getTargetCaptureAmount(), 2)));
            solutionValues[3].setText(Integer.toString(soln.getNumEdgesOpened()));
            solutionValues[4].setText(Integer.toString(soln.getProjectLength()));
            solutionValues[5].setText(Double.toString(round(soln.getTotalCaptureCost(), 2)));
            solutionValues[6].setText(Double.toString(round(soln.getUnitCaptureCost(), 2)));
            solutionValues[7].setText(Double.toString(round(soln.getTotalTransportCost(), 2)));
            solutionValues[8].setText(Double.toString(round(soln.getUnitTransportCost(), 2)));
            solutionValues[9].setText(Double.toString(round(soln.getTotalStorageCost(), 2)));
            solutionValues[10].setText(Double.toString(round(soln.getUnitStorageCost(), 2)));
            solutionValues[11].setText(Double.toString(round(soln.getTotalCost(), 2)));
            solutionValues[12].setText(Double.toString(round(soln.getUnitTotalCost(), 2)));

            // Write to shapefiles.
            data.makeShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file, soln);
        } else {
            solutionLayer.getChildren().clear();
        }
    }

    public void selectEnsembleSolution(String file, ComboBox inEnsembleSolutionChoice, Label[] solutionValues) {
        solutionLayer.getChildren().clear();
        ensembleSolutionLayer.getChildren().clear();
        for (Label l : solutionValues) {
            l.setText("-");
        }

        if (file != null && !file.equals("None")) {
            if (new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file + "/mip.zip").exists()
                    && new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file + "/result.zip").exists()) {
                zippedSolutions(file);
            }
        }
        initializeInEnsembleSolutionSelection(file, inEnsembleSolutionChoice);
    }

    public void initializeInEnsembleSolutionSelection(String ensemble, ComboBox inEnsembleSolutionChoice) {
        if (!Objects.equals(basePath, "") && !Objects.equals(dataset, "") && !Objects.equals(scenario, "")) {
            if (ensemble != null && !ensemble.equals("None")) {
                // Set initial datasets.
                File f = new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + ensemble);
                ArrayList<String> solns = new ArrayList<>();
                solns.add("None");
                File[] filelist = f.listFiles();
                Arrays.sort(filelist);
                for (File file : filelist) {
                    if (file.isDirectory() && file.getName().charAt(0) != '.') {
                        boolean sol = false;
                        boolean mps = false;
                        for (File subFile : file.listFiles()) {
                            if (subFile.getName().endsWith(".sol")) {
                                sol = true;
                            } else if (subFile.getName().endsWith(".mps")) {
                                mps = true;
                            }
                        }
                        if (sol && mps) {
                            solns.add(file.getName());
                        }
                    }
                }
                inEnsembleSolutionChoice.setItems(FXCollections.observableArrayList(solns));
            }
        }
    }

    public void scrollSolutions(ScrollEvent s, ComboBox solutionChoice) {
        double direction = s.getDeltaY();
        String currentChoice = (String) solutionChoice.getValue();
        ObservableList<String> choices = solutionChoice.getItems();
        int index = choices.indexOf(currentChoice);
        if (direction < 0 && index < choices.size() - 1) {
            solutionChoice.setValue(choices.get(index + 1));
        } else if (direction > 0 && index > 0) {
            solutionChoice.setValue(choices.get(index - 1));
        }
    }

    public void pageThroughSolutions(KeyEvent ke, ComboBox solutionChoice) {
        String currentChoice = (String) solutionChoice.getValue();
        ObservableList<String> choices = solutionChoice.getItems();
        int index = choices.indexOf(currentChoice);
        if (ke.getCode() == KeyCode.PAGE_UP && index > 0) {
            solutionChoice.setValue(choices.get(index - 1));
        } else if (ke.getCode() == KeyCode.PAGE_DOWN && index < choices.size() - 1) {
            solutionChoice.setValue(choices.get(index + 1));
        }
    }

    public void aggregateSolutions(String file, long fileNumber) {
        Solution aggSoln = new Solution();
        HashMap<Source, Integer> sourcePopularity = new HashMap<>();
        HashMap<Sink, Integer> sinkPopularity = new HashMap<>();
        HashMap<Edge, Integer> edgePopularity = new HashMap<>();
        for (int i = 0; i < fileNumber; i++) {
            Solution soln = data.loadSolution(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file + "/run" + i);

            HashMap<Edge, Double> edgeTransportAmounts = soln.getEdgeTransportAmounts();
            HashMap<Source, Double> sourceCaptureAmounts = soln.getSourceCaptureAmounts();
            HashMap<Sink, Double> sinkStorageAmounts = soln.getSinkStorageAmounts();

            for (Edge e : soln.getOpenedEdges()) {
                if (!edgePopularity.containsKey(e)) {
                    edgePopularity.put(e, 1);
                } else {
                    edgePopularity.put(e, edgePopularity.get(e) + 1);
                }

                aggSoln.addEdgeTransportAmount(e, edgeTransportAmounts.get(e));
            }

            for (Source source : soln.getOpenedSources()) {
                if (!sourcePopularity.containsKey(source)) {
                    sourcePopularity.put(source, 1);
                } else {
                    sourcePopularity.put(source, sourcePopularity.get(source) + 1);
                }

                aggSoln.addSourceCaptureAmount(source, sourceCaptureAmounts.get(source));
            }

            for (Sink sink : soln.getOpenedSinks()) {
                if (!sinkPopularity.containsKey(sink)) {
                    sinkPopularity.put(sink, 1);
                } else {
                    sinkPopularity.put(sink, sinkPopularity.get(sink) + 1);
                }

                aggSoln.addSinkStorageAmount(sink, sinkStorageAmounts.get(sink));
            }
        }

        HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();
        for (Edge e : edgePopularity.keySet()) {
            int[] route = graphEdgeRoutes.get(e);
            Path path = new Path();
            for (int src = 0; src < route.length - 1; src++) {
                int dest = src + 1;
                double[] rawSrc = data.cellLocationToRawXY(route[src]);
                double[] rawDest = data.cellLocationToRawXY(route[dest]);
                MoveTo moveTo = new MoveTo();
                moveTo.setX(rawXtoDisplayX(rawSrc[0]));
                moveTo.setY(rawYtoDisplayY(rawSrc[1]));
                LineTo lineTo = new LineTo();
                lineTo.setX(rawXtoDisplayX(rawDest[0]));
                lineTo.setY(rawYtoDisplayY(rawDest[1]));
                path.getElements().add(moveTo);
                path.getElements().add(lineTo);
            }
            int popularity = edgePopularity.get(e);
            double totalCost = round(data.getGraphEdgeCosts().get(e), 3);
            double constructionCost = round(data.getGraphEdgeConstructionCosts().get(e), 3);
            double rightOfWayCost = round(data.getGraphEdgeRightOfWayCosts().get(e), 3);
            path.setId("EnsembleSolutionNetwork\t" + String.valueOf(popularity) + "\t" + String.valueOf(totalCost) + "\t" + String.valueOf(constructionCost) + "\t" + String.valueOf(rightOfWayCost));
            path.setStroke(Color.DARKOLIVEGREEN);
            path.setStrokeWidth(1 + Math.ceil(100.0 * edgePopularity.get(e) / fileNumber) / 100.0 * (4 / gui.getScale()));
            path.setStrokeLineCap(StrokeLineCap.ROUND);
            ensembleSolutionLayer.getChildren().add(path);
        }

        for (Source source : sourcePopularity.keySet()) {
            double[] rawXYLocation = data.cellLocationToRawXY(source.getCellNum());
            DataCircle c = new DataCircle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 5 + Math.ceil(100.0 * sourcePopularity.get(source) / fileNumber) / 100.0 * (15 / gui.getScale()));
            c.setStroke(Color.INDIANRED);
            c.setFill(Color.INDIANRED);
            Object[] sourceSolution = new Object[2];
            sourceSolution[0] = source;
            sourceSolution[1] = sourcePopularity.get(source);
            c.putData(sourceSolution);
            ensembleSolutionLayer.getChildren().add(c);
        }

        for (Sink sink : sinkPopularity.keySet()) {
            double[] rawXYLocation = data.cellLocationToRawXY(sink.getCellNum());
            DataCircle c = new DataCircle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 5 + Math.ceil(100.0 * sinkPopularity.get(sink) / fileNumber) / 100.0 * (15 / gui.getScale()));
            c.setStroke(Color.DEEPSKYBLUE);
            c.setFill(Color.DEEPSKYBLUE);
            Object[] sinkSolution = new Object[2];
            sinkSolution[0] = sink;
            sinkSolution[1] = sinkPopularity.get(sink);
            c.putData(sinkSolution);
            ensembleSolutionLayer.getChildren().add(c);
        }

        // Write to shapefiles.
        data.makeShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file, aggSoln);
    }

    public void zippedSolutions(String file) {
        String rootPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file + "/";
        try {
            ZipFile zMIP = new ZipFile(rootPath + "mip.zip");
            ZipFile zResult = new ZipFile(rootPath + "result.zip");
            long numMIP = zMIP.size();
            long numResult = zResult.size();
            if (numMIP == numResult) {
                ProgressForm pForm = new ProgressForm(StageStyle.UTILITY, Modality.APPLICATION_MODAL);
                Task<Void> taskMIP = new UnzipFilesTask(rootPath, "mip.zip");
                Task<Void> taskResult = new UnzipFilesTask(rootPath, "result.zip");
                pForm.setProgressTitle("Extracting files...");
                pForm.activateProgressBar(new Task[] {taskMIP, taskResult});
                pForm.getDialogStage().setOnCloseRequest(Event::consume);
                /*final boolean[] mipSucceeded = {false};
                final boolean[] resultSucceeded = {false};
                taskMIP.setOnSucceeded(event -> {
                    mipSucceeded[0] = true;
                    if (resultSucceeded[0]) {
                        aggregateSolutions(file, numMIP);
                        pForm.getDialogStage().close();
                    }
                });*/
                taskResult.setOnSucceeded(event -> {
//                    resultSucceeded[0] = true;
//                    if (mipSucceeded[0]) {
                    try {
                        aggregateSolutions(file, numMIP);
                    } catch (Exception e) {
                        messenger.setText("Number of source/sink in solution does not match!");
//                        JOptionPane.showMessageDialog(null,"Number of source/sink in solution does not match!" , "Solution error", JOptionPane.ERROR_MESSAGE);
                        SimCCSDialogHelper.showExceptionDialog(e, "Solution error", null,  "Number of source/sink in solution does not match!");
                    }
                    pForm.getDialogStage().close();
//                    }
                });
                pForm.getDialogStage().show();
//                Thread threadMIP = new Thread(taskMIP);
//                Thread threadResult = new Thread(taskResult);
//                threadMIP.start();
//                threadResult.start();
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(taskMIP);
                executor.submit(taskResult);
            }
            else {
                messenger.setText("Please check your aggregate solutions!");
                //            System.out.println("Please check your aggregate solutions!");
            }
        } catch (IOException ioe) {
            messenger.setText("Cannot open zip files!");
        }
    }

    public double rawXtoDisplayX(double rawX) {
        double widthRatio = map.getBoundsInParent().getWidth() / data.getWidth();
        // Need to offset to middle of pixel.
        return (rawX - .5) * widthRatio;
    }

    public double rawYtoDisplayY(double rawY) {
        double heightRatio = map.getBoundsInParent().getHeight() / data.getHeight();
        // Need to offset to middle of pixel.
        return (rawY - .5) * heightRatio;
    }

    public int displayXYToVectorized(double x, double y) {
        int rawX = (int) (x / (map.getBoundsInParent().getWidth() / data.getWidth())) + 1;
        int rawY = (int) (y / (map.getBoundsInParent().getHeight() / data.getHeight())) + 1;
        return data.xyToVectorized(rawX, rawY);
    }

    public double[] latLonToDisplayXY(double lat, double lon) {
        double[] rawXY = data.latLonToXY(lat, lon);
        double heightRatio = map.getBoundsInParent().getHeight() / data.getHeight();
        double widthRatio = map.getBoundsInParent().getWidth() / data.getWidth();
        return new double[]{rawXY[0] * widthRatio, rawXY[1] * heightRatio};
    }

    public void addSourceLocationsLayer(Pane layer) {
        sourceLocationsLayer = layer;
    }

    public void addSinkLocationsLayer(Pane layer) {
        sinkLocationsLayer = layer;
    }

    public void addSourceLabelsLayer(Pane layer) {
        sourceLabelsLayer = layer;
    }

    public void addSinkLabelsLayer(Pane layer) {
        sinkLabelsLayer = layer;
    }

    public void addShortestPathsLayer(Pane layer) {
        shortestPathsLayer = layer;
    }

    public void addCandidateNetworkLayer(Pane layer) {
        candidateNetworkLayer = layer;
    }

    public void addSolutionLayer(Pane layer) {
        solutionLayer = layer;
    }

    public void clearSolutionLayer() {
        solutionLayer.getChildren().clear();
    }

    public void addEnsembleSolutionLayer(Pane layer) {
        ensembleSolutionLayer = layer;
    }

    public void addRawDelaunayLayer(Pane layer) {
        rawDelaunayLayer = layer;
    }

    public DataStorer getDataStorer() {
        return data;
    }

    public void addMessenger(TextArea messenger) {
        this.messenger = messenger;
    }

    public TextArea getMessenger() {
        return messenger;
    }

    public DataStorer getData() {
        return data;
    }
}
