package common.dto;

import java.io.Serializable;

/**
 * DTO for tour stop data.
 * Represents a single stop in a tour with order and duration.
 */
public class TourStopDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int tourId;
    private int poiId;
    private String poiName; // For display purposes
    private String poiCategory; // For display purposes
    private int stopOrder;
    private int durationMinutes;
    private String notes;

    public TourStopDTO() {
    }

    public TourStopDTO(int id, int tourId, int poiId, String poiName,
            String poiCategory, int stopOrder, int durationMinutes, String notes) {
        this.id = id;
        this.tourId = tourId;
        this.poiId = poiId;
        this.poiName = poiName;
        this.poiCategory = poiCategory;
        this.stopOrder = stopOrder;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
    }

    public int getPoiId() {
        return poiId;
    }

    public void setPoiId(int poiId) {
        this.poiId = poiId;
    }

    public String getPoiName() {
        return poiName;
    }

    public void setPoiName(String poiName) {
        this.poiName = poiName;
    }

    public String getPoiCategory() {
        return poiCategory;
    }

    public void setPoiCategory(String poiCategory) {
        this.poiCategory = poiCategory;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(int stopOrder) {
        this.stopOrder = stopOrder;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return stopOrder + ". " + poiName + " (" + durationMinutes + " min)";
    }
}
