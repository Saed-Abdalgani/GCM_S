package common.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for purchase requests.
 */
public class PurchaseRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PurchaseType {
        ONE_TIME,
        SUBSCRIPTION
    }

    private int cityId;
    private PurchaseType purchaseType;
    private int months; // Only for subscription (1-6)

    // Constructor for one-time purchase
    public PurchaseRequest(int cityId) {
        this.cityId = cityId;
        this.purchaseType = PurchaseType.ONE_TIME;
        this.months = 0;
    }

    // Constructor for subscription
    public PurchaseRequest(int cityId, int months) {
        this.cityId = cityId;
        this.purchaseType = PurchaseType.SUBSCRIPTION;
        this.months = months;
    }

    public int getCityId() {
        return cityId;
    }

    public PurchaseType getPurchaseType() {
        return purchaseType;
    }

    public int getMonths() {
        return months;
    }
}
