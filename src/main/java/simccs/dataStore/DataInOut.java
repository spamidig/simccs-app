package simccs.dataStore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static simccs.utilities.Utilities.*;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.dataAccess.shape.EsriPolyline;
import com.bbn.openmap.dataAccess.shape.EsriPolylineList;
import com.bbn.openmap.dataAccess.shape.EsriShapeExport;
import com.bbn.openmap.dataAccess.shape.DbfTableModel;
import com.bbn.openmap.dataAccess.shape.EsriPoint;
import com.bbn.openmap.dataAccess.shape.EsriPointList;

/**
 *
 * @author yaw
 */
public class DataInOut {

    private static String basePath;
    private static String dataset;
    private static String scenario;
    private static DataStorer data;

    public static void loadData(String basePath, String dataset, String scenario, DataStorer data) {
        DataInOut.basePath = basePath;
        DataInOut.dataset = dataset;
        DataInOut.scenario = scenario;
        DataInOut.data = data;

        System.out.println("Loading Geography...");
        loadGeography();
        System.out.println("Loading Source Data...");
        loadSources();
        System.out.println("Loading Sink Data...");
        loadSinks();
        System.out.println("Loading Transport Data...");
        loadTransport();
        System.out.print("Loading Shortest Paths...");
        loadShortestPaths();
        System.out.print("Loading Delaunay Pairs...");
        loadDelaunayPairs();
        System.out.print("Loading Candidate Graph...");
        loadCandidateGraph();
        System.out.println("Data Loaded.");
    }

