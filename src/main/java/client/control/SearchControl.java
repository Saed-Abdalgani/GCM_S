package client.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import client.GCMClient;
import common.MessageType;
import common.Request;
import common.Response;
import common.dto.CitySearchResult;
import common.dto.SearchRequest;

/**
 * Client-side controller for search operations.
 * Manages communication with server for all search-related functionality.
 */
public class SearchControl implements GCMClient.MessageHandler {

    /** Callback for displaying search results */
    private SearchResultCallback resultCallback;
    private GCMClient client;

    /**
     * Callback interface for search results.
     */
    public interface SearchResultCallback {
        void onSearchResults(List<CitySearchResult> results);

        void onError(String errorCode, String errorMessage);
    }

    public SearchControl(String host, int port) throws IOException {
        // Host/port ignored as we use singleton
        client = GCMClient.getInstance();
        client.setMessageHandler(this);
        System.out.println("SearchControl: Connected via Singleton Client");
    }

    /**
     * Set the callback for receiving search results.
     */
    public void setResultCallback(SearchResultCallback callback) {
        this.resultCallback = callback;
    }

    /**
     * Get the full cities catalog.
     */
    public void getCatalog() {
        Request request = new Request(MessageType.GET_CITIES_CATALOG);
        sendRequest(request);
    }

    /**
     * Search by city name.
     */
    public void searchByCityName(String cityName) {
        SearchRequest searchReq = SearchRequest.byCity(cityName);
        Request request = new Request(MessageType.SEARCH_BY_CITY_NAME, searchReq);
        sendRequest(request);
    }

    /**
     * Search by POI name.
     */
    public void searchByPoiName(String poiName) {
        SearchRequest searchReq = SearchRequest.byPoi(poiName);
        Request request = new Request(MessageType.SEARCH_BY_POI_NAME, searchReq);
        sendRequest(request);
    }

    /**
     * Search by both city and POI name.
     */
    public void searchByCityAndPoi(String cityName, String poiName) {
        SearchRequest searchReq = SearchRequest.byCityAndPoi(cityName, poiName);
        Request request = new Request(MessageType.SEARCH_BY_CITY_AND_POI, searchReq);
        sendRequest(request);
    }

    /**
     * Send a request to the server.
     */
    private void sendRequest(Request request) {
        try {
            System.out.println("SearchControl: Sending request - " + request.getType());
            client.sendToServer(request);
        } catch (IOException e) {
            System.out.println("SearchControl: Error sending request");
            e.printStackTrace();
            if (resultCallback != null) {
                resultCallback.onError("CONNECTION_ERROR", "Failed to send request to server");
            }
        }
    }

    public void sendPurchaseRequest(Request request) {
        try {
            client.sendToServer(request);
        } catch (IOException e) {
            if (resultCallback != null)
                resultCallback.onError("NETWORK", "Failed to send purchase request");
        }
    }

    @Override
    public void displayMessage(Object msg) {
        System.out.println("SearchControl: Received message from server");

        if (msg instanceof Response) {
            Response response = (Response) msg;
            handleResponse(response);
        } else {
            System.out.println("SearchControl: Unknown message type: " + msg.getClass().getName());
        }
    }

    /**
     * Handle a response from the server.
     */
    @SuppressWarnings("unchecked")
    private void handleResponse(Response response) {
        if (resultCallback == null) {
            System.out.println("SearchControl: No callback registered for results");
            return;
        }

        // Pass through purchase/other errors if needed, but primarily handle search
        if (response.isOk()) {
            Object payload = response.getPayload();
            if (payload instanceof List) {
                // Check if it's a list of search results
                List<?> list = (List<?>) payload;
                if (list.isEmpty() || list.get(0) instanceof CitySearchResult) {
                    resultCallback.onSearchResults((List<CitySearchResult>) payload);
                }
            } else {
                // Could be other success messages, ignore for search
            }
        } else {
            System.out.println("SearchControl: Error response - " + response.getErrorCode());
            resultCallback.onError(response.getErrorCode(), response.getErrorMessage());
        }
    }

    /**
     * Disconnect from server.
     */
    public void disconnect() {
        // Do NOT close connection as it is shared singleton. just remove handler
        if (client != null)
            client.setMessageHandler(null);
    }
}
