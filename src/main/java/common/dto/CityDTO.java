package common.dto;

import java.io.Serializable;

/**
 * DTO for city data used in map editor.
 * Contains full city information including map count.
 */
public class CityDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String description;
    private double price;
    private int mapCount;

    public CityDTO() {
    }

    public CityDTO(int id, String name, String description, double price, int mapCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.mapCount = mapCount;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getMapCount() {
        return mapCount;
    }

    public void setMapCount(int mapCount) {
        this.mapCount = mapCount;
    }

    @Override
    public String toString() {
        return name + " (" + mapCount + " maps)";
    }
}
