package server;

import common.City;
import common.MessageType;
import common.Request;
import common.Response;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import server.handler.MapEditHandler;
import server.handler.SearchHandler;
import server.handler.ApprovalHandler;
import server.handler.AuthHandler;
import server.handler.PurchaseHandler;
import server.handler.CustomerHandler;
import server.handler.NotificationHandler;
import server.handler.PricingHandler;
import server.handler.SupportHandler;
import server.scheduler.SubscriptionScheduler;

import java.io.IOException;
import java.util.ArrayList;

/**
 * GCM Server - Main server class handling client connections.
 * Supports both new Request/Response protocol and legacy string commands.
 */
public class GCMServer extends AbstractServer {

    public GCMServer(int port) {
        super(port);
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Message received from client: " + msg.getClass().getSimpleName());

        try {
            // ==================== NEW PROTOCOL (Request/Response) ====================
            if (msg instanceof Request) {
                Request request = (Request) msg;
                System.out.println("Processing Request: " + request);

                Response response = dispatchRequest(request);
                System.out.println("Sending Response: " + response);

                client.sendToClient(response);
                return;
            }

            // ==================== LEGACY PROTOCOL (String commands) ====================
            if (msg instanceof String) {
                handleLegacyMessage((String) msg, client);
                return;
            }

            System.out.println("Unknown message type: " + msg.getClass().getName());

        } catch (IOException e) {
            System.out.println("Error sending response to client");
            e.printStackTrace();
        }
    }

    /**
     * Dispatch a Request to the appropriate handler.
     */
    private Response dispatchRequest(Request request) {
        MessageType type = request.getType();

        // Search handlers (no authentication required)
        if (SearchHandler.canHandle(type)) {
            return SearchHandler.handle(request);
        }

        // Map editing handlers
        if (MapEditHandler.canHandle(type)) {
            return MapEditHandler.handle(request);
        }

        // Version approval handlers (Phase 3)
        if (ApprovalHandler.canHandle(type)) {
            return ApprovalHandler.handle(request);
        }

        // Authentication handlers (Phase 4)
        if (AuthHandler.canHandle(type)) {
            return AuthHandler.handle(request);
        }

        // Purchase handlers (Phase 5)
        if (PurchaseHandler.canHandle(type)) {
            return PurchaseHandler.handle(request);
        }

        // Customer handlers (Phase 6)
        if (CustomerHandler.canHandle(type)) {
            return CustomerHandler.handle(request);
        }

        // Notification handlers (Phase 7)
        if (NotificationHandler.canHandle(type)) {
            return NotificationHandler.handle(request);
        }

        // Pricing handlers (Phase 8)
        if (PricingHandler.canHandle(type)) {
            return PricingHandler.handle(request);
        }

        // Support handlers (Phase 9)
        if (SupportHandler.canHandle(type)) {
            return SupportHandler.handle(request);
        }

        // Report handlers (Phase 10)
        if (server.handler.ReportHandler.canHandle(type)) {
            return server.handler.ReportHandler.handle(request);
        }

        // Legacy handlers (for backward compatibility)
        if (type == MessageType.LEGACY_GET_CITIES) {
            ArrayList<City> cities = MySQLController.getAllCities();
            return Response.success(request, cities);
        }

        if (type == MessageType.LEGACY_GET_MAPS) {
            // Expects cityId in payload
            if (request.getPayload() instanceof Integer) {
                int cityId = (Integer) request.getPayload();
                ArrayList<common.Map> maps = MySQLController.getMapsForCity(cityId);
                return Response.success(request, maps);
            }
            return Response.error(request, Response.ERR_VALIDATION, "City ID required");
        }

        return Response.error(request, Response.ERR_INTERNAL,
                "No handler for message type: " + type);
    }

    /**
     * Handle legacy string-based protocol (backward compatibility).
     */
    private void handleLegacyMessage(String request, ConnectionToClient client) throws IOException {
        System.out.println("Legacy message: " + request);

        // CASE 0: Login authentication (Format: "login [username] [password]")
        if (request.startsWith("login ")) {
            try {
                String[] parts = request.split(" ");
                if (parts.length >= 3) {
                    String username = parts[1];
                    String password = parts[2];

                    String[] authResult = MySQLController.authenticateUser(username, password);

                    if (authResult != null) {
                        client.sendToClient("login_success " + authResult[0] + " " + authResult[1]);
                    } else {
                        client.sendToClient("login_failed");
                    }
                } else {
                    client.sendToClient("login_failed");
                }
            } catch (Exception e) {
                client.sendToClient("login_failed");
                e.printStackTrace();
            }
        }

        // CASE 1: Get all cities
        else if (request.equals("get_cities")) {
            ArrayList<City> cities = MySQLController.getAllCities();
            client.sendToClient(cities);
        }

        // CASE 2: Get maps for a specific city (Format: "get_maps [id]")
        else if (request.startsWith("get_maps ")) {
            try {
                int cityId = Integer.parseInt(request.split(" ")[1]);
                ArrayList<common.Map> maps = MySQLController.getMapsForCity(cityId);
                client.sendToClient(maps);
            } catch (Exception e) {
                System.out.println("Error parsing ID for get_maps");
            }
        }

        // CASE 3: Update Price (Format: "update_price [id] [new_price]")
        else if (request.startsWith("update_price ")) {
            try {
                String[] parts = request.split(" ");
                int cityId = Integer.parseInt(parts[1]);
                double newPrice = Double.parseDouble(parts[2]);

                boolean success = MySQLController.updateCityPrice(cityId, newPrice);

                if (success) {
                    client.sendToClient("Success: Price updated!");
                    client.sendToClient(MySQLController.getAllCities());
                } else {
                    client.sendToClient("Error: Could not update price.");
                }
            } catch (Exception e) {
                client.sendToClient("Error: Invalid update format.");
            }
        }
    }

    @Override
    protected void serverStarted() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          GCM SERVER STARTED SUCCESSFULLY                 ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Port: " + getPort() + "                                             ║");
        System.out.println("║  Protocol: Request/Response + Legacy String              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        // Start subscription expiry scheduler (Phase 7)
        SubscriptionScheduler.getInstance().start();
    }

    @Override
    protected void serverStopped() {
        System.out.println("Server has stopped listening for connections.");
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        System.out.println("→ Client connected: " + client.getInetAddress());
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        System.out.println("← Client disconnected: " + client.getInetAddress());
    }

    // MAIN METHOD TO START THE SERVER
    public static void main(String[] args) {
        int port = 5555;
        GCMServer server = new GCMServer(port);

        try {
            server.listen();
        } catch (IOException e) {
            System.out.println("Error starting server");
            e.printStackTrace();
        }
    }
}