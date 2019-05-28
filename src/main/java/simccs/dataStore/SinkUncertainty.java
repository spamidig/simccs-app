package simccs.dataStore;

/**
 *
 * @author yaw
 */
public class SinkUncertainty {
    private String label;
    private String distribution;
    private Double[] capacity;
    private Double[] openingCost;
    private Double[] omCost;
    private Double[] wellCapacity;
    private Double[] wellOpeningCost;
    private Double[] wellOMCost;
    private Double[] injectionCost;

    private DataStorer data;

    public SinkUncertainty(DataStorer data) {
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
    
    public void setWellOpeningCost(Double[] wellOpeningCost) {
        this.wellOpeningCost = wellOpeningCost;
    }
    
    public void setWellOMCost(Double[] wellOMCost) {
        this.wellOMCost = wellOMCost;
    }
    
    public void setInjectionCost(Double[] injectionCost) {
        this.injectionCost = injectionCost;
    }
    
    public void setWellCapacity(Double[] wellCapacity) {
        this.wellCapacity = wellCapacity;
    }
    
    public void setCapacity(Double[] capacity) {
        this.capacity = capacity;
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
    
    public Double[] getWellOpeningCost() {
        return wellOpeningCost;
    }

    public Double[] getWellOMCost() {
        return wellOMCost;
    }
    
    public Double[] getInjectionCost() {
        return injectionCost;
    }
    
    public Double[] getWellCapacity() {
        return wellCapacity;
    }
    
    public Double[] getCapacity() {
        return capacity;
    }
}
