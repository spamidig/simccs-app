package simccs.solver;

import simccs.dataStore.DataStorer;
import simccs.dataStore.Edge;
import simccs.dataStore.LinearComponent;
import simccs.dataStore.Sink;
import simccs.dataStore.Source;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.*;

/**
 *
 * @author yaw
 */
public class MPSWriter {

    public static void writeMPS(String fileName, DataStorer data, double crf, double numYears, double capacityTarget, String basePath, String dataset, String scenario) {
        double pipeUtilization = .93;

        // Collect data
        data.loadNetworkCosts();
        Source[] sources = data.getActiveSources();
        Sink[] sinks = data.getActiveSinks();
        LinearComponent[] linearComponents = data.getLinearComponents();
        int[] graphVertices = data.getGraphVertices();
        HashMap<Integer, HashSet<Integer>> neighbors = data.getGraphNeighbors();

        HashMap<Edge, Double> edgeCosts = data.getGraphEdgeCosts();
        HashMap<Edge, Double> edgeConstructionCosts = data.getGraphEdgeConstructionCosts();
        HashMap<Edge, Double> edgeRightOfWayCosts = data.getGraphEdgeRightOfWayCosts();
        HashMap<Source, Integer> sourceCellToIndex = new HashMap<>();
        HashMap<Integer, Source> sourceIndexToCell = new HashMap<>();
        HashMap<Sink, Integer> sinkCellToIndex = new HashMap<>();
        HashMap<Integer, Sink> sinkIndexToCell = new HashMap<>();
        HashMap<Integer, Integer> vertexCellToIndex = new HashMap<>();
        HashMap<Integer, Integer> vertexIndexToCell = new HashMap<>();
        HashSet<Integer> sourceCells = new HashSet<>();
        HashSet<Integer> sinkCells = new HashSet<>();

        HashMap<String, HashSet<ConstraintTerm>> intVariableToConstraints = new HashMap<>();
        HashMap<String, HashSet<ConstraintTerm>> contVariableToConstraints = new HashMap<>();
        HashMap<String, String> constraintToSign = new HashMap<>();
        HashMap<String, Double> constraintRHS = new HashMap<>();
        HashMap<String, VariableBound> variableBounds = new HashMap<>();

        // Initialize cell/index maps
        for (int i = 0; i < sources.length; i++) {
            sourceCellToIndex.put(sources[i], i);
            sourceIndexToCell.put(i, sources[i]);
            sourceCells.add(sources[i].getCellNum());
        }
        for (int i = 0; i < sinks.length; i++) {
            sinkCellToIndex.put(sinks[i], i);
            sinkIndexToCell.put(i, sinks[i]);
            sinkCells.add(sinks[i].getCellNum());
        }
        for (int i = 0; i < graphVertices.length; i++) {
            vertexCellToIndex.put(graphVertices[i], i);
            vertexIndexToCell.put(i, graphVertices[i]);
        }

        // Build model
        // Make variables
        // Source openings
        String[] s = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            s[i] = "s[" + i + "]";
            variableBounds.put(s[i], new VariableBound("UP", 1));
        }

