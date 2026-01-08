package client.boundary;

import client.control.ContentManagementControl;
import common.Poi;
import common.dto.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Controller for the Map Editor screen.
 * Handles all map editing operations including POIs and Tours.
 */
public class MapEditorScreen implements ContentManagementControl.ContentCallback {

    // Header
    @FXML
    private Label statusLabel;
    @FXML
    private Button backButton;

    // Left panel - City/Map selection
    @FXML
    private ComboBox<CityDTO> cityComboBox;
    @FXML
    private TextField cityPriceField;
    @FXML
    private ListView<MapSummary> mapsListView;
    @FXML
    private Button createCityBtn;
    @FXML
    private Button createMapBtn;

    // Center - Tabs
    @FXML
    private TabPane contentTabs;

    // POIs Tab
    @FXML
    private ListView<Poi> poisListView;
    @FXML
    private VBox poiEditForm;
    @FXML
    private TextField poiNameField;
    @FXML
    private ComboBox<String> poiCategoryCombo;
    @FXML
    private TextField poiLocationField;
    @FXML
    private CheckBox poiAccessibleCheck;
    @FXML
    private TextArea poiDescArea;

    // Tours Tab
    @FXML
    private ListView<TourDTO> toursListView;
    @FXML
    private VBox tourStopsSection;
    @FXML
    private Label tourNameLabel;
    @FXML
    private ListView<TourStopDTO> tourStopsListView;
    @FXML
    private VBox tourEditForm;
    @FXML
    private TextField tourNameField;
    @FXML
    private TextField tourDurationField;
    @FXML
    private TextArea tourDescArea;

    // Map Info Tab
    @FXML
    private TextField mapNameField;
    @FXML
    private TextArea mapDescArea;

    // State
    private ContentManagementControl control;
    private CityDTO selectedCity;
    private MapSummary selectedMap;
    private MapContent currentMapContent;
    private Poi editingPoi;
    private TourDTO editingTour;
    private boolean hasUnsavedChanges = false;

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        System.out.println("MapEditorScreen: Initializing");

        // Setup POI categories
        poiCategoryCombo.setItems(FXCollections.observableArrayList(
                "Museum", "Beach", "Historic", "Religious", "Park", "Shopping",
                "Restaurant", "Entertainment", "Cultural", "Nature", "Other"));

        // Connect to server
        try {
            control = new ContentManagementControl("localhost", 5555);
            control.setCallback(this);

            // Load cities
            control.getCities();

        } catch (IOException e) {
            showError("Failed to connect to server");
            e.printStackTrace();
        }

        // City selection listener
        cityComboBox.setOnAction(e -> {
            selectedCity = cityComboBox.getValue();
            if (selectedCity != null) {
                cityPriceField.setText(String.valueOf(selectedCity.getPrice()));
                control.getMapsForCity(selectedCity.getId());
            }
        });

