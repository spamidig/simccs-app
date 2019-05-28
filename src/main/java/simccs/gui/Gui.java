package simccs.gui;

import java.io.File;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.simccs.desktop.ui.commons.SimCCSDialogHelper;
import simccs.dataStore.Sink;
import simccs.dataStore.Source;


/**
 *
 * @author yaw
 */
public class Gui extends Application {

    private NetworkDisplay displayPane;
    private ChoiceBox scenarioChoice;
    private RadioButton dispRawNetwork;
    private RadioButton dispDelaunayEdges;
    private RadioButton dispCandidateNetwork;
    private RadioButton sourceLabeled;
    private RadioButton sourceVisible;
    private RadioButton sinkLabeled;
    private RadioButton sinkVisible;
    private RadioButton dispCostSurface;
    private ComboBox solutionChoice;
    private ComboBox ensembleSolutionChoice;
    private ComboBox inEnsembleSolutionChoice;

    @Override
    public void start(Stage stage) {
        Scene scene = buildGUI(stage);
        stage.setScene(scene);
        stage.setTitle("SimCCS");
        stage.show();
    }

    public Scene buildGUI(Stage stage) {
        Group group = new Group();

        // Build display pane.
        displayPane = new NetworkDisplay();
        // Offset Network Display to account for controlPane.
        displayPane.setTranslateX(240);
        // Associate scroll/navigation actions.
        SceneGestures sceneGestures = new SceneGestures(displayPane);
        displayPane.addEventFilter(MouseEvent.MOUSE_PRESSED, sceneGestures.getOnMousePressedEventHandler());
        displayPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, sceneGestures.getOnMouseDraggedEventHandler());
        displayPane.addEventFilter(MouseEvent.MOUSE_RELEASED, sceneGestures.getOnMouseReleasedEventHandler());
        displayPane.addEventFilter(MouseEvent.MOUSE_MOVED, sceneGestures.getOnMouseMovedEventHandler());
        displayPane.addEventFilter(ScrollEvent.ANY, sceneGestures.getOnScrollEventHandler());

        // Make background.
        Rectangle background = new Rectangle();
        background.setStroke(Color.WHITE);
        background.setFill(Color.WHITE);
        displayPane.getChildren().add(background);

        // Add base cost surface display.
        PixelatedImageView map = new PixelatedImageView();
        map.setPreserveRatio(true);
        map.setFitWidth(830);
        map.setFitHeight(660);
        map.setSmooth(false);
        displayPane.getChildren().add(map);

        // Action handler.
        ControlActions controlActions = new ControlActions(map, this);
        displayPane.setControlActions(controlActions);

        // Build tab background with messenger.
        AnchorPane messengerPane = new AnchorPane();
        messengerPane.setStyle("-fx-background-color: white; -fx-border-color: lightgrey;");
        messengerPane.setPrefSize(240, 80);
        messengerPane.setLayoutX(0);
        messengerPane.setLayoutY(580);
        TextArea messenger = new TextArea();
        messenger.setEditable(false);
        messenger.setPrefSize(212, 70);
        messenger.setLayoutX(14);
        messenger.setLayoutY(5);
        messengerPane.getChildren().add(messenger);
        controlActions.addMessenger(messenger);

        // Build tab pane and tabs.
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab dataTab = new Tab();
        dataTab.setText("Data");
        tabPane.getTabs().add(dataTab);
        Tab modelTab = new Tab();
        modelTab.setText("Model");
        tabPane.getTabs().add(modelTab);
        Tab resultsTab = new Tab();
        resultsTab.setText("Results");
        tabPane.getTabs().add(resultsTab);

        // Build data pane.
        AnchorPane dataPane = new AnchorPane();
        dataPane.setStyle("-fx-background-color: white; -fx-border-color: lightgrey;");
        dataPane.setPrefSize(240, 580);
        dataTab.setContent(dataPane);

        // Build model pane.
        AnchorPane modelPane = new AnchorPane();
        modelPane.setStyle("-fx-background-color: white; -fx-border-color: lightgrey;");
        modelPane.setPrefSize(240, 580);
        modelTab.setContent(modelPane);

        // Build results pane.
        AnchorPane resultsPane = new AnchorPane();
        resultsPane.setStyle("-fx-background-color: white; -fx-border-color: lightgrey;");
        resultsPane.setPrefSize(240, 580);
        resultsTab.setContent(resultsPane);

