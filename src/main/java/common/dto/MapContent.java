package common.dto;

import common.Poi;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO containing complete map content for editing.
 * Includes map details, all POIs, and all tours.
 */
public class MapContent implements Serializable {
    private static final long serialVersionUID = 1L;

    // Map basic info
    private int mapId;
    private int cityId;
    private String cityName;
    private String mapName;
    private String shortDescription;

    // Map content
    private List<Poi> pois;
    private List<TourDTO> tours;

    // Metadata
    private String createdAt;
    private String updatedAt;

    public MapContent() {
        this.pois = new ArrayList<>();
        this.tours = new ArrayList<>();
    }

    public MapContent(int mapId, int cityId, String cityName, String mapName, String shortDescription) {
        this.mapId = mapId;
        this.cityId = cityId;
        this.cityName = cityName;
        this.mapName = mapName;
        this.shortDescription = shortDescription;
        this.pois = new ArrayList<>();
        this.tours = new ArrayList<>();
    }

    // Add methods
    public void addPoi(Poi poi) {
        this.pois.add(poi);
    }

    public void addTour(TourDTO tour) {
        this.tours.add(tour);
    }

    // Getters and Setters
    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public List<Poi> getPois() {
        return pois;
    }

    public void setPois(List<Poi> pois) {
        this.pois = pois;
    }

    public List<TourDTO> getTours() {
        return tours;
    }

    public void setTours(List<TourDTO> tours) {
        this.tours = tours;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return mapName + " [" + pois.size() + " POIs, " + tours.size() + " Tours]";
    }
}
