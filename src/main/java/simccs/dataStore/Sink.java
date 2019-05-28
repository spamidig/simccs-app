package simccs.dataStore;

/**
 *
 * @author yaw
 */
public class Sink {
    private int cellNum;
    private String label;
    private double capacity;
    private double openingCost;
    private double omCost;
    private double wellCapacity;
    private double wellOpeningCost;
    private double wellOMCost;
    private double injectionCost;
    private boolean disabled = false;
    
    private DataStorer data;
    
    public Sink(DataStorer data) {
        this.data = data;
    }
    
    public void setCellNum(int cellNum) {
        this.cellNum = cellNum;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public void setOpeningCost(double openingCost) {
        this.openingCost = openingCost;
    }
    
    public void setOMCost(double omCost) {
        this.omCost = omCost;
    }
    
    public void setWellOpeningCost(double wellOpeningCost) {
        this.wellOpeningCost = wellOpeningCost;
    }
    
    public void setWellOMCost(double wellOMCost) {
        this.wellOMCost = wellOMCost;
    }
    
    public void setInjectionCost(double injectionCost) {
        this.injectionCost = injectionCost;
    }
    
    public void setWellCapacity(double wellCapacity) {
        this.wellCapacity = wellCapacity;
    }
    
    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public int getCellNum() {
        return cellNum;
    }
    
    public String getLabel() {
        return label;
    }
    
    public double getOpeningCost(double crf) {
        return crf * openingCost + omCost;
    }

    public double getInternalOpeningCost() {
        return openingCost;
    }

    public double getInternalOMCost() {
        return omCost;
    }
    
    public double getWellOpeningCost(double crf) {
        return crf * wellOpeningCost + wellOMCost;
    }

    public double getInternalWellOpeningCost() {
        return wellOpeningCost;
    }

    public double getInternalWellOMCost() {
        return wellOMCost;
    }
    
    public double getInjectionCost() {
        return injectionCost;
    }
    
    public double getWellCapacity() {
        return wellCapacity;
    }
    
    public double getCapacity() {
        return capacity;
    }

    public boolean isDisabled(){return disabled;}

    public void disable(){disabled = true;}

    public void enable(){disabled = false;}
}