        // Map selection listener
        mapsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newMap) -> {
            selectedMap = newMap;
            if (selectedMap != null) {
                control.getMapContent(selectedMap.getId());
            }
        });

        // POI selection listener
        poisListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newPoi) -> {
            if (newPoi != null) {
                showPoiEditForm(newPoi);
            }
        });

        // Tour selection listener
        toursListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newTour) -> {
            if (newTour != null) {
                showTourDetails(newTour);
            }
        });

        // Custom cell factories
        setupCellFactories();

        setStatus("Ready - Select a city to begin");
    }

    private void setupCellFactories() {
        // City combo display
        cityComboBox.setButtonCell(new ListCell<CityDTO>() {
            @Override
            protected void updateItem(CityDTO city, boolean empty) {
                super.updateItem(city, empty);
                setText(empty || city == null ? "" : city.getName() + " (" + city.getMapCount() + " maps)");
            }
        });
        cityComboBox.setCellFactory(lv -> new ListCell<CityDTO>() {
            @Override
            protected void updateItem(CityDTO city, boolean empty) {
                super.updateItem(city, empty);
                setText(empty || city == null ? "" : city.getName() + " (" + city.getMapCount() + " maps)");
            }
        });

        // Maps list
        mapsListView.setCellFactory(lv -> new ListCell<MapSummary>() {
            @Override
            protected void updateItem(MapSummary map, boolean empty) {
                super.updateItem(map, empty);
                if (empty || map == null) {
                    setText(null);
                    setStyle("-fx-text-fill: white;");
                } else {
                    setText(map.getName() + " [" + map.getPoiCount() + " POIs]");
                    setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                }
            }
        });

        // POIs list
        poisListView.setCellFactory(lv -> new ListCell<Poi>() {
            @Override
            protected void updateItem(Poi poi, boolean empty) {
                super.updateItem(poi, empty);
                if (empty || poi == null) {
                    setText(null);
                } else {
                    setText("📍 " + poi.getName() + " [" + poi.getCategory() + "]" +
                            (poi.isAccessible() ? " ♿" : ""));
                    setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                }
            }
        });

        // Tours list
        toursListView.setCellFactory(lv -> new ListCell<TourDTO>() {
            @Override
            protected void updateItem(TourDTO tour, boolean empty) {
                super.updateItem(tour, empty);
                if (empty || tour == null) {
                    setText(null);
                } else {
                    setText("🚶 " + tour.getName() + " (" + tour.getStops().size() + " stops, ~" +
                            tour.getEstimatedDurationMinutes() + " min)");
                    setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                }
            }
        });

        // Tour stops list
        tourStopsListView.setCellFactory(lv -> new ListCell<TourStopDTO>() {
            @Override
            protected void updateItem(TourStopDTO stop, boolean empty) {
                super.updateItem(stop, empty);
                if (empty || stop == null) {
                    setText(null);
                } else {
                    setText(stop.getStopOrder() + ". " + stop.getPoiName() + " (" +
                            stop.getDurationMinutes() + " min)");
                    setStyle("-fx-text-fill: #9b59b6; -fx-font-size: 12px;");
                }
            }
        });
    }

    // ==================== City Operations ====================

    @FXML
    private void handleCreateCity() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create City");
        dialog.setHeaderText("Create a new city");
        dialog.setContentText("City name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                control.createCity(name.trim(), "New city description", 50.0);
                setStatus("Creating city...");
            }
        });
    }

    @FXML
    private void handleUpdatePrice() {
        if (selectedCity == null) {
            showError("Please select a city first");
            return;
        }

        try {
            double price = Double.parseDouble(cityPriceField.getText().trim());
            if (price < 0) {
                showError("Price must be non-negative");
                return;
            }

            // Update local object
            selectedCity.setPrice(price);

            // Send update to server
            control.updateCity(selectedCity);
            setStatus("Updating price...");

        } catch (NumberFormatException e) {
            showError("Invalid price format");
        }
    }

    // ==================== Map Operations ====================

    @FXML
    private void handleCreateMap() {
        if (selectedCity == null) {
            showError("Please select a city first");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Map");
        dialog.setHeaderText("Create a new map for " + selectedCity.getName());
        dialog.setContentText("Map name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                control.createMap(selectedCity.getId(), name.trim(), "New map description");
                setStatus("Creating map...");
            }
        });
    }

    @FXML
    private void handleSaveMapInfo() {
        if (currentMapContent == null) {
            showError("No map selected");
            return;
        }

        MapChanges changes = new MapChanges();
        changes.setMapId(currentMapContent.getMapId());
        changes.setNewMapName(mapNameField.getText().trim());
        changes.setNewMapDescription(mapDescArea.getText().trim());

        control.submitMapChanges(changes);
        setStatus("Saving map info...");
    }

    // ==================== POI Operations ====================

    @FXML
    private void handleAddPoi() {
        if (selectedCity == null) {
            showError("Please select a city first");
            return;
        }

        editingPoi = new Poi(0, selectedCity.getId(), "", "", "", "", true);
        showPoiEditForm(editingPoi);
    }

    private void showPoiEditForm(Poi poi) {
        editingPoi = poi;
        poiNameField.setText(poi.getName());
        poiCategoryCombo.setValue(poi.getCategory());
        poiLocationField.setText(poi.getLocation());
        poiAccessibleCheck.setSelected(poi.isAccessible());
        poiDescArea.setText(poi.getShortExplanation());

        poiEditForm.setVisible(true);
        poiEditForm.setManaged(true);
    }

    @FXML
    private void handleCancelPoiEdit() {
        poiEditForm.setVisible(false);
        poiEditForm.setManaged(false);
        editingPoi = null;
    }

    @FXML
    private void handleSavePoi() {
        if (editingPoi == null)
            return;

        if (poiNameField.getText().trim().isEmpty()) {
            showError("POI name is required");
            return;
        }

        editingPoi.setName(poiNameField.getText().trim());
        editingPoi.setCategory(poiCategoryCombo.getValue() != null ? poiCategoryCombo.getValue() : "Other");
        editingPoi.setLocation(poiLocationField.getText().trim());
        editingPoi.setAccessible(poiAccessibleCheck.isSelected());
        editingPoi.setShortExplanation(poiDescArea.getText().trim());

        if (editingPoi.getId() == 0) {
            control.addPoi(editingPoi);
        } else {
            control.updatePoi(editingPoi);
        }

        handleCancelPoiEdit();
        setStatus("Saving POI...");
    }

    @FXML
    private void handleDeletePoi() {
        if (editingPoi == null || editingPoi.getId() == 0)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete POI");
        confirm.setHeaderText("Delete " + editingPoi.getName() + "?");
        confirm.setContentText("This cannot be undone. POIs used in tours cannot be deleted.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                control.deletePoi(editingPoi.getId());
                handleCancelPoiEdit();
                setStatus("Deleting POI...");
            }
        });
    }

    // ==================== Tour Operations ====================

    @FXML
    private void handleAddTour() {
        if (selectedCity == null) {
            showError("Please select a city first");
            return;
        }

        editingTour = new TourDTO(0, selectedCity.getId(), "", "", 60);
        showTourEditForm(editingTour);
    }

    private void showTourDetails(TourDTO tour) {
        tourNameLabel.setText("Stops for: " + tour.getName());
        tourStopsListView.setItems(FXCollections.observableArrayList(tour.getStops()));

        tourStopsSection.setVisible(true);
        tourStopsSection.setManaged(true);
    }

    private void showTourEditForm(TourDTO tour) {
        editingTour = tour;
        tourNameField.setText(tour.getName());
        tourDurationField.setText(String.valueOf(tour.getEstimatedDurationMinutes()));
        tourDescArea.setText(tour.getDescription());

        tourEditForm.setVisible(true);
        tourEditForm.setManaged(true);
    }

    @FXML
    private void handleCancelTourEdit() {
        tourEditForm.setVisible(false);
        tourEditForm.setManaged(false);
        editingTour = null;
    }

    @FXML
    private void handleSaveTour() {
        if (editingTour == null)
            return;

        if (tourNameField.getText().trim().isEmpty()) {
            showError("Tour name is required");
            return;
        }

        try {
            int duration = Integer.parseInt(tourDurationField.getText().trim());
            if (duration <= 0) {
                showError("Duration must be greater than 0");
                return;
            }
            editingTour.setEstimatedDurationMinutes(duration);
        } catch (NumberFormatException e) {
            showError("Invalid duration");
            return;
        }

        editingTour.setName(tourNameField.getText().trim());
        editingTour.setDescription(tourDescArea.getText().trim());

        if (editingTour.getId() == 0) {
            control.createTour(editingTour);
        } else {
            control.updateTour(editingTour);
        }

        handleCancelTourEdit();
        setStatus("Saving tour...");
    }

    @FXML
    private void handleDeleteTour() {
        if (editingTour == null || editingTour.getId() == 0)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Tour");
        confirm.setHeaderText("Delete " + editingTour.getName() + "?");
        confirm.setContentText("This will also delete all tour stops.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                control.deleteTour(editingTour.getId());
                handleCancelTourEdit();
                setStatus("Deleting tour...");
            }
        });
    }

    @FXML
    private void handleAddTourStop() {
        TourDTO selectedTour = toursListView.getSelectionModel().getSelectedItem();
        if (selectedTour == null) {
            showError("Please select a tour first");
            return;
        }

        // Show POI selection dialog
        if (currentMapContent == null || currentMapContent.getPois().isEmpty()) {
            showError("No POIs available. Add POIs first.");
            return;
        }

        ChoiceDialog<Poi> dialog = new ChoiceDialog<>(
                currentMapContent.getPois().get(0),
                currentMapContent.getPois());
        dialog.setTitle("Add Tour Stop");
        dialog.setHeaderText("Select POI to add to tour");
        dialog.setContentText("POI:");

        dialog.showAndWait().ifPresent(poi -> {
            TourStopDTO stop = new TourStopDTO();
            stop.setTourId(selectedTour.getId());
            stop.setPoiId(poi.getId());
            stop.setStopOrder(selectedTour.getStops().size() + 1);
            stop.setDurationMinutes(15);
            stop.setNotes("");

            control.addTourStop(stop);
            setStatus("Adding tour stop...");
        });
    }

    // ==================== Batch Operations ====================

    @FXML
    private void handleSaveAllChanges() {
        if (currentMapContent == null) {
            showError("No map selected. Please select a map first.");
            return;
        }

        // Create a MapChanges object with the current map info
        MapChanges changes = new MapChanges();
        changes.setMapId(currentMapContent.getMapId());
        changes.setCityId(selectedCity != null ? selectedCity.getId() : null);

        // Set map name and description if they were modified
        if (mapNameField.getText() != null && !mapNameField.getText().isEmpty()) {
            changes.setNewMapName(mapNameField.getText().trim());
        }
        if (mapDescArea.getText() != null) {
            changes.setNewMapDescription(mapDescArea.getText().trim());
        }

        // Submit changes to server (creates PENDING version for approval)
        control.submitMapChanges(changes);
        setStatus("Submitting changes for approval...");
        hasUnsavedChanges = false;
    }

    @FXML
    private void handleDiscardChanges() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Discard Changes");
        confirm.setHeaderText("Discard all changes?");
        confirm.setContentText("All unsaved changes will be lost.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (currentMapContent != null) {
                    control.getMapContent(currentMapContent.getMapId());
                }
                hasUnsavedChanges = false;
                setStatus("Changes discarded - reloaded from server");
            }
        });
    }

    // ==================== Navigation ====================

    @FXML
    private void handleBack() {
        if (hasUnsavedChanges) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Unsaved Changes");
            confirm.setHeaderText("You have unsaved changes");
            confirm.setContentText("Discard changes and go back?");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        if (control != null) {
            control.disconnect(); // This now only clears the handler
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GCM Dashboard");
            stage.setWidth(1000);
            stage.setHeight(700);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== Callbacks ====================

    @Override
    public void onCitiesReceived(List<CityDTO> cities) {
        Platform.runLater(() -> {
            cityComboBox.setItems(FXCollections.observableArrayList(cities));
            setStatus("Loaded " + cities.size() + " cities");
        });
    }

    @Override
    public void onMapsReceived(List<MapSummary> maps) {
        Platform.runLater(() -> {
            mapsListView.setItems(FXCollections.observableArrayList(maps));
            setStatus("Loaded " + maps.size() + " maps for " + selectedCity.getName());
        });
    }

    @Override
    public void onMapContentReceived(MapContent content) {
        Platform.runLater(() -> {
            currentMapContent = content;

            // Update POIs tab
            poisListView.setItems(FXCollections.observableArrayList(content.getPois()));

            // Update Tours tab
            toursListView.setItems(FXCollections.observableArrayList(content.getTours()));

            // Update Map Info tab
            mapNameField.setText(content.getMapName());
            mapDescArea.setText(content.getShortDescription());

            setStatus("Editing: " + content.getMapName());
        });
    }

    @Override
    public void onValidationResult(ValidationResult result) {
        Platform.runLater(() -> {
            if (result.isValid()) {
                setStatus("✓ " + result.getSuccessMessage());

                // Refresh data
                if (selectedCity != null) {
                    control.getMapsForCity(selectedCity.getId());
                }
                if (currentMapContent != null) {
                    control.getMapContent(currentMapContent.getMapId());
                }
                control.getCities();
            } else {
                showError(result.getErrorSummary());
            }
        });
    }

    @Override
    public void onError(String errorCode, String errorMessage) {
        Platform.runLater(() -> {
            showError(errorCode + ": " + errorMessage);
        });
    }

    // ==================== Helpers ====================

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setText("⚠️ " + message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");

        // Reset style after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            Platform.runLater(() -> statusLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8);"));
        }).start();
    }
}