    private static void loadGeography() {
        String path = basePath + "/" + dataset + "/BaseData/CostNetwork/Construction Costs.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine();
            br.readLine();

            // Read dimensions.
            String line = br.readLine();
            String[] elements = line.split("\\s+");
            data.setWidth(Integer.parseInt(elements[1]));

            line = br.readLine();
            elements = line.split("\\s+");
            data.setHeight(Integer.parseInt(elements[1]));

            // Read conversions.
            line = br.readLine();
            elements = line.split("\\s+");
            data.setLowerLeftX(Double.parseDouble(elements[1]));

            line = br.readLine();
            elements = line.split("\\s+");
            data.setLowerLeftY(Double.parseDouble(elements[1]));

            line = br.readLine();
            elements = line.split("\\s+");
            data.setCellSize(Double.valueOf(elements[1]));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void loadCosts() {
        String path = basePath + "/" + dataset + "/BaseData/CostNetwork/Construction Costs.txt";
        double[][] adjacencyCosts = new double[0][0];
        double[][] rightOfWayCosts = new double[0][0];
        double[][] constructionCosts = new double[0][0];
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            for (int i = 0; i < 8; i++) {
                br.readLine();
            }

            // Create cost array.
            adjacencyCosts = new double[data.getWidth() * data.getHeight() + 1][8];
            rightOfWayCosts = new double[data.getWidth() * data.getHeight() + 1][8];
            constructionCosts = new double[data.getWidth() * data.getHeight() + 1][8];
            for (int i = 0; i < adjacencyCosts.length; i++) {
                for (int j = 0; j < adjacencyCosts[j].length; j++) {
                    adjacencyCosts[i][j] = Double.MAX_VALUE;
                    rightOfWayCosts[i][j] = Double.MAX_VALUE;
                    constructionCosts[i][j] = Double.MAX_VALUE;
                }
            }

            // Load construction costs.
            String line = br.readLine();
            while (line != null) {
                String costLine = br.readLine();
                String[] costs = costLine.split("\\s+");
                String[] cells = line.split("\\s+");
                int centerCell = Integer.parseInt(cells[0]);
                for (int i = 1; i < costs.length; i++) {
                    adjacencyCosts[centerCell][data.getNeighborNum(centerCell, Integer.parseInt(cells[i]))] = Double.parseDouble(costs[i]);
                    constructionCosts[centerCell][data.getNeighborNum(centerCell, Integer.parseInt(cells[i]))] = Double.parseDouble(costs[i]);
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        // Load right of way costs.  
        path = basePath + "/" + dataset + "/BaseData/CostNetwork/RightOfWay Costs.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            for (int i = 0; i < 8; i++) {
                br.readLine();
            }

            String line = br.readLine();
            while (line != null) {
                String costLine = br.readLine();
                String[] costs = costLine.split("\\s+");
                String[] cells = line.split("\\s+");
                int centerCell = Integer.parseInt(cells[0]);
                for (int i = 1; i < costs.length; i++) {
                    adjacencyCosts[centerCell][data.getNeighborNum(centerCell, Integer.parseInt(cells[i]))] += Double.parseDouble(costs[i]);
                    rightOfWayCosts[centerCell][data.getNeighborNum(centerCell, Integer.parseInt(cells[i]))] = Double.parseDouble(costs[i]);
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        data.setAdjacencyCosts(adjacencyCosts);
        data.setConstructionCosts(constructionCosts);
        data.setRightOfWayCosts(rightOfWayCosts);
    }

    private static void loadSources() {
        String sourcePath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Sources/Sources.txt";
        String sourceStatePath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Sources/SourceStates.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(sourcePath))) {
            br.readLine();
            String line = br.readLine();
            ArrayList<Source> sources = new ArrayList<>();
            while (line != null) {
                String[] elements = line.split("\\s+");
                Source source = new Source(data);
                source.setLabel(elements[0]);
                source.setCellNum(data.latLonToCell(Double.parseDouble(elements[7]), Double.parseDouble(elements[6])));
                source.setOpeningCost(Double.parseDouble(elements[1]));
                source.setOMCost(Double.parseDouble(elements[2]));
                source.setCaptureCost(Double.parseDouble(elements[3]));
                source.setProductionRate(Double.parseDouble(elements[4]));
                source.enable();
                sources.add(source);
                line = br.readLine();
            }
            try (BufferedReader brSt = new BufferedReader(new FileReader(sourceStatePath))) {
                line = brSt.readLine();
                while (line != null) {
                    String[] elements = line.split("\\s+");
                    if (elements[1].equals("0")) {
                        for (Source s : sources) {
                            if (s.getLabel().equals(elements[0]))
                                s.disable();
                        }
                    }
                    line = brSt.readLine();
                }
            } catch (FileNotFoundException ignored) {
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
            data.setSources(sources.toArray(new Source[0]));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void loadSinks() {
        String sinkPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Sinks/Sinks.txt";
        String sinkStatePath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Sinks/SinkStates.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(sinkPath))) {
            br.readLine();
            String line = br.readLine();
            List<Sink> sinks = new ArrayList<>();
            while (line != null) {
                String[] elements = line.split("\\s+");
                Sink sink = new Sink(data);
                sink.setLabel(elements[0]);
                sink.setCellNum(data.latLonToCell(Double.parseDouble(elements[11]), Double.parseDouble(elements[10])));
                sink.setOpeningCost(Double.parseDouble(elements[3]));
                sink.setOMCost(Double.parseDouble(elements[4]));
                sink.setWellOpeningCost(Double.parseDouble(elements[6]));
                sink.setWellOMCost(Double.parseDouble(elements[7]));
                sink.setInjectionCost(Double.parseDouble(elements[8]));
                sink.setWellCapacity(Double.parseDouble(elements[5]));
                sink.setCapacity(Double.parseDouble(elements[2]));
                sinks.add(sink);
                line = br.readLine();
            }
            try (BufferedReader brSt = new BufferedReader(new FileReader(sinkStatePath))) {
                line = brSt.readLine();
                while (line != null) {
                    String[] elements = line.split("\\s+");
                    if (elements[1].equals("0")) {
                        for (Sink s : sinks) {
                            if (s.getLabel().equals(elements[0]))
                                s.disable();
                        }
                    }
                    line = brSt.readLine();
                }
            } catch (FileNotFoundException ignored) {
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
            data.setSinks(sinks.toArray(new Sink[0]));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void loadSinkUncertainties(String sinkUncertaintyPath, DataStorer data) {
        DataInOut.data = data;
        try (BufferedReader br = new BufferedReader(new FileReader(sinkUncertaintyPath))) {
            br.readLine();
            String line = br.readLine();
            List<SinkUncertainty> sinksUncertainty = new ArrayList<>();
            List<Double> al = new ArrayList<>();
            while (line != null) {
                String[] elements = line.split("\\s+");
                SinkUncertainty sinkUncertainty = new SinkUncertainty(data);
                sinkUncertainty.setLabel(elements[0]);
                sinkUncertainty.setDistribution(elements[1]);
                if (Objects.equals(elements[1], "Normal")
                        || Objects.equals(elements[1], "Cauchy")) {
                    al.add(Double.parseDouble(elements[2]));
                    al.add(Double.parseDouble(elements[3]));
                    sinkUncertainty.setCapacity(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[4]));
                    al.add(Double.parseDouble(elements[5]));
                    sinkUncertainty.setOpeningCost(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[6]));
                    al.add(Double.parseDouble(elements[7]));
                    sinkUncertainty.setOMCost(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[8]));
                    al.add(Double.parseDouble(elements[9]));
                    sinkUncertainty.setWellCapacity(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[10]));
                    al.add(Double.parseDouble(elements[11]));
                    sinkUncertainty.setWellOpeningCost(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[12]));
                    al.add(Double.parseDouble(elements[13]));
                    sinkUncertainty.setWellOMCost(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[14]));
                    al.add(Double.parseDouble(elements[15]));
                    sinkUncertainty.setInjectionCost(al.toArray(new Double[0]));
                    al.clear();
                }
                sinksUncertainty.add(sinkUncertainty);
                line = br.readLine();
            }
            data.setSinkUncertainties(sinksUncertainty.toArray(new SinkUncertainty[0]));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void loadSourceUncertainties(String sourceUncertaintyPath, DataStorer data) {
        DataInOut.data = data;
        try (BufferedReader br = new BufferedReader(new FileReader(sourceUncertaintyPath))) {
            br.readLine();
            String line = br.readLine();
            List<SourceUncertainty> sourcesUncertainty = new ArrayList<>();
            List<Double> al = new ArrayList<>();
            while (line != null) {
                String[] elements = line.split("\\s+");
                SourceUncertainty sourceUncertainty = new SourceUncertainty(data);
                sourceUncertainty.setLabel(elements[0]);
                sourceUncertainty.setDistribution(elements[1]);
                if (Objects.equals(elements[1], "Normal")
                        || Objects.equals(elements[1], "Cauchy")) {
                    al.add(Double.parseDouble(elements[2]));
                    al.add(Double.parseDouble(elements[3]));
                    sourceUncertainty.setOpeningCost(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[4]));
                    al.add(Double.parseDouble(elements[5]));
                    sourceUncertainty.setOMCost(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[6]));
                    al.add(Double.parseDouble(elements[7]));
                    sourceUncertainty.setCaptureCost(al.toArray(new Double[0]));
                    al.clear();
                    al.add(Double.parseDouble(elements[8]));
                    al.add(Double.parseDouble(elements[9]));
                    sourceUncertainty.setProductionRate(al.toArray(new Double[0]));
                    al.clear();
                }
                sourcesUncertainty.add(sourceUncertainty);
                line = br.readLine();
            }
            data.setSourceUncertainties(sourcesUncertainty.toArray(new SourceUncertainty[0]));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void loadTransport() {
        String tranportPath = basePath + "/" + dataset + "/Scenarios/" ++ "/Transport/Linear.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(tranportPath))) {
            br.readLine();
            String line = br.readLine();
            ArrayList<LinearComponent> linearComponents = new ArrayList<>();
            while (line != null) {
                String[] elements = line.split("\\s+");
                LinearComponent linearComponent = new LinearComponent(data);
                linearComponent.setConAlpha(Double.parseDouble(elements[1]));
                linearComponent.setConBeta(Double.parseDouble(elements[2]));
                linearComponent.setRowAlpha(Double.parseDouble(elements[3]));
                linearComponent.setRowBeta(Double.parseDouble(elements[4]));
                linearComponents.add(linearComponent);
                line = br.readLine();
            }
            data.setLinearComponents(linearComponents.toArray(new LinearComponent[0]));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void loadShortestPaths() {
        String rawPathsPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/RawPaths/RawPaths.txt";

        if (new File(rawPathsPath).exists()) {
            // Load from file.
            try (BufferedReader br = new BufferedReader(new FileReader(rawPathsPath))) {
                br.readLine();
                String line = br.readLine();
                ArrayList<int[]> rawPaths = new ArrayList<>();
                ArrayList<Double> rawPathCosts = new ArrayList<>();
                while (line != null) {
                    // Read path cost
                    String[] elements = line.split("\\s+");
                    rawPathCosts.add(Double.parseDouble(elements[2]));

                    // Read path
                    line = br.readLine();
                    elements = line.split("\\s+");
                    int[] path = new int[Integer.parseInt(elements[0])];
                    for (int nodeNum = 1; nodeNum < elements.length; nodeNum++) {
                        path[nodeNum - 1] = Integer.parseInt(elements[nodeNum]);
                    }
                    rawPaths.add(path);

                    // Prepare for next entry
                    line = br.readLine();
                }
                data.setShortestPaths(rawPaths.toArray(new int[0][0]));
                data.setShortestPathCosts(convertDoubleArray(rawPathCosts.toArray(new Double[0])));
                System.out.println();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("Not Yet Generated.");
        }
    }

    private static void loadCandidateGraph() {
        // Check if file exists
        String candidateGraphPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/CandidateNetwork/CandidateNetwork.txt";
        if (new File(candidateGraphPath).exists()) {
            // Load from file.
            try (BufferedReader br = new BufferedReader(new FileReader(candidateGraphPath))) {
                String line = br.readLine();
                // Determine data version
                int routeStarting = 3;
                if (!line.startsWith("Vertex1")) {
                    routeStarting = 4;
                    br.readLine();
                    br.readLine();
                    br.readLine();
                }
                line = br.readLine();

                HashSet<Integer> graphVertices = new HashSet<>();
                HashMap<Edge, Double> graphEdgeCosts = new HashMap<>();
                HashMap<Edge, int[]> graphEdgeRoutes = new HashMap<>();
                while (line != null) {
                    String[] elements = line.split("\\s+");
                    int v1 = Integer.parseInt(elements[0]);
                    int v2 = Integer.parseInt(elements[1]);
                    Edge edge = new Edge(v1, v2);
                    graphVertices.add(v1);
                    graphVertices.add(v2);
                    double cost = Double.parseDouble(elements[2]);

                    ArrayList<Integer> route = new ArrayList<>();
                    for (int i = routeStarting; i < elements.length; i++) {
                        route.add(Integer.parseInt(elements[i]));
                    }

                    graphEdgeCosts.put(edge, cost);
                    graphEdgeRoutes.put(edge, convertIntegerArray(route.toArray(new Integer[0])));

                    // Prepare for next entry
                    line = br.readLine();
                }

                int[] vertices = new int[graphVertices.size()];
                int i = 0;
                for (int vertex : graphVertices) {
                    vertices[i++] = vertex;
                }
                Arrays.sort(vertices);

                data.setGraphVertices(vertices);
                data.setGraphEdgeCosts(graphEdgeCosts);
                data.setGraphEdgeRoutes(graphEdgeRoutes);
                System.out.println();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("Not Yet Generated.");
        }
    }

    private static void loadDelaunayPairs() {
        // Check if file exists
        String delaunayPairsPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/DelaunayNetwork/DelaunayPaths.txt";
        if (new File(delaunayPairsPath).exists()) {
            // Load from file.
            try (BufferedReader br = new BufferedReader(new FileReader(delaunayPairsPath))) {
                br.readLine();
                String line = br.readLine();

                HashSet<Edge> pairs = new HashSet<>();
                while (line != null) {
                    String[] elements = line.split("\\s+");
                    int v1 = Integer.parseInt(elements[4]);
                    int v2 = Integer.parseInt(elements[5]);
                    Edge edge = new Edge(v1, v2);
                    pairs.add(edge);

                    // Prepare for next entry
                    line = br.readLine();
                }

                data.setDelaunayPairs(pairs);
                System.out.println();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("Not Yet Generated.");
        }
    }

    public static void saveShortestPathsNetwork() {
        int[][] shortestPaths = data.getShortestPaths();
        double[] shortestPathCosts = data.getShortestPathCosts();

        String rawPathsPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/RawPaths/RawPaths.txt";

        Path pathToFile = Paths.get(rawPathsPath);
        try {
            if (!pathToFile.getParent().toFile().exists())
                Files.createDirectories(pathToFile.getParent());
            if (!pathToFile.toFile().exists())
                Files.createFile(pathToFile);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        // Save to file.
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(rawPathsPath))) {
            bw.write("FromCell\tToCell\tCost\tLength\n");
            for (int pathNum = 0; pathNum < shortestPaths.length; pathNum++) {
                int[] pathArray = shortestPaths[pathNum];
                bw.write(pathArray[0] + "\t" + pathArray[pathArray.length - 1] + "\t" + shortestPathCosts[pathNum] + "\t" + pathArray.length + "\n");
                bw.write(pathArray.length + "");
                for (int aPathArray : pathArray) {
                    bw.write("\t" + aPathArray);
                }
                bw.write("\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void saveDelaunayPairs() {
        HashSet<Edge> delaunayPairs = data.getDelaunayPairs();

        String delaunayPairsPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/DelaunayNetwork/DelaunayPaths.txt";

        // Save to file.
        Path pathToFile = Paths.get(delaunayPairsPath);
        try {
            if (!pathToFile.getParent().toFile().exists())
                Files.createDirectories(pathToFile.getParent());
            if (!pathToFile.toFile().exists())
                Files.createFile(pathToFile);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(delaunayPairsPath))) {
            bw.write("#  Selected node pairs\n");
            for (Edge pair : delaunayPairs) {
                int vNum = data.sourceNum(pair.v1);
                if (vNum > -1) {
                    bw.write("SOURCE\t" + data.getSources()[vNum].getLabel() + "\t");
                } else {
                    bw.write("SINK\t" + data.getSinks()[data.sinkNum(pair.v1)].getLabel() + "\t");
                }
                vNum = data.sourceNum(pair.v2);
                if (vNum > -1) {
                    bw.write("SOURCE\t" + data.getSources()[vNum].getLabel() + "\t");
                } else {
                    bw.write("SINK\t" + data.getSinks()[data.sinkNum(pair.v2)].getLabel() + "\t");
                }
                bw.write(pair.v1 + "\t" + pair.v2 + "\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void saveCandidateGraph() {
        HashMap<Edge, Double> graphEdgeCosts = data.getGraphEdgeCosts();
        HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();

        String candidateNetworkPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/Network/CandidateNetwork/CandidateNetwork.txt";

        Path pathToFile = Paths.get(candidateNetworkPath);
        try {
            if (!pathToFile.getParent().toFile().exists())
                Files.createDirectories(pathToFile.getParent());
            if (!pathToFile.toFile().exists())
                Files.createFile(pathToFile);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        // Save to file.
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(candidateNetworkPath))) {
            bw.write("Vertex1\tVertex2\tCost\tCellRoute\n");
            for (Edge e : graphEdgeRoutes.keySet()) {
                bw.write(e.v1 + "\t" + e.v2 + "\t" + graphEdgeCosts.get(e));
                int[] route = graphEdgeRoutes.get(e);
                for (int vertex : route) {
                    bw.write("\t" + vertex);
                }
                bw.write("\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static Solution loadSolution(String solutionPath) {
        double threshold = .000001;
        Solution soln = new Solution();

        // Make file paths.
        File solFile = null;
        File mpsFile = null;
        for (File f : new File(solutionPath).listFiles()) {
            if (f.getName().endsWith(".sol")) {
                solFile = f;
            } else if (f.getName().endsWith(".mps")) {
                mpsFile = f;
            }
        }

        // Collect data.
        Source[] sources = data.getActiveSources();
        Sink[] sinks = data.getActiveSinks();
        int[] graphVertices = data.getGraphVertices();

        // Make cell/index maps.
        HashMap<Source, Integer> sourceCellToIndex = new HashMap<>();
        HashMap<Integer, Source> sourceIndexToCell = new HashMap<>();
        HashMap<Sink, Integer> sinkCellToIndex = new HashMap<>();
        HashMap<Integer, Sink> sinkIndexToCell = new HashMap<>();
        HashMap<Integer, Integer> vertexCellToIndex = new HashMap<>();
        HashMap<Integer, Integer> vertexIndexToCell = new HashMap<>();

        // Initialize cell/index maps.
        for (int i = 0; i < sources.length; i++) {
            sourceCellToIndex.put(sources[i], i);
            sourceIndexToCell.put(i, sources[i]);
        }
        for (int i = 0; i < sinks.length; i++) {
            sinkCellToIndex.put(sinks[i], i);
            sinkIndexToCell.put(i, sinks[i]);
        }
        for (int i = 0; i < graphVertices.length; i++) {
            vertexCellToIndex.put(graphVertices[i], i);
            vertexIndexToCell.put(i, graphVertices[i]);
        }

        HashMap<String, Double> variableValues = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(solFile))) {
            String line = br.readLine();
            while (!line.equals(" <variables>")) {
                line = br.readLine();
            }
            line = br.readLine();

            while (!line.equals(" </variables>")) {
                String[] variable = split(line);
                if (Double.parseDouble(variable[2]) > threshold) {
                    variableValues.put(variable[0], Double.parseDouble(variable[2]));
                    String[] components = variable[0].split("\\]\\[|\\[|\\]");
                    if (components[0].equals("a")) {
                        soln.addSourceCaptureAmount(sources[Integer.parseInt(components[1])], Double.parseDouble(variable[2]));
                    } else if (components[0].equals("b")) {
                        soln.addSinkStorageAmount(sinks[Integer.parseInt(components[1])], Double.parseDouble(variable[2]));
                    } else if (components[0].equals("p")) {
                        soln.addEdgeTransportAmount(new Edge(vertexIndexToCell.get(Integer.parseInt(components[1])), vertexIndexToCell.get(Integer.parseInt(components[2]))), Double.parseDouble(variable[2]));
                    } else if (variable[0].equals("captureTarget")) {
                        soln.setTargetCaptureAmountPerYear(Double.parseDouble(variable[2]));
                    } else if (variable[0].equals("crf")) {
                        soln.setCRF(Double.parseDouble(variable[2]));
                    } else if (variable[0].equals("projectLength")) {
                        soln.setProjectLength(Integer.parseInt(variable[2]));
                    }
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        try (BufferedReader br = new BufferedReader(new FileReader(mpsFile))) {
            String line = br.readLine();
            while (!line.equals("COLUMNS")) {
                line = br.readLine();
            }
            br.readLine();
            line = br.readLine();

            while (!line.equals("RHS")) {
                String[] column = line.replaceFirst("\\s+", "").split("\\s+");
                if (column[1].equals("OBJ") && variableValues.keySet().contains(column[0])) {
                    String[] components = column[0].split("\\]\\[|\\[|\\]");
                    if (column[0].charAt(0) == 's' || column[0].charAt(0) == 'a') {
                        double cost = variableValues.get(column[0]) * Double.parseDouble(column[2]);
                        soln.addSourceCostComponent(sources[Integer.parseInt(components[1])], cost);
                    } else if (column[0].charAt(0) == 'r' || column[0].charAt(0) == 'w' || column[0].charAt(0) == 'b') {
                        double cost = variableValues.get(column[0]) * Double.parseDouble(column[2]);
                        soln.addSinkCostComponent(sinks[Integer.parseInt(components[1])], cost);
                    } else if (column[0].charAt(0) == 'p' || column[0].charAt(0) == 'y') {
                        double cost = variableValues.get(column[0]) * Double.parseDouble(column[2]);
                        soln.addEdgeCostComponent(new Edge(vertexIndexToCell.get(Integer.parseInt(components[1])), vertexIndexToCell.get(Integer.parseInt(components[2]))), cost);
                    }
                } else if (column[1].charAt(0) == 'F' && variableValues.keySet().contains(column[0])) {
                    String[] components = column[0].split("\\]\\[|\\[|\\]");
                    if (column[0].charAt(0) == 'r') {
                        double capacity = Double.parseDouble(column[2]);
                        soln.addSinkCapacityComponent(sinks[Integer.parseInt(components[1])], capacity * soln.getProjectLength());
                    }
                } else if (column[1].charAt(0) == 'E' && variableValues.keySet().contains(column[0])) {
                    String[] components = column[0].split("\\]\\[|\\[|\\]");
                    if (column[0].charAt(0) == 'w') {
                        double capacity = Double.parseDouble(column[2]);
                        soln.addSinkWellCapacityComponent(sinks[Integer.parseInt(components[1])], capacity);
                    }
                } else if (column[1].charAt(0) == 'D' && variableValues.keySet().contains(column[0])) {
                    String[] components = column[0].split("\\]\\[|\\[|\\]");
                    if (column[0].charAt(0) == 's') {
                        double production = Double.parseDouble(column[2]);
                        soln.addSourceProductionRateComponent(sources[Integer.parseInt(components[1])], production);
                    }
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return soln;
    }

    private static String[] split(String variable) {
        String[] components = variable.split("\"");
        return new String[]{components[1], components[3], components[5]};
    }

    public static void makeShapeFiles(String path, Solution soln) {
        // Make shapefiles if they do not already exist.
        File newDir = new File(path + "/shapeFiles/");
        if (!newDir.exists()) {
            newDir.mkdir();

            // Collect data.
            Source[] sources = data.getSources();
            Sink[] sinks = data.getSinks();
            HashMap<Source, Double> sourceCaptureAmounts = soln.getSourceCaptureAmounts();
            HashMap<Sink, Double> sinkStorageAmounts = soln.getSinkStorageAmounts();
            HashMap<Edge, Double> edgeTransportAmounts = soln.getEdgeTransportAmounts();
            HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();

            // Map projection string.
            String projction = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]";

            // Make source shapefiles.
            EsriPointList sourceList = new EsriPointList();
            String[] sourceAttributeNames = {"Id", "X", "Y", "CO2Cptrd", "MxSpply", "PieWdge", "GensUsed", "MaxGens", "ActlCst", "TtlCst", "Name", "Cell#"};
            int[] sourceAttributeDecimals = {0, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0};
            DbfTableModel sourceAttributeTable = new DbfTableModel(sourceAttributeNames.length);   //12
            for (int colNum = 0; colNum < sourceAttributeNames.length; colNum++) {
                sourceAttributeTable.setColumnName(colNum, sourceAttributeNames[colNum]);
                sourceAttributeTable.setDecimalCount(colNum, (byte) sourceAttributeDecimals[colNum]);
                sourceAttributeTable.setLength(colNum, 10);
                if (sourceAttributeNames[colNum].equals("Id")) {
                    sourceAttributeTable.setType(colNum, DbfTableModel.TYPE_CHARACTER);
                } else {
                    sourceAttributeTable.setType(colNum, DbfTableModel.TYPE_NUMERIC);
                }
            }
            for (Source src : sources) {
                EsriPoint source = new EsriPoint(data.cellToLatLon(src.getCellNum())[0], data.cellToLatLon(src.getCellNum())[1]);
                sourceList.add(source);

                // Add attributes.
                ArrayList row = new ArrayList();
                row.add(src.getLabel());
                row.add(data.cellToLatLon(src.getCellNum())[1]);
                row.add(data.cellToLatLon(src.getCellNum())[0]);
                if (sourceCaptureAmounts.containsKey(src)) {
                    row.add(sourceCaptureAmounts.get(src));
                    row.add(soln.getSourceProductionRate().get(src));
                    row.add(soln.getSourceProductionRate().get(src) - sourceCaptureAmounts.get(src));
                } else {
                    row.add(0);
                    row.add(src.getProductionRate());
                    row.add(src.getProductionRate());
                }
                for (int i = 0; i < 6; i++) {
                    row.add(0);
                }

                sourceAttributeTable.addRecord(row);
            }

            EsriShapeExport writeSourceShapefiles = new EsriShapeExport(sourceList, sourceAttributeTable, newDir.toString() + "/Sources");
            writeSourceShapefiles.export();
            try(PrintWriter out = new PrintWriter( newDir.toString() + "/Sources.prj")){
                out.println(projction);
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }

            // Make sink shapefiles.
            EsriPointList sinkList = new EsriPointList();
            String[] sinkAttributeNames = {"Id", "X", "Y", "CO2Strd", "MxStrg", "PieWdge", "WllsUsed", "MxWlls", "ActCst", "TtlCst", "Name", "Cell#"};
            int[] sinkAttributeDecimals = {0, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0};
            DbfTableModel sinkAttributeTable = new DbfTableModel(sinkAttributeNames.length);   //12
            for (int colNum = 0; colNum < sinkAttributeNames.length; colNum++) {
                sinkAttributeTable.setColumnName(colNum, sinkAttributeNames[colNum]);
                sinkAttributeTable.setDecimalCount(colNum, (byte) sinkAttributeDecimals[colNum]);
                sinkAttributeTable.setLength(colNum, 10);
                if (sinkAttributeNames[colNum].equals("Id")) {
                    sinkAttributeTable.setType(colNum, DbfTableModel.TYPE_CHARACTER);
                } else {
                    sinkAttributeTable.setType(colNum, DbfTableModel.TYPE_NUMERIC);
                }
            }
            for (Sink snk : sinks) {
                EsriPoint source = new EsriPoint(data.cellToLatLon(snk.getCellNum())[0], data.cellToLatLon(snk.getCellNum())[1]);
                sinkList.add(source);

                // Add attributes.
                ArrayList row = new ArrayList();
                row.add(snk.getLabel());
                row.add(data.cellToLatLon(snk.getCellNum())[1]);
                row.add(data.cellToLatLon(snk.getCellNum())[0]);
                if (sinkStorageAmounts.containsKey(snk)) {
                    row.add(sinkStorageAmounts.get(snk));
                    row.add(soln.getSinkCapacity().get(snk) / soln.getProjectLength());
                    row.add(soln.getSinkCapacity().get(snk) / soln.getProjectLength() - sinkStorageAmounts.get(snk));
                } else {
                    row.add(0);
                    row.add(snk.getCapacity() / soln.getProjectLength());
                    row.add(snk.getCapacity() / soln.getProjectLength());
                }
                for (int i = 0; i < 6; i++) {
                    row.add(0);
                }

                sinkAttributeTable.addRecord(row);
            }

            EsriShapeExport writeSinkShapefiles = new EsriShapeExport(sinkList, sinkAttributeTable, newDir.toString() + "/Sinks");
            writeSinkShapefiles.export();
            try(PrintWriter out = new PrintWriter( newDir.toString() + "/Sinks.prj")){
                out.println(projction);
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }

            // Make network shapefiles.
            EsriPolylineList edgeList = new EsriPolylineList();
            String[] edgeAttributeNames = {"Id", "CapID", "CapValue", "Flow", "Cost", "LengKM", "LengROW", "LengCONS", "Variable"};
            int[] edgeAttributeDecimals = {0, 0, 0, 6, 0, 0, 0, 0, 0};
            DbfTableModel edgeAttributeTable = new DbfTableModel(edgeAttributeNames.length);   //12
            for (int colNum = 0; colNum < edgeAttributeNames.length; colNum++) {
                edgeAttributeTable.setColumnName(colNum, edgeAttributeNames[colNum]);
                edgeAttributeTable.setDecimalCount(colNum, (byte) edgeAttributeDecimals[colNum]);
                edgeAttributeTable.setLength(colNum, 10);
                if (edgeAttributeNames[colNum].equals("Id")) {
                    edgeAttributeTable.setType(colNum, DbfTableModel.TYPE_CHARACTER);
                } else {
                    edgeAttributeTable.setType(colNum, DbfTableModel.TYPE_NUMERIC);
                }
            }
            for (Edge edg : soln.getOpenedEdges()) {
                // Build route
                int[] route = graphEdgeRoutes.get(edg);
                double[] routeLatLon = new double[route.length * 2];    // Route cells translated into: lat, lon, lat, lon,...
                for (int i = 0; i < route.length; i++) {
                    int cell = route[i];
                    routeLatLon[i * 2] = data.cellToLatLon(cell)[0];
                    routeLatLon[i * 2 + 1] = data.cellToLatLon(cell)[1];
                }
                
                EsriPolyline edge = new EsriPolyline(routeLatLon, OMGraphic.DECIMAL_DEGREES, OMGraphic.LINETYPE_STRAIGHT);
                edgeList.add(edge);

                // Add attributes.
                ArrayList row = new ArrayList();
                for (int i = 0; i < 3; i++) {
                    row.add(0);
                }
                row.add(edgeTransportAmounts.get(edg));
                for (int i = 0; i < 5; i++) {
                    row.add(0);
                }

                edgeAttributeTable.addRecord(row);
            }

            EsriShapeExport writeEdgeShapefiles = new EsriShapeExport(edgeList, edgeAttributeTable, newDir.toString() + "/Network");
            writeEdgeShapefiles.export();
            try(PrintWriter out = new PrintWriter( newDir.toString() + "/Network.prj")){
                out.println(projction);
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
        }
    }

    public static void dumpSink2Text (String basePath, String dataset, String scenario, String filename){
        String directoryName = basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/Uncertainty";
        File directory = new File(String.valueOf(directoryName));
        if(!directory.exists()){
            directory.mkdir();
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(directoryName + "/" + filename, false));
            // APPEND MODE SET HERE
            bw.write("Label\t" + "Capacity\t" + "OpeningCost\t" + "OMCost\t" + "WellCapacity\t" + "WellOpeningCost\t" + "WellOMCostMean\t" + "InjectionCost\t");
            bw.newLine();
            for (Sink s : data.getSinks()) {
                bw.write(s.getLabel()); bw.write("\t");
                bw.write(Double.toString(s.getCapacity())); bw.write("\t");
                bw.write(Double.toString(s.getInternalOpeningCost())); bw.write("\t");
                bw.write(Double.toString(s.getInternalOMCost())); bw.write("\t");
                bw.write(Double.toString(s.getWellCapacity())); bw.write("\t");
                bw.write(Double.toString(s.getInternalWellOpeningCost())); bw.write("\t");
                bw.write(Double.toString(s.getInternalWellOMCost())); bw.write("\t");
                bw.write(Double.toString(s.getInjectionCost()));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } finally {
            if (bw != null) try {
                bw.close();
            } catch (IOException ignored) {
                // just ignore it
            }
        }
    }

    public static void dumpSource2Text (String basePath, String dataset, String scenario, String filename){
        String directoryName = basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/Uncertainty";
        File directory = new File(String.valueOf(directoryName));
        if(!directory.exists()){
            directory.mkdir();
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(directoryName + "/" + filename, false));
            // APPEND MODE SET HERE
            bw.write("Label\t" + "OpeningCost\t" + "OMCost\t" + "CaptureCost\t" + "ProductionRate\t");
            bw.newLine();
            for (Source s : data.getSources()) {
                bw.write(s.getLabel()); bw.write("\t");
                bw.write(Double.toString(s.getInternalOpeningCost())); bw.write("\t");
                bw.write(Double.toString(s.getInternalOMCost())); bw.write("\t");
                bw.write(Double.toString(s.getCaptureCost())); bw.write("\t");
                bw.write(Double.toString(s.getProductionRate()));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } finally {
            if (bw != null) try {
                bw.close();
            } catch (IOException ignored) {
                // just ignore it
            }
        }
    }

    public static void makeGenerateFile(String path, Solution soln) {
        File newDir = new File(path + "/genFiles");
        if (true) {
            newDir.mkdir();
            Source[] sources = data.getSources();
            Sink[] sinks = data.getSinks();
            HashMap<Source, Double> sourceCaptureAmounts = soln.getSourceCaptureAmounts();
            HashMap<Sink, Double> sinkStorageAmounts = soln.getSinkStorageAmounts();
            HashMap<Edge, Double> edgeTransportAmounts = soln.getEdgeTransportAmounts();
            HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();

            // Make Sources.
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(newDir, "Sources.txt")))) {
                bw.write("ID,X,Y,CO2Cptrd,MxSpply,PieWdge,GensUsed,MaxGens,ActlCst,TtlCst,Name,Cell#\n");
                for (Source src : sources) {
                    bw.write(src.getLabel() + "," + data.cellToLatLon(src.getCellNum())[1] + "," + data.cellToLatLon(src.getCellNum())[0] + ",");
                    if (sourceCaptureAmounts.containsKey(src)) {
                        bw.write(sourceCaptureAmounts.get(src) + "," + src.getProductionRate() + "," + (src.getProductionRate() - sourceCaptureAmounts.get(src)));
                    } else {
                        bw.write("0," + src.getProductionRate() + "," + src.getProductionRate());
                    }
                    bw.write(",0,0,0,0,0,0\n");
                }
                bw.write("END");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

            // Make Sinks.
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(newDir, "Sinks.txt")))) {
                bw.write("ID,X,Y,CO2Strd,MxStrg,PieWdge,WllsUsd,MxWlls,ActCst,TtlCst,Name,Cell#\n");
                for (Sink snk : sinks) {
                    bw.write(snk.getLabel() + "," + data.cellToLatLon(snk.getCellNum())[1] + "," + data.cellToLatLon(snk.getCellNum())[0] + ",");
                    if (sinkStorageAmounts.containsKey(snk)) {
                        bw.write(sinkStorageAmounts.get(snk) + "," + snk.getCapacity() + "," + (snk.getCapacity() - sinkStorageAmounts.get(snk)));
                    } else {
                        bw.write("0," + snk.getCapacity() + "," + snk.getCapacity());
                    }
                    bw.write(",0,0,0,0,0,0\n");
                }
                bw.write("END");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

            // Make PipeDiameters.
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(newDir, "PipeDiameters.txt")))) {
                bw.write("ID,CapID,CapValue,Flow,Cost,LengKM,LengROW,LengCONS,Variable\n");
                for (Edge e : soln.getOpenedEdges()) {
                    bw.write("0,0,0," + edgeTransportAmounts.get(e) + ",0,0,0,0,0\n");
                    int[] route = graphEdgeRoutes.get(e);
                    for (int vertex : route) {
                        bw.write(round(data.cellToLatLon(vertex)[1], 5) + "," + round(data.cellToLatLon(vertex)[0], 5) + "\n");
                    }
                    bw.write("END\n");
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void saveSourceSinkState(String path, DataStorer data) {
        Path sourcePath = Paths.get(path + "/Sources/SourceStates.txt");
        Path sinkPath = Paths.get(path + "/Sinks/SinkStates.txt");
        try {
            if (!sourcePath.getParent().toFile().exists())
                Files.createDirectories(sourcePath.getParent());
            if (!sinkPath.getParent().toFile().exists())
                Files.createDirectories(sinkPath.getParent());
            if (!sourcePath.toFile().exists())
                Files.createFile(sourcePath);
            if (!sinkPath.toFile().exists())
                Files.createFile(sinkPath);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(sourcePath.toFile()))) {
            for (Source s : data.getSources()) {
                if (s.isDisabled()) {
                    bw.write(s.getLabel() + "\t0");
                    bw.newLine();
                }
                else {
                    bw.write(s.getLabel() + "\t1");
                    bw.newLine();
                }
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(sinkPath.toFile()))) {
            for (Sink s : data.getSinks()) {
                if (s.isDisabled()) {
                    bw.write(s.getLabel() + "\t0");
                    bw.newLine();
                }
                else {
                    bw.write(s.getLabel() + "\t1");
                    bw.newLine();
                }
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}
