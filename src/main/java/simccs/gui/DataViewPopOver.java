package simccs.gui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;
import simccs.dataStore.Sink;
import simccs.dataStore.Solution;
import simccs.dataStore.Source;

import static simccs.utilities.Utilities.round;

public class DataViewPopOver {
    private PopOver popOver;
    private Object object;
    private Node node;
    DataViewPopOver (Node n, Object o) {
        object = o;
        node = n;
        popOver = new PopOver();
        VBox vBox = new VBox();
        if (object instanceof Sink) {
            Label label = new Label("Sink No." + ((Sink) object).getLabel());
            Label capacity = new Label("Capacity: " + ((Sink) object).getCapacity());
            Label openingCost = new Label("Opening Cost: " + ((Sink) object).getInternalOpeningCost());
            Label omCost = new Label("OM Cost: " + ((Sink) object).getInternalOMCost());
            Label wellCapacity = new Label("Well Capacity: " + ((Sink) object).getWellCapacity());
            Label wellOpeningCost = new Label("Well Opening Cost: " + ((Sink) object).getInternalWellOpeningCost());
            Label wellOMCost = new Label("Well OM Cost: " + ((Sink) object).getInternalWellOMCost());
            Label injectionCost = new Label("Injection Cost: " + ((Sink) object).getInjectionCost());
            vBox.getChildren().addAll(label, capacity, openingCost, omCost, wellCapacity, wellOpeningCost, wellOMCost, injectionCost);
            popOver.setTitle("Sink Info");
        } else if (object instanceof Source) {
            Label label = new Label("Source No." + ((Source) object).getLabel());
            Label productionRate = new Label("Production Rate: " + ((Source) object).getProductionRate());
            Label captureCost = new Label("Capture Cost: " + ((Source) object).getCaptureCost());
            vBox.getChildren().addAll(label, productionRate, captureCost);
            popOver.setTitle("Source Info");
        } else if (object instanceof String) {
            String[] elements = ((String) object).split("\\s+");
            switch (elements[0]) {
                case "CandidateNetwork": {
                    Label totalCost = new Label("Total Cost: " + elements[1]);
                    Label constructionCost = new Label("Construction Cost: " + elements[2]);
                    Label rightOfWayCost = new Label("Right of Way Cost: " + elements[3]);
                    vBox.getChildren().addAll(totalCost, constructionCost, rightOfWayCost);
                    popOver.setTitle("Pipeline Info");
                    break;
                }
                case "SolutionNetwork": {
                    Label transportAmount = new Label("Transport Amount: " + elements[1]);
                    Label transportCost = new Label("Transport Cost: " + elements[2]);
                    Label totalCost = new Label("Total Cost: " + elements[3]);
                    Label constructionCost = new Label("Construction Cost: " + elements[4]);
                    Label rightOfWayCost = new Label("Right of Way Cost: " + elements[5]);
                    vBox.getChildren().addAll(totalCost, constructionCost, rightOfWayCost, new Separator(), transportAmount, transportCost);
                    popOver.setTitle("Pipeline Solution");
                    break;
                }
                case "EnsembleSolutionNetwork": {
                    Label popularity = new Label("Popularity: " + elements[1]);
                    Label totalCost = new Label("Total Cost: " + elements[2]);
                    Label constructionCost = new Label("Construction Cost: " + elements[3]);
                    Label rightOfWayCost = new Label("Right of Way Cost: " + elements[4]);
                    vBox.getChildren().addAll(totalCost, constructionCost, rightOfWayCost, new Separator(), popularity);
                    popOver.setTitle("Pipeline Ensemble");
                    break;
                }
            }
        } else if (object instanceof Object[]) {
            if (((Object[]) object)[0] instanceof Sink && ((Object[]) object)[1] instanceof Solution) {
                Label label = new Label("Sink No." + ((Sink) ((Object[]) object)[0]).getLabel());
                Label capacity = new Label("Capacity: " + ((Sink) ((Object[]) object)[0]).getCapacity());
                Label openingCost = new Label("Opening Cost: " + ((Sink) ((Object[]) object)[0]).getInternalOpeningCost());
                Label omCost = new Label("OM Cost: " + ((Sink) ((Object[]) object)[0]).getInternalOMCost());
                Label wellCapacity = new Label("Well Capacity: " + ((Sink) ((Object[]) object)[0]).getWellCapacity());
                Label wellOpeningCost = new Label("Well Opening Cost: " + ((Sink) ((Object[]) object)[0]).getInternalWellOpeningCost());
                Label wellOMCost = new Label("Well OM Cost: " + ((Sink) ((Object[]) object)[0]).getInternalWellOMCost());
                Label injectionCost = new Label("Injection Cost: " + ((Sink) ((Object[]) object)[0]).getInjectionCost());
                Label storageAmounts = new Label("Storage Amount: " + round(((Solution) ((Object[]) object)[1]).getSinkStorageAmounts().get(((Sink) ((Object[]) object)[0])), 3));
                Label sinkCost = new Label("Sink Cost: " + round(((Solution) ((Object[]) object)[1]).getSinkCosts().get(((Sink) ((Object[]) object)[0])), 3));
                Label percentStored = new Label("Percent Stored: " + round(((Solution) ((Object[]) object)[1]).getPercentStored(((Sink) ((Object[]) object)[0])) * 100, 2));
                Label sinkCapacity = new Label("Sampled Capacity: " + round(((Solution) ((Object[]) object)[1]).getSinkCapacity().get((Sink) ((Object[]) object)[0]), 3));
                Label sinkWellCapacity = new Label("Sampled Well Capacity: " + round(((Solution) ((Object[]) object)[1]).getSinkWellCapacity().get((Sink) ((Object[]) object)[0]), 3));
                vBox.getChildren().addAll(label, capacity, openingCost, omCost, wellCapacity, wellOpeningCost,
                        wellOMCost, injectionCost, new Separator(), storageAmounts, sinkCost, percentStored, sinkCapacity, sinkWellCapacity);
                popOver.setTitle("Sink Solution");
            } else if (((Object[]) object)[0] instanceof Source && ((Object[]) object)[1] instanceof Solution) {
                Label label = new Label("Source No." + ((Source) ((Object[]) object)[0]).getLabel());
                Label productionRate = new Label("Production Rate: " + ((Source) ((Object[]) object)[0]).getProductionRate());
                Label captureCost = new Label("Capture Cost: " + ((Source) ((Object[]) object)[0]).getCaptureCost());
                Label captureAmounts = new Label("Capture Amount: " + round(((Solution) ((Object[]) object)[1]).getSourceCaptureAmounts().get(((Source) ((Object[]) object)[0])), 3));
                Label sourceCost = new Label("Source Cost: " + round(((Solution) ((Object[]) object)[1]).getSourceCosts().get(((Source) ((Object[]) object)[0])), 3));
                Label percentCaptured = new Label("Percent Captured: " + round(((Solution) ((Object[]) object)[1]).getPercentCaptured(((Source) ((Object[]) object)[0])) * 100, 2));
                Label sourceProductionRate = new Label("Sampled Production Rate: " + round(((Solution) ((Object[]) object)[1]).getSourceProductionRate().get((Source) ((Object[]) object)[0]), 3));
                vBox.getChildren().addAll(label, productionRate, captureCost, new Separator(), captureAmounts, sourceCost, percentCaptured, sourceProductionRate);
                popOver.setTitle("Source Solution");
            } else if (((Object[]) object)[0] instanceof Sink && ((Object[]) object)[1] instanceof Integer) {
                Label label = new Label("Sink No." + ((Sink) ((Object[]) object)[0]).getLabel());
                Label capacity = new Label("Capacity: " + ((Sink) ((Object[]) object)[0]).getCapacity());
                Label openingCost = new Label("Opening Cost: " + ((Sink) ((Object[]) object)[0]).getInternalOpeningCost());
                Label omCost = new Label("OM Cost: " + ((Sink) ((Object[]) object)[0]).getInternalOMCost());
                Label wellCapacity = new Label("Well Capacity: " + ((Sink) ((Object[]) object)[0]).getWellCapacity());
                Label wellOpeningCost = new Label("Well Opening Cost: " + ((Sink) ((Object[]) object)[0]).getInternalWellOpeningCost());
                Label wellOMCost = new Label("Well OM Cost: " + ((Sink) ((Object[]) object)[0]).getInternalWellOMCost());
                Label injectionCost = new Label("Injection Cost: " + ((Sink) ((Object[]) object)[0]).getInjectionCost());
                Label popularity = new Label("Popularity: " + ((Object[]) object)[1]);
                vBox.getChildren().addAll(label, capacity, openingCost, omCost, wellCapacity, wellOpeningCost, wellOMCost, injectionCost, new Separator(), popularity);
                popOver.setTitle("Sink Ensemble");
            } else if (((Object[]) object)[0] instanceof Source && ((Object[]) object)[1] instanceof Integer) {
                Label label = new Label("Source No." + ((Source) ((Object[]) object)[0]).getLabel());
                Label productionRate = new Label("Production Rate: " + ((Source) ((Object[]) object)[0]).getProductionRate());
                Label captureCost = new Label("Capture Cost: " + ((Source) ((Object[]) object)[0]).getCaptureCost());
                Label popularity = new Label("Popularity: " + ((Object[]) object)[1]);
                vBox.getChildren().addAll(label, productionRate, captureCost, new Separator(), popularity);
                popOver.setTitle("Source Ensemble");
            }
        }
        popOver.setContentNode(vBox);
        popOver.setOpacity(0.85);
    }

    public void show (){
        popOver.show(node);
    }
}
