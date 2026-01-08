package server.dao;

import common.dto.CityDTO;
import server.DBConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for City operations.
 */
public class CityDAO {

    /**
     * Get all cities with map counts for editor.
     */
    public static List<CityDTO> getAllCities() {
        List<CityDTO> cities = new ArrayList<>();

        String query = "SELECT c.id, c.name, c.description, c.price, " +
                "(SELECT COUNT(*) FROM maps WHERE city_id = c.id) as map_count " +
                "FROM cities c ORDER BY c.name";

        try (Connection conn = DBConnector.getConnection()) {
            if (conn == null)
                return cities;

            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                cities.add(new CityDTO(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getInt("map_count")));
            }

            System.out.println("CityDAO: Retrieved " + cities.size() + " cities");

        } catch (SQLException e) {
            System.out.println("CityDAO: Error getting cities");
            e.printStackTrace();
        }

        return cities;
    }

    /**
     * Create a new city.
     * 
     * @return the created city ID, or -1 on failure
     */
    public static int createCity(Connection conn, String name, String description, double price) throws SQLException {
        String query = "INSERT INTO cities (name, description, price) VALUES (?, ?, ?)";

        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, name);
        stmt.setString(2, description);
        stmt.setDouble(3, price);

        int affected = stmt.executeUpdate();

        if (affected > 0) {
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int cityId = keys.getInt(1);
                System.out.println("CityDAO: Created city with ID " + cityId);
                return cityId;
            }
        }

        return -1;
    }

    /**
     * Create a new city (standalone, auto-commits).
     */
    public static int createCity(String name, String description, double price) {
        try (Connection conn = DBConnector.getConnection()) {
            if (conn == null)
                return -1;
            return createCity(conn, name, description, price);
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Update an existing city.
     */
    public static boolean updateCity(int cityId, String name, String description, double price) {
        String query = "UPDATE cities SET name = ?, description = ?, price = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection()) {
            if (conn == null)
                return false;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setDouble(3, price);
            stmt.setInt(4, cityId);

            int affected = stmt.executeUpdate();
            System.out.println("CityDAO: Updated city " + cityId + ", affected: " + affected);
            return affected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get a city by ID.
     */
    public static CityDTO getCityById(int cityId) {
        String query = "SELECT c.id, c.name, c.description, c.price, " +
                "(SELECT COUNT(*) FROM maps WHERE city_id = c.id) as map_count " +
                "FROM cities c WHERE c.id = ?";

        try (Connection conn = DBConnector.getConnection()) {
            if (conn == null)
                return null;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, cityId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new CityDTO(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getInt("map_count"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Check if a city name already exists.
     */
    public static boolean cityNameExists(String name) {
        String query = "SELECT COUNT(*) FROM cities WHERE LOWER(name) = LOWER(?)";

        try (Connection conn = DBConnector.getConnection()) {
            if (conn == null)
                return false;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