        // Capture amounts
        String[] a = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            a[i] = "a[" + i + "]";
        }

        // Reservoir openings
        String[] r = new String[sinks.length];
        for (int i = 0; i < sinks.length; i++) {
            r[i] = "r[" + i + "]";
            variableBounds.put(r[i], new VariableBound("UP", 1));
        }

        // Injection amounts
        String[] b = new String[sinks.length];
        for (int i = 0; i < sinks.length; i++) {
            b[i] = "b[" + i + "]";
        }

        // Well openings
        String[] w = new String[sinks.length];
        for (int i = 0; i < sinks.length; i++) {
            w[i] = "w[" + i + "]";
            variableBounds.put(w[i], new VariableBound("LI", 0));
        }

        // Pipeline between i and j with trend c
        String[][][] y = new String[graphVertices.length][graphVertices.length][linearComponents.length];
        for (int i = 0; i < graphVertices.length; i++) {
            for (int j = 0; j < graphVertices.length; j++) {
                for (int c = 0; c < linearComponents.length; c++) {
                    for (int dest : neighbors.get(vertexIndexToCell.get(i))) {
                        if (dest == vertexIndexToCell.get(j)) {
                            y[i][j][c] = "y[" + i + "][" + j + "][" + c + "]";
                            variableBounds.put(y[i][j][c], new VariableBound("UP", 1));
                        }
                    }
                }
            }
        }

        // Pipeline capcaity
        String[][][] p = new String[graphVertices.length][graphVertices.length][linearComponents.length];
        for (int i = 0; i < graphVertices.length; i++) {
            for (int j = 0; j < graphVertices.length; j++) {
                for (int c = 0; c < linearComponents.length; c++) {
                    p[i][j][c] = "p[" + i + "][" + j + "][" + c + "]";
                }
            }
        }

        // Make constraints
        // Pipeline capacity constraints
        int constraintCounter = 1;
        for (int src : graphVertices) {
            for (int dest : neighbors.get(src)) {
                for (int c = 0; c < linearComponents.length; c++) {
                    String constraint = "A" + constraintCounter++;
                    if (!contVariableToConstraints.containsKey(p[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c])) {
                        contVariableToConstraints.put(p[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c], new HashSet<>());
                    }
                    contVariableToConstraints.get(p[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c]).add(new ConstraintTerm(constraint, 1));

                    if (!intVariableToConstraints.containsKey(y[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c])) {
                        intVariableToConstraints.put(y[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c], new HashSet<>());
                    }
                    intVariableToConstraints.get(y[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c]).add(new ConstraintTerm(constraint, -capacityTarget));

                    constraintToSign.put(constraint, "L");
                    constraintRHS.put(constraint, 0.0);

                    constraint = "A" + constraintCounter++;
                    contVariableToConstraints.get(p[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c]).add(new ConstraintTerm(constraint, 1));
                    constraintToSign.put(constraint, "G");
                }
            }
        }

        // No pipeline loops
        constraintCounter = 1;
        for (int src : graphVertices) {
            for (int dest : neighbors.get(src)) {
                String constraint = "B" + constraintCounter++;
                for (int c = 0; c < linearComponents.length; c++) {
                    if (!intVariableToConstraints.containsKey(y[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c])) {
                        intVariableToConstraints.put(y[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c], new HashSet<>());
                    }
                    intVariableToConstraints.get(y[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c]).add(new ConstraintTerm(constraint, 1));

                }
                constraintToSign.put(constraint, "L");
                constraintRHS.put(constraint, 1.0);
            }
        }

        // Conservation of flow
        constraintCounter = 1;
        for (int src : graphVertices) {
            String constraint = "C" + constraintCounter++;
            for (int dest : neighbors.get(src)) {
                for (int c = 0; c < linearComponents.length; c++) {
                    if (!contVariableToConstraints.containsKey(p[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c])) {
                        contVariableToConstraints.put(p[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c], new HashSet<>());
                    }
                    contVariableToConstraints.get(p[vertexCellToIndex.get(src)][vertexCellToIndex.get(dest)][c]).add(new ConstraintTerm(constraint, 1));
                }
            }

            for (int dest : neighbors.get(src)) {
                for (int c = 0; c < linearComponents.length; c++) {
                    if (!contVariableToConstraints.containsKey(p[vertexCellToIndex.get(dest)][vertexCellToIndex.get(src)][c])) {
                        contVariableToConstraints.put(p[vertexCellToIndex.get(dest)][vertexCellToIndex.get(src)][c], new HashSet<>());
                    }
                    contVariableToConstraints.get(p[vertexCellToIndex.get(dest)][vertexCellToIndex.get(src)][c]).add(new ConstraintTerm(constraint, -1));
                }
            }

            // Set right hand side
            if (sourceCells.contains(src)) {
                for (Source source : sources) {
                    if (source.getCellNum() == src) {
                        if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(source)])) {
                            contVariableToConstraints.put(a[sourceCellToIndex.get(source)], new HashSet<>());
                        }
                        contVariableToConstraints.get(a[sourceCellToIndex.get(source)]).add(new ConstraintTerm(constraint, -1));

                        constraintToSign.put(constraint, "E");
                    }
                }
            } 
            if (sinkCells.contains(src)) {
                for (Sink sink : sinks) {
                    if (sink.getCellNum() == src) {
                        if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(sink)])) {
                            contVariableToConstraints.put(b[sinkCellToIndex.get(sink)], new HashSet<>());
                        }
                        contVariableToConstraints.get(b[sinkCellToIndex.get(sink)]).add(new ConstraintTerm(constraint, 1));

                        constraintToSign.put(constraint, "E");
                    }
                }

            } else {
                constraintToSign.put(constraint, "E");
            }

        }

        // Capture capped by max production
        constraintCounter = 1;
        for (Source src : sources) {
            String constraint = "D" + constraintCounter++;

            if (!intVariableToConstraints.containsKey(s[sourceCellToIndex.get(src)])) {
                intVariableToConstraints.put(s[sourceCellToIndex.get(src)], new HashSet<>());
            }
            intVariableToConstraints.get(s[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, src.getProductionRate()));

            if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<>());
            }
            contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, -1));
            constraintToSign.put(constraint, "G");
            //constraintRHS.put(constraint, 0.0);
        }

        // Well injection capped by max injectivity
        constraintCounter = 1;
        for (Sink snk : sinks) {
            String constraint = "E" + constraintCounter++;

            if (!intVariableToConstraints.containsKey(w[sinkCellToIndex.get(snk)])) {
                intVariableToConstraints.put(w[sinkCellToIndex.get(snk)], new HashSet<>());
            }
            intVariableToConstraints.get(w[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getWellCapacity()));

            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)])) {
                contVariableToConstraints.put(b[sinkCellToIndex.get(snk)], new HashSet<>());
            }
            contVariableToConstraints.get(b[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, -1));
            constraintToSign.put(constraint, "G");
            //constraintRHS.put(constraint, 0.0);
        }

        // Storage capped by max capacity
        constraintCounter = 1;
        for (Sink snk : sinks) {
            String constraint = "F" + constraintCounter++;
            if (!intVariableToConstraints.containsKey(r[sinkCellToIndex.get(snk)])) {
                intVariableToConstraints.put(r[sinkCellToIndex.get(snk)], new HashSet<>());
            }
            intVariableToConstraints.get(r[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getCapacity() / numYears));

            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)])) {
                contVariableToConstraints.put(b[sinkCellToIndex.get(snk)], new HashSet<>());
            }
            contVariableToConstraints.get(b[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, -1));
            constraintToSign.put(constraint, "G");
            //constraintRHS.put(constraint, 0.0);
        }

        // Set amount of CO2 to capture
        constraintCounter = 1;
        String constraint = "G" + constraintCounter++;
        for (Source src : sources) {
            if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<>());
            }
            contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, 1));
        }
        constraintToSign.put(constraint, "G");
        constraintRHS.put(constraint, capacityTarget);

        // Hardcode constants.
        contVariableToConstraints.put("captureTarget", new HashSet<>());
        contVariableToConstraints.get("captureTarget").add(new ConstraintTerm("H1", 1));
        constraintToSign.put("H1", "E");
        constraintRHS.put("H1", capacityTarget);
        contVariableToConstraints.put("crf", new HashSet<>());
        contVariableToConstraints.get("crf").add(new ConstraintTerm("H2", 1));
        constraintToSign.put("H2", "E");
        constraintRHS.put("H2", crf);
        contVariableToConstraints.put("projectLength", new HashSet<>());
        contVariableToConstraints.get("projectLength").add(new ConstraintTerm("H3", 1));
        constraintToSign.put("H3", "E");
        constraintRHS.put("H3", numYears);

        // Make objective
        constraint = "OBJ";
        for (Source src : sources) {
            if (!intVariableToConstraints.containsKey(s[sourceCellToIndex.get(src)])) {
                intVariableToConstraints.put(s[sourceCellToIndex.get(src)], new HashSet<>());
            }
            intVariableToConstraints.get(s[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, src.getOpeningCost(crf)));

            if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<>());
            }
            contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, src.getCaptureCost()));
        }

        for (int vertex : graphVertices) {
            for (int neighbor : neighbors.get(vertex)) {
                for (int c = 0; c < linearComponents.length; c++) {
                    if (!intVariableToConstraints.containsKey(y[vertexCellToIndex.get(vertex)][vertexCellToIndex.get(neighbor)][c])) {
                        intVariableToConstraints.put(y[vertexCellToIndex.get(vertex)][vertexCellToIndex.get(neighbor)][c], new HashSet<>());
                    }
                    double coefficient = (linearComponents[c].getConBeta() * edgeConstructionCosts.get(new Edge(vertex, neighbor)) + linearComponents[c].getRowBeta() * edgeRightOfWayCosts.get(new Edge(vertex, neighbor))) * crf;
                    intVariableToConstraints.get(y[vertexCellToIndex.get(vertex)][vertexCellToIndex.get(neighbor)][c]).add(new ConstraintTerm(constraint, coefficient));

                    if (!contVariableToConstraints.containsKey(p[vertexCellToIndex.get(vertex)][vertexCellToIndex.get(neighbor)][c])) {
                        contVariableToConstraints.put(p[vertexCellToIndex.get(vertex)][vertexCellToIndex.get(neighbor)][c], new HashSet<>());
                    }
                    coefficient = (linearComponents[c].getConAlpha() * edgeConstructionCosts.get(new Edge(vertex, neighbor)) + linearComponents[c].getRowAlpha() * edgeRightOfWayCosts.get(new Edge(vertex, neighbor))) * crf / pipeUtilization;
                    contVariableToConstraints.get(p[vertexCellToIndex.get(vertex)][vertexCellToIndex.get(neighbor)][c]).add(new ConstraintTerm(constraint, coefficient));
                }
            }
        }

        for (Sink snk : sinks) {
            if (!intVariableToConstraints.containsKey(r[sinkCellToIndex.get(snk)])) {
                intVariableToConstraints.put(r[sinkCellToIndex.get(snk)], new HashSet<>());
            }
            intVariableToConstraints.get(r[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getOpeningCost(crf)));

            if (!intVariableToConstraints.containsKey(w[sinkCellToIndex.get(snk)])) {
                intVariableToConstraints.put(w[sinkCellToIndex.get(snk)], new HashSet<>());
            }
            intVariableToConstraints.get(w[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getWellOpeningCost(crf)));

            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)])) {
                contVariableToConstraints.put(b[sinkCellToIndex.get(snk)], new HashSet<>());
            }
            contVariableToConstraints.get(b[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getInjectionCost()));
        }

        constraintToSign.put(constraint, "N");

        makeFile(fileName, basePath, dataset, scenario, intVariableToConstraints, contVariableToConstraints, constraintToSign, constraintRHS, variableBounds);
    }

    private static void makeFile(String fileName, String basePath, String dataset, String scenario, HashMap<String, HashSet<ConstraintTerm>> intVariableToConstraints, HashMap<String, HashSet<ConstraintTerm>> contVariableToConstraints, HashMap<String, String> constraintToSign, HashMap<String, Double> constraintRHS, HashMap<String, VariableBound> variableBounds) {
        StringBuilder problemFormulation = new StringBuilder("NAME\tSimCCS\n");

        // Identify constraints.
        problemFormulation.append("ROWS\n");
        for (String constraint : constraintToSign.keySet()) {
            problemFormulation.append("\t").append(constraintToSign.get(constraint)).append("\t").append(constraint).append("\n");
        }

        // Identify columns.
        problemFormulation.append("COLUMNS\n");
        problemFormulation.append("\tMARK0000\t'MARKER'\t'INTORG'\n");
        for (String intVar : intVariableToConstraints.keySet()) {
            for (ConstraintTerm term : intVariableToConstraints.get(intVar)) {
                problemFormulation.append("\t").append(intVar).append("\t").append(term.constraint).append("\t").append(term.coefficient).append("\n");
            }
        }
        problemFormulation.append("\tMARK0001\t'MARKER'\t'INTEND'\n");
        for (String contVar : contVariableToConstraints.keySet()) {
            for (ConstraintTerm term : contVariableToConstraints.get(contVar)) {
                problemFormulation.append("\t").append(contVar).append("\t").append(term.constraint).append("\t").append(term.coefficient).append("\n");
            }
        }

        // Identify RHSs.
        problemFormulation.append("RHS\n");
        for (String constraint : constraintRHS.keySet()) {
            problemFormulation.append("\trhs\t").append(constraint).append("\t").append(constraintRHS.get(constraint)).append("\n");
        }

        // Identify bounds.
        problemFormulation.append("BOUNDS\n");
        for (String variable : variableBounds.keySet()) {
            problemFormulation.append("\t").append(variableBounds.get(variable).type).append(" bnd\t").append(variable).append("\t").append(variableBounds.get(variable).bound).append("\n");
        }

        // End file.
        problemFormulation.append("ENDATA");

        // Save to file.
        String mipPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/" + fileName;

        Path pathToFile = Paths.get(mipPath);
        try {
            if (!pathToFile.getParent().toFile().exists())
                Files.createDirectories(pathToFile.getParent());
            if (!pathToFile.toFile().exists())
                Files.createFile(pathToFile);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mipPath))) {
            bw.write(problemFormulation.toString());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void zipFile(String fileName, String basePath, String dataset, String scenario, String number) {
        final int BUFFER = 2048;

        Path pathToFile = Paths.get(basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/" + fileName);
        try {
            if (!pathToFile.getParent().toFile().exists())
                Files.createDirectories(pathToFile.getParent());
            if (!pathToFile.toFile().exists())
                Files.createFile(pathToFile);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/" + fileName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER];
            for (int i = 0; i < Integer.parseInt(number); i++) {
                String file = basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/" + "mip" + i + ".mps";
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry("mip" + i + ".mps");
                out.putNextEntry(entry);
                int count;
                while((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
                new File(file).delete();
            }
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static class ConstraintTerm {

        String constraint;
        double coefficient;

        public ConstraintTerm(String constraint, double coefficient) {
            this.constraint = constraint;
            this.coefficient = Math.round(coefficient * 100000.0) / 100000.0;
        }
    }

    private static class VariableBound {

        String type;
        double bound;

        public VariableBound(String type, double bound) {
            this.type = type;
            this.bound = bound;
        }
    }
}
