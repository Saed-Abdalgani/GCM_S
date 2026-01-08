package common;

import java.io.Serializable;

public class Map implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String description;
    private int cityId;

    public Map(int id, String description, int cityId) {
        this.id = id;
        this.description = description;
        this.cityId = cityId;
    }

    public String toString() {
        return "Map ID: " + id + " | " + description;
    }
}