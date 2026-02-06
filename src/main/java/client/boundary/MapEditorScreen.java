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
    private int pendingSelectCityId = -1;
    private MapChanges pendingChanges = new MapChanges(); // Collect changes locally

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
        cityComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("DEBUG: City selection changed from " + (oldVal != null ? oldVal.getName() : "null") +
                    " to " + (newVal != null ? newVal.getName() : "null"));

            selectedCity = newVal;
            if (selectedCity != null) {
                System.out.println(
                        "DEBUG: Selected city ID: " + selectedCity.getId() + ", Maps: " + selectedCity.getMapCount());
                control.getMapsForCity(selectedCity.getId());

                // Force UI state update
                createMapBtn.setDisable(false);
                setStatus("Selected: " + selectedCity.getName());
            } else {
                System.out.println("DEBUG: Selection cleared");
                createMapBtn.setDisable(true);
                mapsListView.getItems().clear();
                setStatus("No city selected");
            }
        });

        // Keep the ActionEvent handler just in case, but rely on listener
        cityComboBox.setOnAction(e -> {
            System.out.println("DEBUG: ComboBox ActionEvent triggered");
        });

        // Map selection listener
        mapsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newMap) -> {
            selectedMap = newMap;
            if (selectedMap != null) {
                control.getMapContent(selectedMap.getId());
            } else {
                clearMapContent();
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

    private void clearMapContent() {
        currentMapContent = null;

        // Clear Lists
        poisListView.getItems().clear();
        toursListView.getItems().clear();
        tourStopsListView.getItems().clear();

        // Clear Fields
        mapNameField.clear();
        mapDescArea.clear();

        // Clear Forms
        handleCancelPoiEdit();
        handleCancelTourEdit();

        // Hide/Reset Info
        tourStopsSection.setVisible(false);

        setStatus("No map selected");
    }

    private void setupCellFactories() {
        // City combo display
        cityComboBox.setButtonCell(new ListCell<CityDTO>() {
            @Override
            protected void updateItem(CityDTO city, boolean empty) {
                super.updateItem(city, empty);
                if (empty || city == null) {
                    setText(null);
                    System.out.println("DEBUG: ButtonCell update - empty/null");
                } else {
                    String text = city.getName() + " (" + city.getMapCount() + " maps)";
                    setText(text);
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                    System.out.println("DEBUG: ButtonCell update - " + text);
                }
            }
        });
        cityComboBox.setCellFactory(lv -> new ListCell<CityDTO>() {
            @Override
            protected void updateItem(CityDTO city, boolean empty) {
                super.updateItem(city, empty);
                if (empty || city == null) {
                    setText(null);
                } else {
                    setText(city.getName() + " (" + city.getMapCount() + " maps)");
                    // Dropdown list has white background, so use black text
                    setStyle("-fx-text-fill: black; -fx-font-size: 14px;");
                }
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
                // Add to pending changes instead of creating immediately
                pendingChanges.withNewCity(name.trim(), "New city description", 50.0);
                hasUnsavedChanges = true;
                setStatus("City creation added to pending changes. Click 'Save All Changes' to submit for approval.");
            }
        });
    }

    // ==================== Map Operations ====================

    @FXML
    private void handleCreateMap() {
        System.out.println("DEBUG: handleCreateMap called. SelectedCity: "
                + (selectedCity != null ? selectedCity.getName() : "null"));
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
                // Add to pending changes instead of creating immediately
                pendingChanges.setCityId(selectedCity.getId());
                pendingChanges.setNewMapName(name.trim());
                pendingChanges.setNewMapDescription("New map description");
                hasUnsavedChanges = true;
                setStatus("Map creation added to pending changes. Click 'Save All Changes' to submit for approval.");
            }
        });
    }

    @FXML
    private void handleSaveMapInfo() {
        if (currentMapContent == null) {
            showError("No map selected");
            return;
        }

        // Add to pending changes instead of sending directly
        pendingChanges.setMapId(currentMapContent.getMapId());
        pendingChanges.setNewMapName(mapNameField.getText().trim());
        pendingChanges.setNewMapDescription(mapDescArea.getText().trim());
        hasUnsavedChanges = true;
        setStatus("Map info added to pending changes. Click 'Save All Changes' to submit for approval.");
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

        // Add to pending changes instead of sending immediately
        if (editingPoi.getId() == 0) {
            pendingChanges.addPoi(editingPoi);
            setStatus("POI added to pending changes. Click 'Save All Changes' to submit.");
        } else {
            pendingChanges.updatePoi(editingPoi);
            setStatus("POI updated in pending changes. Click 'Save All Changes' to submit.");
        }

        hasUnsavedChanges = true;
        handleCancelPoiEdit();
    }

    @FXML
    private void handleDeletePoi() {
        if (editingPoi == null || editingPoi.getId() == 0)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete POI");
        confirm.setHeaderText("Delete " + editingPoi.getName() + "?");
        confirm.setContentText("This will be submitted for manager approval.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                pendingChanges.deletePoi(editingPoi.getId());
                handleCancelPoiEdit();
                hasUnsavedChanges = true;
                setStatus("POI deletion added to pending changes. Click 'Save All Changes' to submit.");
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

        // Add to pending changes instead of sending immediately
        if (editingTour.getId() == 0) {
            pendingChanges.addTour(editingTour);
            setStatus("Tour added to pending changes. Click 'Save All Changes' to submit.");
        } else {
            pendingChanges.updateTour(editingTour);
            setStatus("Tour updated in pending changes. Click 'Save All Changes' to submit.");
        }

        hasUnsavedChanges = true;
        handleCancelTourEdit();
    }

    @FXML
    private void handleDeleteTour() {
        if (editingTour == null || editingTour.getId() == 0)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Tour");
        confirm.setHeaderText("Delete " + editingTour.getName() + "?");
        confirm.setContentText("This will be submitted for manager approval.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                pendingChanges.deleteTour(editingTour.getId());
                handleCancelTourEdit();
                hasUnsavedChanges = true;
                setStatus("Tour deletion added to pending changes. Click 'Save All Changes' to submit.");
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
        // Allow saving if we have either a map or a city selected, or pending changes
        // exist
        if (currentMapContent == null && selectedCity == null && !pendingChanges.hasChanges()) {
            showError("Please select a city or map first, or make some changes.");
            return;
        }

        // Set map and city IDs on pendingChanges
        if (currentMapContent != null) {
            pendingChanges.setMapId(currentMapContent.getMapId());
        }

        if (selectedCity != null) {
            pendingChanges.setCityId(selectedCity.getId());
        }

        // Set map name and description if they were modified
        if (mapNameField.getText() != null && !mapNameField.getText().isEmpty()) {
            pendingChanges.setNewMapName(mapNameField.getText().trim());
        }
        if (mapDescArea.getText() != null) {
            pendingChanges.setNewMapDescription(mapDescArea.getText().trim());
        }

        // Submit all pending changes to server (creates PENDING request for manager
        // approval)
        control.submitMapChanges(pendingChanges);
        setStatus("Submitting changes for manager approval...");

        // Reset pending changes
        pendingChanges = new MapChanges();
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
            // Save current selection ID if no pending selection
            int targetId = pendingSelectCityId;
            if (targetId == -1 && selectedCity != null) {
                targetId = selectedCity.getId();
            }

            // Store the items
            cityComboBox.setItems(FXCollections.observableArrayList(cities));

            // Restore selection
            CityDTO cityToSelect = null;
            if (targetId != -1) {
                for (CityDTO c : cities) {
                    if (c.getId() == targetId) {
                        cityToSelect = c;
                        break;
                    }
                }
            }

            if (cityToSelect != null) {
                // Use setValue instead of select to ensure ButtonCell updates
                cityComboBox.setValue(cityToSelect);
                selectedCity = cityToSelect;
                control.getMapsForCity(selectedCity.getId());
                createMapBtn.setDisable(false);
            } else {
                // No city to select - clear value explicitly
                cityComboBox.setValue(null);
                selectedCity = null;
                createMapBtn.setDisable(true);
            }

            pendingSelectCityId = -1; // Reset
            setStatus("Loaded " + cities.size() + " cities");
        });
    }

    @Override
    public void onMapsReceived(List<MapSummary> maps) {
        Platform.runLater(() -> {
            mapsListView.setItems(FXCollections.observableArrayList(maps));
            if (selectedCity != null) {
                setStatus("Loaded " + maps.size() + " maps for " + selectedCity.getName());
            }
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

                // Auto-select newly created city
                if (result.getCreatedCityId() != null && result.getCreatedCityId() > 0) {
                    pendingSelectCityId = result.getCreatedCityId();
                }

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

    @Override
    public void onPendingRequestsReceived(List<MapEditRequestDTO> requests) {
        // Not used in this screen
    }

    // ==================== Helpers ====================

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setText("⚠️ " + message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");

        // Use proper JavaFX timer instead of Thread.sleep (Phase 14)
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(3000));
        pause.setOnFinished(e -> statusLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8);"));
        pause.play();
    }
}