        // Populate data pane.
        // Build scenario selection control and add to control pane.
        scenarioChoice = new ChoiceBox();
        scenarioChoice.setPrefSize(170, 27);
        TitledPane scenarioContainer = new TitledPane("Scenario", scenarioChoice);
        scenarioContainer.setCollapsible(false);
        scenarioContainer.setPrefSize(212, 63);
        scenarioContainer.setLayoutX(14);
        scenarioContainer.setLayoutY(73);
        dataPane.getChildren().add(scenarioContainer);
        solutionChoice = new ComboBox();
        ensembleSolutionChoice = new ComboBox();
        inEnsembleSolutionChoice = new ComboBox();
        scenarioChoice.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (selected, oldScenario, newScenario) -> controlActions.selectScenario(newScenario, background, solutionChoice, ensembleSolutionChoice, inEnsembleSolutionChoice));


        // Build dataset selection control and add to control pane.
        Button openDataset = new Button("[Open Dataset]");
        openDataset.setMnemonicParsing(false);
        openDataset.setPrefSize(170, 27);
        openDataset.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Open Dataset");
            File selectedDataset = directoryChooser.showDialog(stage);
            if (selectedDataset != null) {
                openDataset.setText(selectedDataset.getName());
                controlActions.selectDataset(selectedDataset, scenarioChoice);
            }
        });
        TitledPane datasetContainer = new TitledPane("Dataset", openDataset);
        datasetContainer.setCollapsible(false);
        datasetContainer.setPrefSize(212, 63);
        datasetContainer.setLayoutX(14);
        datasetContainer.setLayoutY(5);
        dataPane.getChildren().add(datasetContainer);
        
        //Build network buttons and add to control pane.
        Button rawNetwork = new Button("Shortest Paths Network");
        rawNetwork.setLayoutX(19);
        rawNetwork.setLayoutY(4);
        rawNetwork.setOnAction(e -> controlActions.generateShortestPathsNetwork());

        Button candidateNetwork = new Button("Candidate Network");
        candidateNetwork.setLayoutX(19);
        candidateNetwork.setLayoutY(35);
        candidateNetwork.setOnAction(e -> controlActions.generateCandidateGraph());

        Button reprocessAll = new Button("Reprocess All");
        reprocessAll.setLayoutX(19);
        reprocessAll.setLayoutY(66);
        reprocessAll.setOnAction(e -> {
            if (controlActions.getData() != null) {
                boolean disp = dispCostSurface.isSelected();
                boolean source = sourceVisible.isSelected();
                boolean sink = sinkVisible.isSelected();
                boolean cand = dispCandidateNetwork.isSelected();
                fullReset();
                controlActions.generateShortestPathsNetwork(); //FixedFIXME: need to understand why sometimes it requires being called twice; Aha, it is related the modified edge cost
                controlActions.getData().generateDelaunayPairs();
                controlActions.generateCandidateGraph();
                controlActions.saveSourceSinkState();
                dispCostSurface.setSelected(disp);
                sourceVisible.setSelected(source);
                sinkVisible.setSelected(sink);
                dispCandidateNetwork.setSelected(cand);
            }
        });

        Button enableAll = new Button("Enable All");
        enableAll.setLayoutX(19);
        enableAll.setLayoutY(97);
        enableAll.setOnAction(e -> {
            if (controlActions.getData() != null) {
                for (Sink s : controlActions.getData().getSinks()) {
                    s.enable();
                }
                for (Source s : controlActions.getData().getSources()) {
                    s.enable();
                }
                reprocessAll.fire();
            }
        });

        AnchorPane buttonPane = new AnchorPane();
        buttonPane.setPrefSize(210, 30);
        buttonPane.setMinSize(0, 0);
        buttonPane.getChildren().addAll(rawNetwork, candidateNetwork, reprocessAll, enableAll);
        TitledPane networkContainer = new TitledPane("Network Generation", buttonPane);
        networkContainer.setCollapsible(false);
        networkContainer.setPrefSize(212, 157);
        networkContainer.setLayoutX(14);
        networkContainer.setLayoutY(141);
        dataPane.getChildren().add(networkContainer);

        //Build display selection legend and add to control pane.
        AnchorPane selectionPane = new AnchorPane();
        selectionPane.setPrefSize(226, 237);
        selectionPane.setMinSize(0, 0);

        dispRawNetwork = new RadioButton("Shortest Paths Network");
        dispRawNetwork.setLayoutX(4);
        dispRawNetwork.setLayoutY(83);
        selectionPane.getChildren().add(dispRawNetwork);
        Pane rawPathsLayer = new Pane();
        sceneGestures.addEntityToResize(rawPathsLayer);
        displayPane.getChildren().add(rawPathsLayer);
        controlActions.addShortestPathsLayer(rawPathsLayer);
        dispRawNetwork.selectedProperty().addListener((selected, oldVal, show) -> controlActions.toggleShortestPathsDisplay(show));

        dispDelaunayEdges = new RadioButton("Raw Delaunay Edges");
        dispDelaunayEdges.setLayoutX(4);
        dispDelaunayEdges.setLayoutY(106);
        selectionPane.getChildren().add(dispDelaunayEdges);
        Pane rawDelaunayLayer = new Pane();
        sceneGestures.addEntityToResize(rawDelaunayLayer);
        displayPane.getChildren().add(rawDelaunayLayer);
        controlActions.addRawDelaunayLayer(rawDelaunayLayer);
        dispDelaunayEdges.selectedProperty().addListener((selected, oldVal, show) -> controlActions.toggleRawDelaunayDisplay(show));

        dispCandidateNetwork = new RadioButton("Candidate Network");
        dispCandidateNetwork.setLayoutX(4);
        dispCandidateNetwork.setLayoutY(129);
        selectionPane.getChildren().add(dispCandidateNetwork);
        Pane candidateNetworkLayer = new Pane();
        sceneGestures.addEntityToResize(candidateNetworkLayer, "CandidateNetwork");
        displayPane.getChildren().add(candidateNetworkLayer);
        controlActions.addCandidateNetworkLayer(candidateNetworkLayer);
        dispCandidateNetwork.selectedProperty().addListener((selected, oldVal, show) -> controlActions.toggleCandidateNetworkDisplay(show));

        Label sourceLabel = new Label("Sources:");
        sourceLabel.setLayoutX(2);
        sourceLabel.setLayoutY(5);
        selectionPane.getChildren().add(sourceLabel);

        // Toggle source locations display button.
        sourceLabeled = new RadioButton("Label");  // Need reference before definition.
        sourceVisible = new RadioButton("Visible");
        sourceVisible.setLayoutX(62);
        sourceVisible.setLayoutY(4);
        selectionPane.getChildren().add(sourceVisible);
        Pane sourcesLayer = new Pane();
        displayPane.getChildren().add(sourcesLayer);
        controlActions.addSourceLocationsLayer(sourcesLayer);
        sceneGestures.addEntityToResize(sourcesLayer, "Sources");
        sourceVisible.selectedProperty().addListener((selected, oldVal, show) -> {
            if (!show) {
                sourceLabeled.setSelected(false);
            }
            controlActions.toggleSourceDisplay(show);
        });

        // Toggle source labels display button.
        sourceLabeled.setLayoutX(131);
        sourceLabeled.setLayoutY(4);
        selectionPane.getChildren().add(sourceLabeled);
        Pane sourceLabelsLayer = new Pane();
        displayPane.getChildren().add(sourceLabelsLayer);
        controlActions.addSourceLabelsLayer(sourceLabelsLayer);
        sceneGestures.addEntityToResize(sourceLabelsLayer);
        sourceLabeled.selectedProperty().addListener((selected, oldVal, show) -> {
            if (!sourceVisible.isSelected()) {
                show = false;
                sourceLabeled.setSelected(false);
            }
            controlActions.toggleSourceLabels(show);
        });

        Label sinkLabel = new Label("Sinks:");
        sinkLabel.setLayoutX(19);
        sinkLabel.setLayoutY(30);
        selectionPane.getChildren().add(sinkLabel);

        // Toggle sink locations display button.
        sinkLabeled = new RadioButton("Label");  // Need reference before definition.
        sinkVisible = new RadioButton("Visible");
        sinkVisible.setLayoutX(62);
        sinkVisible.setLayoutY(29);
        selectionPane.getChildren().add(sinkVisible);
        Pane sinksLayer = new Pane();
        displayPane.getChildren().add(sinksLayer);
        controlActions.addSinkLocationsLayer(sinksLayer);
        sceneGestures.addEntityToResize(sinksLayer, "Sinks");
        sinkVisible.selectedProperty().addListener((selected, oldVal, show) -> {
            if (!show) {
                sinkLabeled.setSelected(false);
            }
            controlActions.toggleSinkDisplay(show);
        });

        // Toggle sink labels.
        sinkLabeled.setLayoutX(131);
        sinkLabeled.setLayoutY(29);
        selectionPane.getChildren().add(sinkLabeled);
        Pane sinkLabelsLayer = new Pane();
        displayPane.getChildren().add(sinkLabelsLayer);
        controlActions.addSinkLabelsLayer(sinkLabelsLayer);
        sceneGestures.addEntityToResize(sinkLabelsLayer);
        sinkLabeled.selectedProperty().addListener((selected, oldVal, show) -> {
            if (!sinkVisible.isSelected()) {
                show = false;
                sinkLabeled.setSelected(false);
            }
            controlActions.toggleSinkLabels(show);
        });

        // Toggle cost surface button.
        dispCostSurface = new RadioButton("Cost Surface");
        dispCostSurface.setLayoutX(4);
        dispCostSurface.setLayoutY(60);
        selectionPane.getChildren().add(dispCostSurface);
        dispCostSurface.selectedProperty().addListener((selected, oldVal, show) -> controlActions.toggleCostSurface(show, background));

        TitledPane selectionContainer = new TitledPane("Legend", selectionPane);
        selectionContainer.setCollapsible(false);
        selectionContainer.setPrefSize(212, 180);
        selectionContainer.setLayoutX(14);
        selectionContainer.setLayoutY(303);
        dataPane.getChildren().add(selectionContainer);

        // Solution area
        AnchorPane formulationPane = new AnchorPane();
        formulationPane.setPrefSize(226, 237);
        formulationPane.setMinSize(0, 0);

        Label crfLabel = new Label("Capital Recovery Rate");
        crfLabel.setLayoutX(4);
        crfLabel.setLayoutY(8);
        formulationPane.getChildren().add(crfLabel);
        TextField crfValue = new TextField(".1");
        crfValue.setEditable(true);
        crfValue.setPrefColumnCount(2);
        crfValue.setLayoutX(163);
        crfValue.setLayoutY(4);
        formulationPane.getChildren().add(crfValue);

        Label yearLabel = new Label("Number of Years");
        yearLabel.setLayoutX(4);
        yearLabel.setLayoutY(38);
        formulationPane.getChildren().add(yearLabel);
        TextField yearValue = new TextField("30");
        yearValue.setEditable(true);
        yearValue.setPrefColumnCount(2);
        yearValue.setLayoutX(163);
        yearValue.setLayoutY(34);
        formulationPane.getChildren().add(yearValue);

        Label capLabel = new Label("Capture Target (MT/y)");
        capLabel.setLayoutX(4);
        capLabel.setLayoutY(68);
        formulationPane.getChildren().add(capLabel);
        TextField capValue = new TextField("15");
        capValue.setEditable(true);
        capValue.setPrefColumnCount(2);
        capValue.setLayoutX(163);
        capValue.setLayoutY(64);
        formulationPane.getChildren().add(capValue);

        Button generateSolutionFile = new Button("Generate MPS File");
        generateSolutionFile.setLayoutX(4);
        generateSolutionFile.setLayoutY(94);
        formulationPane.getChildren().add(generateSolutionFile);
        generateSolutionFile.setOnAction(e -> controlActions.generateMPSFile(crfValue.getText(), yearValue.getText(), capValue.getText()));

        Button gatewaySolve = new Button("Send to Gateway");
        gatewaySolve.setLayoutX(4);
        gatewaySolve.setLayoutY(124);
        formulationPane.getChildren().add(gatewaySolve);
        gatewaySolve.setOnAction(e -> controlActions.sendSingleJobToGateway());

        // Populate model pane.
        TitledPane modelContainer = new TitledPane("Simple Model", formulationPane);
        modelContainer.setCollapsible(false);
        modelContainer.setPrefSize(212, 182);
        modelContainer.setLayoutX(14);
        modelContainer.setLayoutY(5);
        modelPane.getChildren().add(modelContainer);

        // Solution pane.
        AnchorPane solutionPane = new AnchorPane();
        solutionPane.setPrefSize(212, 220);
        solutionPane.setMinSize(0, 0);

        Label simLabel = new Label("Number of Simulations");
        simLabel.setLayoutX(4);
        simLabel.setLayoutY(8);
        solutionPane.getChildren().add(simLabel);
        TextField simValue = new TextField("100");
        simValue.setEditable(true);
        simValue.setPrefColumnCount(3);
        simValue.setPrefWidth(51);
        simValue.setLayoutX(154);
        simValue.setLayoutY(4);
        solutionPane.getChildren().add(simValue);

        Button loadSourceUncertainty = new Button("Load Source Uncertainty");
        loadSourceUncertainty.setLayoutX(4);
        loadSourceUncertainty.setLayoutY(34);
        solutionPane.getChildren().add(loadSourceUncertainty);
        loadSourceUncertainty.setOnAction(e -> {
            if (scenarioChoice.getValue() != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialDirectory(new File(controlActions.getCurrentScenarioDirectory() + "/Sources/"));
                fileChooser.setTitle("Select Source Uncertainty");
                File selectedDataset = fileChooser.showOpenDialog(stage);
                if (selectedDataset != null) {
                    controlActions.loadSourceUncertainty(selectedDataset.getPath());
                }
            }
        });

        Button loadSinkUncertainty = new Button("Load Sink Uncertainty");
        loadSinkUncertainty.setLayoutX(4);
        loadSinkUncertainty.setLayoutY(64);
        solutionPane.getChildren().add(loadSinkUncertainty);
        loadSinkUncertainty.setOnAction(e -> {
            if (scenarioChoice.getValue() != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialDirectory(new File(controlActions.getCurrentScenarioDirectory() + "/Sinks/"));
                fileChooser.setTitle("Select Sink Uncertainty");
                File selectedDataset = fileChooser.showOpenDialog(stage);
                if (selectedDataset != null) {
                    controlActions.loadSinkUncertainty(selectedDataset.getPath());
                }
            }
        });

        Button generateSolutionFiles = new Button("Generate File Ensemble");
        generateSolutionFiles.setLayoutX(4);
        generateSolutionFiles.setLayoutY(94);
        solutionPane.getChildren().add(generateSolutionFiles);
        generateSolutionFiles.setOnAction(e -> controlActions.generateZippedMPSFile(crfValue.getText(), yearValue.getText(), capValue.getText(), simValue.getText()));

        Button gatewayEnsembleSolve = new Button("Send Ensemble to Gateway");
        gatewayEnsembleSolve.setLayoutX(4);
        gatewayEnsembleSolve.setLayoutY(124);
        solutionPane.getChildren().add(gatewayEnsembleSolve);
        gatewayEnsembleSolve.setOnAction(e -> controlActions.sendEnsembleJobToGateway());

        // Populate solution method pane.
        TitledPane solutionContainer = new TitledPane("Uncertainty Model", solutionPane);
        solutionContainer.setCollapsible(false);
        solutionContainer.setPrefSize(212, 185);
        solutionContainer.setLayoutX(14);
        solutionContainer.setLayoutY(192);
        modelPane.getChildren().add(solutionContainer);

        // Populate results pane.
        // Build solution selection control.
        solutionChoice.setPrefSize(170, 27);
        TitledPane resultsContainer = new TitledPane("Load Solution", solutionChoice);
        resultsContainer.setCollapsible(false);
        resultsContainer.setPrefSize(212, 63);
        resultsContainer.setLayoutX(14);
        resultsContainer.setLayoutY(5);
        resultsPane.getChildren().add(resultsContainer);

        ensembleSolutionChoice.setPrefSize(170, 27);
        TitledPane ensembleResultsContainer = new TitledPane("Load Solution Ensemble ", ensembleSolutionChoice);
        ensembleResultsContainer.setCollapsible(false);
        ensembleResultsContainer.setPrefSize(212, 63);
        ensembleResultsContainer.setLayoutX(14);
        ensembleResultsContainer.setLayoutY(73);
        resultsPane.getChildren().add(ensembleResultsContainer);

        inEnsembleSolutionChoice.setPrefSize(170, 27);
        TitledPane inEnsembleResultsContainer = new TitledPane("Solution within Ensemble", inEnsembleSolutionChoice);
        inEnsembleResultsContainer.setCollapsible(false);
        inEnsembleResultsContainer.setPrefSize(212, 63);
        inEnsembleResultsContainer.setLayoutX(14);
        inEnsembleResultsContainer.setLayoutY(141);
        resultsPane.getChildren().add(inEnsembleResultsContainer);

        // Solution labels.
        Label sources = new Label("Sources:");
        sources.setLayoutX(69);
        sources.setLayoutY(206 );
        Label sourcesValue = new Label("-");
        sourcesValue.setLayoutX(135);
        sourcesValue.setLayoutY(206 );
        resultsPane.getChildren().addAll(sources, sourcesValue);

        Label sinks = new Label("Sinks:");
        sinks.setLayoutX(86);
        sinks.setLayoutY(226);
        Label sinksValue = new Label("-");
        sinksValue.setLayoutX(135);
        sinksValue.setLayoutY(226);
        resultsPane.getChildren().addAll(sinks, sinksValue);

        Label stored = new Label("CO2 Stored:");
        stored.setLayoutX(47);
        stored.setLayoutY(246);
        Label storedValue = new Label("-");
        storedValue.setLayoutX(135);
        storedValue.setLayoutY(246);
        resultsPane.getChildren().addAll(stored, storedValue);

        Label edges = new Label("Edges:");
        edges.setLayoutX(81);
        edges.setLayoutY(266);
        Label edgesValue = new Label("-");
        edgesValue.setLayoutX(135);
        edgesValue.setLayoutY(266);
        resultsPane.getChildren().addAll(edges, edgesValue);

        Label length = new Label("Project Length:");
        length.setLayoutX(30);
        length.setLayoutY(286);
        Label lengthValue = new Label("-");
        lengthValue.setLayoutX(135);
        lengthValue.setLayoutY(286);
        resultsPane.getChildren().addAll(length, lengthValue);

        Label total = new Label("Total Cost\n   ($m/yr)");
        total.setLayoutX(65);
        total.setLayoutY(326);
        Label unit = new Label("Unit Cost\n ($/tCO2)");
        unit.setLayoutX(150);
        unit.setLayoutY(326);
        resultsPane.getChildren().addAll(total, unit);

        Label cap = new Label("Capture:");
        cap.setLayoutX(4);
        cap.setLayoutY(366);
        Label capT = new Label("-");
        capT.setLayoutX(75);
        capT.setLayoutY(366);
        Label capU = new Label("-");
        capU.setLayoutX(160);
        capU.setLayoutY(366);
        resultsPane.getChildren().addAll(cap, capT, capU);

        Label trans = new Label("Transport:");
        trans.setLayoutX(4);
        trans.setLayoutY(386);
        Label transT = new Label("-");
        transT.setLayoutX(75);
        transT.setLayoutY(386);
        Label transU = new Label("-");
        transU.setLayoutX(160);
        transU.setLayoutY(386);
        resultsPane.getChildren().addAll(trans, transT, transU);

        Label stor = new Label("Storage:");
        stor.setLayoutX(4);
        stor.setLayoutY(406);
        Label storT = new Label("-");
        storT.setLayoutX(75);
        storT.setLayoutY(406);
        Label storU = new Label("-");
        storU.setLayoutX(160);
        storU.setLayoutY(406);
        resultsPane.getChildren().addAll(stor, storT, storU);

        Label tot = new Label("Total:");
        tot.setLayoutX(4);
        tot.setLayoutY(426);
        Label totT = new Label("-");
        totT.setLayoutX(75);
        totT.setLayoutY(426);
        Label totU = new Label("-");
        totU.setLayoutX(160);
        totU.setLayoutY(426);
        resultsPane.getChildren().addAll(tot, totT, totU);

        Label[] solutionValues = new Label[]{sourcesValue, sinksValue, storedValue, edgesValue, lengthValue, capT, capU, transT, transU, storT, storU, totT, totU};

        // Solution selection action.
        solutionChoice.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (selected, oldSolution, newSolution) -> {
            if (oldSolution != null && newSolution != null && !oldSolution.equals(newSolution))
                try {
                    controlActions.selectSolution(newSolution, solutionValues);
                } catch (Exception e) {
                    SimCCSDialogHelper.showExceptionDialog(e, "Solution error", null,  "Number of source/sink in solution does not match!");
                    messenger.setText("Number of source/sink in solution does not match!");
//                    solutionChoice.setValue("None");
                }
        });
        solutionChoice.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                controlActions.initializeSolutionSelection(solutionChoice);
            }
        });
        solutionChoice.setOnScroll(s -> controlActions.scrollSolutions(s, solutionChoice));
        solutionChoice.addEventFilter(KeyEvent.KEY_PRESSED, ke -> controlActions.pageThroughSolutions(ke, solutionChoice));


        ensembleSolutionChoice.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (selected, oldSolution, newSolution) -> {
            if (oldSolution != null && newSolution != null && !oldSolution.equals(newSolution))
                controlActions.selectEnsembleSolution(newSolution, inEnsembleSolutionChoice, solutionValues);
        });
        ensembleSolutionChoice.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                controlActions.initializeEnsembleSolutionSelection(ensembleSolutionChoice, inEnsembleSolutionChoice);
            }
        });
        ensembleSolutionChoice.addEventFilter(KeyEvent.KEY_RELEASED, ke -> controlActions.pageThroughSolutions(ke, ensembleSolutionChoice));


        inEnsembleSolutionChoice.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (selected, oldSolution, newSolution) -> {
            if (ensembleSolutionChoice.getValue() != null && !ensembleSolutionChoice.getValue().equals("None")
                    && newSolution != null && !newSolution.equals("None")
                    && oldSolution != null && newSolution != null && !oldSolution.equals(newSolution))
                controlActions.selectSolution(ensembleSolutionChoice.getValue().toString() + "/" + newSolution, solutionValues);
            else if (newSolution != null && newSolution.equals("None"))
                controlActions.clearSolutionLayer();
        });
        inEnsembleSolutionChoice.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing && ensembleSolutionChoice.getValue() != null) {
                controlActions.initializeInEnsembleSolutionSelection(ensembleSolutionChoice.getValue().toString(), inEnsembleSolutionChoice);
            }
        });
        inEnsembleSolutionChoice.setOnScroll(s -> controlActions.scrollSolutions(s, inEnsembleSolutionChoice));
        inEnsembleSolutionChoice.addEventFilter(KeyEvent.KEY_PRESSED, ke -> controlActions.pageThroughSolutions(ke, inEnsembleSolutionChoice));

        Pane ensembleSolutionLayer = new Pane();
        displayPane.getChildren().add(ensembleSolutionLayer);
        controlActions.addEnsembleSolutionLayer(ensembleSolutionLayer);
        sceneGestures.addEntityToResize(ensembleSolutionLayer, "EnsembleSolution");

        Pane solutionLayer = new Pane();
        displayPane.getChildren().add(solutionLayer);
        controlActions.addSolutionLayer(solutionLayer);
        sceneGestures.addEntityToResize(solutionLayer, "Solution");

        // Add everything to group and display.
        group.getChildren().addAll(displayPane, tabPane, messengerPane);
        return new Scene(group, 1070, 660);
    }

    public void displayCostSurface() {
        dispCostSurface.setSelected(true);
    }

    public void fullReset() {
        //scenarioChoice;
        dispRawNetwork.setSelected(false);
        dispDelaunayEdges.setSelected(false);
        dispCandidateNetwork.setSelected(false);
        sourceLabeled.setSelected(false);
        sourceVisible.setSelected(false);
        sinkLabeled.setSelected(false);
        sinkVisible.setSelected(false);
        dispCostSurface.setSelected(false);
        solutionChoice.setValue("None");
        ensembleSolutionChoice.setValue("None");
        inEnsembleSolutionChoice.setValue("None");
    }

    public void softReset() {
        dispRawNetwork.setSelected(false);
        dispDelaunayEdges.setSelected(false);
        dispCandidateNetwork.setSelected(false);
        sourceLabeled.setSelected(false);
        sourceVisible.setSelected(false);
        sinkLabeled.setSelected(false);
        sinkVisible.setSelected(false);
        dispCostSurface.setSelected(false);
    }

    public double getScale() {
        return displayPane.getScale();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
