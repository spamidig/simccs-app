package simccs.dataStore;

/**
 *
 * @author yaw
 */
public class SourceUncertainty {
    private String label;
    private String distribution;
    private Double[] openingCost;
    private Double[] omCost;
    private Double[] captureCost;
    private Double[] productionRate;

    private DataStorer data;

    public SourceUncertainty(DataStorer data) {
        this.data = data;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }
    
    public void setOpeningCost(Double[] openingCost) {
        this.openingCost = openingCost;
    }
    
    public void setOMCost(Double[] omCost) {
        this.omCost = omCost;
    }
    
    public void setCaptureCost(Double[] captureCost) {
        this.captureCost = captureCost;
    }
    
    public void setProductionRate(Double[] productionRate) {
        this.productionRate = productionRate;
    }

    public String getLabel() {
        return label;
    }

    public String getDistribution() {
        return distribution;
    }

    public Double[] getOpeningCost() {
        return openingCost;
    }

    public Double[] getOMCost() {
        return omCost;
    }
    
    public Double[] getCaptureCost() {
        return captureCost;
    }

    public Double[] getProductionRate() {
        return productionRate;
    }
}
