package simccs.solver;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import javafx.util.Pair;
import org.apache.commons.math4.distribution.CauchyDistribution;
import org.apache.commons.math4.distribution.NormalDistribution;
import org.apache.commons.math4.distribution.RealDistribution;
import org.apache.commons.math4.exception.NotStrictlyPositiveException;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import simccs.dataStore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProcessUncertainty extends Task<Void> {
    private String basePath;
    private String dataset;
    private String scenario;
    private DataStorer data;
    private String crf;
    private String numYears;
    private String capacityTarget;
    private String simulationNumber;
    private TextArea messenger;
    private boolean sourceExist = false;
    private boolean sinkExist = false;

    private HashMap<String, Pair<Sink, SinkUncertainty>> sinkUncertaintyMatch;
    private List<List<RealDistribution.Sampler>> sinkSamplerMatrix;
    private List<Pair<Sink, SinkUncertainty>> matchedSinkList;
    private HashMap<String, Pair<Source, SourceUncertainty>> sourceUncertaintyMatch;
    private List<List<RealDistribution.Sampler>> sourceSamplerMatrix;
    private List<Pair<Source, SourceUncertainty>> matchedSourceList;

    @Override
    public Void call() throws InterruptedException {
        if (data.getSourceUncertainties() != null) {
            processSourceUncertainty();
            sourceExist = true;
        }
        if (data.getSinkUncertainties() != null) {
            processSinkUncertainty();
            sinkExist = true;
        }
        sampleUncertainty();
        return null;
    }

    public ProcessUncertainty(String basePath, String dataset, String scenario, DataStorer data,
                              String crf, String numYears, String capacityTarget, String simulationNumber, TextArea messenger) {
        this.basePath = basePath;
        this.dataset = dataset;
        this.scenario = scenario;
        this.data = data;
        this.crf = crf;
        this.numYears = numYears;
        this.capacityTarget = capacityTarget;
        this.simulationNumber = simulationNumber;
        this.messenger = messenger;
    }

    public void processSinkUncertainty() {
        UniformRandomProvider rng = RandomSource.create(RandomSource.MT);
        Sink[] sinks = data.getSinks();
        SinkUncertainty[] sinkUncertainties = data.getSinkUncertainties();
        sinkUncertaintyMatch = new HashMap<>();
        for (Sink sink : sinks) {
            sinkUncertaintyMatch.put(sink.getLabel(), new Pair<>(sink, null));
        }
        for (SinkUncertainty sinkUncertainty : sinkUncertainties) {
            sinkUncertaintyMatch.replace(sinkUncertainty.getLabel(), new Pair<>(sinkUncertaintyMatch.get(sinkUncertainty.getLabel()).getKey(), sinkUncertainty));
        }
//        List<List<RealDistribution>> distMatrix = new ArrayList<>();
        sinkSamplerMatrix = new ArrayList<>();
        matchedSinkList = new ArrayList<>();
        for (Pair<Sink, SinkUncertainty> sinkUncertaintyPair : sinkUncertaintyMatch.values()) {
            if (sinkUncertaintyPair.getValue() != null) {
                try {
                    List<RealDistribution.Sampler> samplerList;
                    RealDistribution dist;
                    switch (sinkUncertaintyPair.getValue().getDistribution()) {
                        case "Normal":
                            matchedSinkList.add(sinkUncertaintyPair);

//                            List<RealDistribution> distList = new ArrayList<>();
                            samplerList = new ArrayList<>();

                            dist = new NormalDistribution(sinkUncertaintyPair.getValue().getCapacity()[0], sinkUncertaintyPair.getValue().getCapacity()[1] + Double.MIN_VALUE);
//                            distList.add(dist);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sinkUncertaintyPair.getValue().getOpeningCost()[0], sinkUncertaintyPair.getValue().getOpeningCost()[1] + Double.MIN_VALUE);
//                            distList.add(dist);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sinkUncertaintyPair.getValue().getOMCost()[0], sinkUncertaintyPair.getValue().getOMCost()[1] + Double.MIN_VALUE);
//                            distList.add(dist);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sinkUncertaintyPair.getValue().getWellCapacity()[0], sinkUncertaintyPair.getValue().getWellCapacity()[1] + Double.MIN_VALUE);
//                            distList.add(dist);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sinkUncertaintyPair.getValue().getWellOpeningCost()[0], sinkUncertaintyPair.getValue().getWellOpeningCost()[1] + Double.MIN_VALUE);
//                            distList.add(dist);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sinkUncertaintyPair.getValue().getWellOMCost()[0], sinkUncertaintyPair.getValue().getWellOMCost()[1] + Double.MIN_VALUE);
//                            distList.add(dist);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sinkUncertaintyPair.getValue().getInjectionCost()[0], sinkUncertaintyPair.getValue().getInjectionCost()[1] + Double.MIN_VALUE);
//                            distList.add(dist);
                            samplerList.add(dist.createSampler(rng));

//                            distMatrix.add(distList);
                            sinkSamplerMatrix.add(samplerList);
                            break;

                        case "Cauchy":
                            matchedSinkList.add(sinkUncertaintyPair);
                            samplerList = new ArrayList<>();

                            dist = new CauchyDistribution(sinkUncertaintyPair.getValue().getCapacity()[0], sinkUncertaintyPair.getValue().getCapacity()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sinkUncertaintyPair.getValue().getOpeningCost()[0], sinkUncertaintyPair.getValue().getOpeningCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sinkUncertaintyPair.getValue().getOMCost()[0], sinkUncertaintyPair.getValue().getOMCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sinkUncertaintyPair.getValue().getWellCapacity()[0], sinkUncertaintyPair.getValue().getWellCapacity()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sinkUncertaintyPair.getValue().getWellOpeningCost()[0], sinkUncertaintyPair.getValue().getWellOpeningCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sinkUncertaintyPair.getValue().getWellOMCost()[0], sinkUncertaintyPair.getValue().getWellOMCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sinkUncertaintyPair.getValue().getInjectionCost()[0], sinkUncertaintyPair.getValue().getInjectionCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            sinkSamplerMatrix.add(samplerList);
                            break;
                    }
                } catch (NotStrictlyPositiveException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void processSourceUncertainty() {
        UniformRandomProvider rng = RandomSource.create(RandomSource.MT);
        Source[] sources = data.getSources();
        SourceUncertainty[] sourceUncertainties = data.getSourceUncertainties();
        sourceUncertaintyMatch = new HashMap<>();
        for (Source source : sources) {
            sourceUncertaintyMatch.put(source.getLabel(), new Pair<>(source, null));
        }
        for (SourceUncertainty sourceUncertainty : sourceUncertainties) {
            sourceUncertaintyMatch.replace(sourceUncertainty.getLabel(), new Pair<>(sourceUncertaintyMatch.get(sourceUncertainty.getLabel()).getKey(), sourceUncertainty));
        }
        sourceSamplerMatrix = new ArrayList<>();
        matchedSourceList = new ArrayList<>();
        for (Pair<Source, SourceUncertainty> sourceUncertaintyPair : sourceUncertaintyMatch.values()) {
            if (sourceUncertaintyPair.getValue() != null) {
                try {
                    List<RealDistribution.Sampler> samplerList;
                    RealDistribution dist;
                    switch (sourceUncertaintyPair.getValue().getDistribution()) {
                        case "Normal":
                            matchedSourceList.add(sourceUncertaintyPair);
                            samplerList = new ArrayList<>();

                            dist = new NormalDistribution(sourceUncertaintyPair.getValue().getOpeningCost()[0], sourceUncertaintyPair.getValue().getOpeningCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sourceUncertaintyPair.getValue().getOMCost()[0], sourceUncertaintyPair.getValue().getOMCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sourceUncertaintyPair.getValue().getCaptureCost()[0], sourceUncertaintyPair.getValue().getCaptureCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new NormalDistribution(sourceUncertaintyPair.getValue().getProductionRate()[0], sourceUncertaintyPair.getValue().getProductionRate()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            sourceSamplerMatrix.add(samplerList);
                            break;

                        case "Cauchy":
                            matchedSourceList.add(sourceUncertaintyPair);
                            samplerList = new ArrayList<>();

                            dist = new CauchyDistribution(sourceUncertaintyPair.getValue().getOpeningCost()[0], sourceUncertaintyPair.getValue().getOpeningCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sourceUncertaintyPair.getValue().getOMCost()[0], sourceUncertaintyPair.getValue().getOMCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sourceUncertaintyPair.getValue().getCaptureCost()[0], sourceUncertaintyPair.getValue().getCaptureCost()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            dist = new CauchyDistribution(sourceUncertaintyPair.getValue().getProductionRate()[0], sourceUncertaintyPair.getValue().getProductionRate()[1] + Double.MIN_VALUE);
                            samplerList.add(dist.createSampler(rng));

                            sourceSamplerMatrix.add(samplerList);
                            break;
                    }
                } catch (NotStrictlyPositiveException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void sampleUncertainty() {
        //Copy the original to restore later
        double[] sinkCapacity = null;
        double[] sinkOpeningCost = null;
        double[] sinkOMCost = null;
        double[] wellCapacity = null;
        double[] wellOpeningCost = null;
        double[] wellOMCost = null;
        double[] injectionCost = null;

        double[] sourceOpeningCost = null;
        double[] sourceOMCost = null;
        double[] sourceCaptureCost = null;
        double[] sourceProductionRate = null;

        if (sinkExist) {
            sinkCapacity = new double[matchedSinkList.size()];
            sinkOpeningCost = new double[matchedSinkList.size()];
            sinkOMCost = new double[matchedSinkList.size()];
            wellCapacity = new double[matchedSinkList.size()];
            wellOpeningCost = new double[matchedSinkList.size()];
            wellOMCost = new double[matchedSinkList.size()];
            injectionCost = new double[matchedSinkList.size()];
            for (int j = 0; j < matchedSinkList.size(); j++) {
                sinkCapacity[j] = matchedSinkList.get(j).getKey().getCapacity();
                sinkOpeningCost[j] = matchedSinkList.get(j).getKey().getInternalOpeningCost();
                sinkOMCost[j] = matchedSinkList.get(j).getKey().getInternalOMCost();
                wellCapacity[j] = matchedSinkList.get(j).getKey().getWellCapacity();
                wellOpeningCost[j] = matchedSinkList.get(j).getKey().getInternalWellOpeningCost();
                wellOMCost[j] = matchedSinkList.get(j).getKey().getInternalWellOMCost();
                injectionCost[j] = matchedSinkList.get(j).getKey().getInjectionCost();
            }
        }
        if (sourceExist) {
            sourceOpeningCost = new double[matchedSourceList.size()];
            sourceOMCost = new double[matchedSourceList.size()];
            sourceCaptureCost = new double[matchedSourceList.size()];
            sourceProductionRate = new double[matchedSourceList.size()];
            for (int j = 0; j < matchedSourceList.size(); j++) {
                sourceOpeningCost[j] = matchedSourceList.get(j).getKey().getInternalOpeningCost();
                sourceOMCost[j] = matchedSourceList.get(j).getKey().getInternalOMCost();
                sourceCaptureCost[j] = matchedSourceList.get(j).getKey().getCaptureCost();
                sourceProductionRate[j] = matchedSourceList.get(j).getKey().getProductionRate();
            }
        }
        for (int i = 0; i < Integer.parseInt(simulationNumber); i++) {
            System.out.println("Writing MPS File " + i + "...");
            messenger.setText("Writing MPS File " + i + "...");
            if (sinkExist) {
                for (int j = 0; j < matchedSinkList.size(); j++) {
                    matchedSinkList.get(j).getKey().setCapacity(sinkSamplerMatrix.get(j).get(0).sample());
                    matchedSinkList.get(j).getKey().setOpeningCost(sinkSamplerMatrix.get(j).get(1).sample());
                    matchedSinkList.get(j).getKey().setOMCost(sinkSamplerMatrix.get(j).get(2).sample());
                    matchedSinkList.get(j).getKey().setWellCapacity(sinkSamplerMatrix.get(j).get(3).sample());
                    matchedSinkList.get(j).getKey().setWellOpeningCost(sinkSamplerMatrix.get(j).get(4).sample());
                    matchedSinkList.get(j).getKey().setWellOMCost(sinkSamplerMatrix.get(j).get(5).sample());
                    matchedSinkList.get(j).getKey().setInjectionCost(sinkSamplerMatrix.get(j).get(6).sample());
                }
            }
            if (sourceExist) {
                for (int j = 0; j < matchedSourceList.size(); j++) {
                    matchedSourceList.get(j).getKey().setOpeningCost(sourceSamplerMatrix.get(j).get(0).sample());
                    matchedSourceList.get(j).getKey().setOMCost(sourceSamplerMatrix.get(j).get(1).sample());
                    matchedSourceList.get(j).getKey().setCaptureCost(sourceSamplerMatrix.get(j).get(2).sample());
                    matchedSourceList.get(j).getKey().setProductionRate(sourceSamplerMatrix.get(j).get(3).sample());
                }
            }
            MPSWriter.writeMPS("mip" + i + ".mps", data, Double.parseDouble(crf), Double.parseDouble(numYears), Double.parseDouble(capacityTarget), basePath, dataset, scenario);
            if (sinkExist) {
                data.dumpSink2Text(basePath, dataset, scenario, "SampledSinks" + i + ".txt");
            }
            if (sourceExist) {
                data.dumpSource2Text(basePath, dataset, scenario, "SampledSources" + i + ".txt");
            }
            updateProgress(i, Integer.parseInt(simulationNumber));
        }
        System.out.println("Zipping MPS Files" + "...");
        messenger.setText("Zipping MPS Files" + "...");
        MPSWriter.zipFile("mip.zip", basePath, dataset, scenario, simulationNumber);
        System.out.println("Zip File Ready!");
        messenger.setText("Zip File Ready!");
        if (sinkExist) {
            for (int j = 0; j < matchedSinkList.size(); j++) {
                matchedSinkList.get(j).getKey().setCapacity(sinkCapacity[j]);
                matchedSinkList.get(j).getKey().setOpeningCost(sinkOpeningCost[j]);
                matchedSinkList.get(j).getKey().setOMCost(sinkOMCost[j]);
                matchedSinkList.get(j).getKey().setWellCapacity(wellCapacity[j]);
                matchedSinkList.get(j).getKey().setWellOpeningCost(wellOpeningCost[j]);
                matchedSinkList.get(j).getKey().setWellOMCost(wellOMCost[j]);
                matchedSinkList.get(j).getKey().setInjectionCost(injectionCost[j]);
            }
        }
        if (sourceExist) {
            for (int j = 0; j < matchedSourceList.size(); j++) {
                matchedSourceList.get(j).getKey().setOpeningCost(sourceOpeningCost[j]);
                matchedSourceList.get(j).getKey().setOMCost(sourceOMCost[j]);
                matchedSourceList.get(j).getKey().setCaptureCost(sourceCaptureCost[j]);
                matchedSourceList.get(j).getKey().setProductionRate(sourceProductionRate[j]);
            }
        }
    }
}
