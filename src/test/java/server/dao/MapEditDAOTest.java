package server.dao;

import common.Poi;
import common.dto.*;
import org.junit.jupiter.api.*;
import server.DBConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Map Editing DAOs.
 * 
 * IMPORTANT: These tests require the database to be set up with seed data.
 * Run: mysql -u root -p < dummy_db.sql
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MapEditDAOTest {

    private static int testCityId;
    private static int testMapId;
    private static int testPoiId;
    private static int testTourId;

    /**
     * Test 1: Create city + first map
     */
    @Test
    @Order(1)
    @DisplayName("Create city with first map - success")
    void createCityWithFirstMap_success() throws SQLException {
        // Create city
        testCityId = CityDAO.createCity("Test City " + System.currentTimeMillis(),
                "Test description", 25.0);
        assertTrue(testCityId > 0, "City should be created with valid ID");

        // Create first map
        testMapId = MapDAO.createMap(testCityId, "Test Map", "First map for test city");
        assertTrue(testMapId > 0, "Map should be created with valid ID");

        // Verify city has map
        List<MapSummary> maps = MapDAO.getMapsForCity(testCityId);
        assertEquals(1, maps.size(), "City should have exactly one map");
        assertEquals("Test Map", maps.get(0).getName());

        System.out.println("✓ Test 1 passed: Created city " + testCityId + " with map " + testMapId);
    }

    /**
     * Test 2: Add POI then link to tour stop
     */
    @Test
    @Order(2)
    @DisplayName("Add POI and link to tour - success")
    void addPoiAndLinkToTour_success() throws SQLException {
        // Create POI
        Poi poi = new Poi(0, testCityId, "Test POI", "123,456", "Museum", "A test POI", true);
        testPoiId = PoiDAO.createPoi(poi);
        assertTrue(testPoiId > 0, "POI should be created with valid ID");

        // Link POI to map
        try (Connection conn = DBConnector.getConnection()) {
            boolean linked = PoiDAO.linkPoiToMap(conn, testMapId, testPoiId, 1);
            assertTrue(linked, "POI should be linked to map");
        }

        // Verify POI is on map
        List<Poi> pois = PoiDAO.getPoisForMap(testMapId);
        assertTrue(pois.stream().anyMatch(p -> p.getId() == testPoiId),
                "POI should appear in map's POI list");

        // Create tour
        TourDTO tour = new TourDTO(0, testCityId, "Test Tour", "A test tour", 60);
        testTourId = TourDAO.createTour(tour);
        assertTrue(testTourId > 0, "Tour should be created with valid ID");

        // Add POI as tour stop
        TourStopDTO stop = new TourStopDTO();
        stop.setTourId(testTourId);
        stop.setPoiId(testPoiId);
        stop.setStopOrder(1);
        stop.setDurationMinutes(15);
        stop.setNotes("First stop");

        try (Connection conn = DBConnector.getConnection()) {
            int stopId = TourDAO.addTourStop(conn, stop);
            assertTrue(stopId > 0, "Tour stop should be created");
        }

        // Verify tour has stop
        TourDTO loadedTour = TourDAO.getTourById(testTourId);
        assertNotNull(loadedTour, "Tour should be loaded");
        assertEquals(1, loadedTour.getStops().size(), "Tour should have one stop");

        System.out.println("✓ Test 2 passed: Created POI " + testPoiId +
                " and linked to tour " + testTourId);
    }

    /**
     * Test 3: Delete POI that is in a tour (must block)
     */
    @Test
    @Order(3)
    @DisplayName("Delete POI in tour - should be blocked")
    void deletePoiInTour_blocked() {
        // Check if POI is used in tour
        boolean isUsed = PoiDAO.isPoiUsedInTour(testPoiId);
        assertTrue(isUsed, "POI should be detected as used in tour");

        // Try to delete - should throw exception
        try (Connection conn = DBConnector.getConnection()) {
            assertThrows(SQLException.class, () -> {
                PoiDAO.deletePoi(conn, testPoiId);
            }, "Deleting POI in tour should throw SQLException");
        } catch (SQLException e) {
            fail("Should not fail opening connection");
        }

        // Verify POI still exists
        Poi poi = PoiDAO.getPoiById(testPoiId);
        assertNotNull(poi, "POI should still exist after failed delete");

        System.out.println("✓ Test 3 passed: POI delete was correctly blocked");
    }

    /**
     * Test 4: Invalid submission returns validation error
     */
    @Test
    @Order(4)
    @DisplayName("Submit invalid changes - returns validation errors")
    void submitInvalidChanges_returnsErrors() {
        // Create changes with invalid data
        MapChanges changes = new MapChanges();
        changes.setMapId(testMapId);

        // Add POI with empty name (invalid)
        Poi invalidPoi = new Poi(0, testCityId, "", "", "", "", false);
        changes.getAddedPois().add(invalidPoi);

        // Add tour with zero duration (invalid)
        TourDTO invalidTour = new TourDTO(0, testCityId, "", "", 0);
        changes.getAddedTours().add(invalidTour);

        // Try to delete POI that's in tour (should be caught)
        changes.getDeletedPoiIds().add(testPoiId);

        // Validate (this would normally be done in handler)
        ValidationResult result = validateChanges(changes);

        assertFalse(result.isValid(), "Validation should fail");
        assertTrue(result.getErrors().size() >= 1, "Should have at least one error");

        System.out.println("✓ Test 4 passed: Invalid changes returned " +
                result.getErrors().size() + " validation errors");
    }

    /**
     * Cleanup: Delete test data
     */
    @Test
    @Order(5)
    @DisplayName("Cleanup test data")
    void cleanup() throws SQLException {
        // First delete tour (which frees the POI)
        try (Connection conn = DBConnector.getConnection()) {
            TourDAO.deleteTour(conn, testTourId);
        }

        // Now we can delete POI
        try (Connection conn = DBConnector.getConnection()) {
            PoiDAO.deletePoi(conn, testPoiId);
        }

        // Delete map
        MapDAO.deleteMap(testMapId);

        // Note: We don't delete the city as other maps might exist

        System.out.println("✓ Cleanup complete");
    }

    /**
     * Helper: Validate changes (simulates validation logic from handler)
     */
    private ValidationResult validateChanges(MapChanges changes) {
        ValidationResult result = new ValidationResult();

        for (int i = 0; i < changes.getAddedPois().size(); i++) {
            Poi poi = changes.getAddedPois().get(i);
            if (poi.getName() == null || poi.getName().trim().isEmpty()) {
                result.addError("addedPoi[" + i + "].name", "POI name is required");
            }
        }

        for (int i = 0; i < changes.getAddedTours().size(); i++) {
            TourDTO tour = changes.getAddedTours().get(i);
            if (tour.getName() == null || tour.getName().trim().isEmpty()) {
                result.addError("addedTour[" + i + "].name", "Tour name is required");
            }
            if (tour.getEstimatedDurationMinutes() <= 0) {
                result.addError("addedTour[" + i + "].duration", "Duration must be > 0");
            }
        }

        for (int poiId : changes.getDeletedPoiIds()) {
            if (PoiDAO.isPoiUsedInTour(poiId)) {
                result.addError("deletedPoi[" + poiId + "]", "Cannot delete POI - used in tour");
            }
        }

        return result;
    }
}
