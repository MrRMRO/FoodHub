package com.foodhub.api;

import com.foodhub.dao.CustomerDAO;
import com.foodhub.dao.MenuItemDAO;
import com.foodhub.dao.OrderDAO;
import com.foodhub.model.Customer;
import com.foodhub.model.MenuItem;
import com.foodhub.model.Order;
import com.foodhub.model.OrderItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestServer {

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd")
            .create();

    private static final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final OrderDAO orderDAO = new OrderDAO();

    public static void main(String[] args) throws IOException {
        // Create server on all network interfaces (0.0.0.0) instead of localhost
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);

        System.out.println("===========================================");
        System.out.println("  FoodHub Backend Server Starting...");
        System.out.println("===========================================");

        // Menu endpoints
        server.createContext("/api/menu", new MenuHandler());
        server.createContext("/api/menu/category", new MenuByCategoryHandler());

        // Customer endpoints
        server.createContext("/api/customers", new CustomerHandler());
        server.createContext("/api/customer", new CustomerByIdHandler());

        // Order endpoints
        server.createContext("/api/orders", new OrderHandler());
        server.createContext("/api/order", new OrderByIdHandler());
        server.createContext("/api/order/items", new OrderItemsHandler());
        server.createContext("/api/order/status", new OrderStatusHandler());

        // Test endpoint
        server.createContext("/api/test", new TestHandler());

        server.setExecutor(null); // Default executor
        server.start();

        System.out.println("✓ Server started successfully!");
        System.out.println("✓ Listening on port: 8080");
        System.out.println("✓ Access from browser: http://localhost:8080/api/test");
        System.out.println("✓ Available endpoints:");
        System.out.println("  - GET  /api/menu");
        System.out.println("  - GET  /api/menu/category?category=Burgers");
        System.out.println("  - GET  /api/customers");
        System.out.println("  - POST /api/customers");
        System.out.println("  - GET  /api/orders");
        System.out.println("  - POST /api/orders");
        System.out.println("  - GET  /api/order/items?orderId=1");
        System.out.println("===========================================");
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Add CORS headers to response
     * This is CRITICAL for React Native to communicate with backend
     */
    private static void addCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
    }

    /**
     * Send JSON response to client
     */
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        // Add CORS headers
        addCORSHeaders(exchange);

        String jsonResponse = gson.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }

        // Log request for debugging
        System.out.println("[" + new Date() + "] " +
                exchange.getRequestMethod() + " " +
                exchange.getRequestURI() + " → " +
                statusCode);
    }

    /**
     * Read request body as String
     */
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Handle OPTIONS preflight requests (CORS)
     */
    private static boolean handleCORSPreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCORSHeaders(exchange);
            exchange.sendResponseHeaders(204, -1); // No content
            return true;
        }
        return false;
    }

    // ============================================
    // TEST HANDLER (for connection testing)
    // ============================================

    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "FoodHub Backend is running!");
            response.put("timestamp", new Date().toString());
            response.put("endpoints", new String[]{
                    "GET /api/menu",
                    "POST /api/customers",
                    "POST /api/orders"
            });

            sendJsonResponse(exchange, 200, response);
        }
    }

    // ============================================
    // MENU HANDLERS
    // ============================================

    static class MenuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    List<MenuItem> menuItems = menuItemDAO.getAvailableMenuItems();
                    sendJsonResponse(exchange, 200, menuItems);

                } else if ("POST".equals(exchange.getRequestMethod())) {
                    String body = readRequestBody(exchange);
                    MenuItem menuItem = gson.fromJson(body, MenuItem.class);
                    boolean success = menuItemDAO.addMenuItem(menuItem);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", success ? "Menu item added successfully" : "Failed to add menu item");
                    sendJsonResponse(exchange, success ? 201 : 400, response);

                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Method not allowed");
                    sendJsonResponse(exchange, 405, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }

    static class MenuByCategoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("category=")) {
                        String category = query.split("=")[1];
                        List<MenuItem> menuItems = menuItemDAO.getMenuItemsByCategory(category);
                        sendJsonResponse(exchange, 200, menuItems);
                    } else {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Category parameter required");
                        sendJsonResponse(exchange, 400, error);
                    }
                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Method not allowed");
                    sendJsonResponse(exchange, 405, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }

    // ============================================
    // CUSTOMER HANDLERS
    // ============================================

    static class CustomerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    List<Customer> customers = customerDAO.getAllCustomers();
                    sendJsonResponse(exchange, 200, customers);

                } else if ("POST".equals(exchange.getRequestMethod())) {
                    String body = readRequestBody(exchange);
                    System.out.println("Received customer data: " + body);

                    Customer customer = gson.fromJson(body, Customer.class);
                    customer.setCreatedDate(new Date());

                    int id = customerDAO.addCustomer(customer);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", id > 0);
                    response.put("customerId", id);
                    response.put("message", id > 0 ? "Customer added successfully" : "Failed to add customer");
                    sendJsonResponse(exchange, id > 0 ? 201 : 400, response);

                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Method not allowed");
                    sendJsonResponse(exchange, 405, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }

    static class CustomerByIdHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("id=")) {
                        int id = Integer.parseInt(query.split("=")[1]);
                        Customer customer = customerDAO.getCustomerById(id);

                        if (customer != null) {
                            sendJsonResponse(exchange, 200, customer);
                        } else {
                            Map<String, String> error = new HashMap<>();
                            error.put("error", "Customer not found");
                            sendJsonResponse(exchange, 404, error);
                        }
                    } else {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "ID parameter required");
                        sendJsonResponse(exchange, 400, error);
                    }
                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Method not allowed");
                    sendJsonResponse(exchange, 405, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }

    // ============================================
    // ORDER HANDLERS
    // ============================================

    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();

                    if (query != null && query.startsWith("customerId=")) {
                        int customerId = Integer.parseInt(query.split("=")[1]);
                        List<Order> orders = orderDAO.getOrdersByCustomerId(customerId);
                        sendJsonResponse(exchange, 200, orders);
                    } else {
                        List<Order> orders = orderDAO.getAllOrders();
                        sendJsonResponse(exchange, 200, orders);
                    }

                } else if ("POST".equals(exchange.getRequestMethod())) {
                    String body = readRequestBody(exchange);
                    System.out.println("Received order data: " + body);

                    Map<String, Object> orderData = gson.fromJson(body, Map.class);

                    // Create order
                    Order order = new Order();
                    order.setCustomerId(((Double) orderData.get("customerId")).intValue());
                    order.setOrderDate(new Date());
                    order.setTotalAmount((Double) orderData.get("totalAmount"));
                    order.setStatus("PENDING");
                    order.setDeliveryAddress((String) orderData.get("deliveryAddress"));

                    int orderId = orderDAO.addOrder(order);

                    if (orderId > 0) {
                        // Add order items
                        List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
                        for (Map<String, Object> item : items) {
                            OrderItem orderItem = new OrderItem();
                            orderItem.setOrderId(orderId);
                            orderItem.setMenuItemId(((Double) item.get("menuItemId")).intValue());
                            orderItem.setQuantity(((Double) item.get("quantity")).intValue());
                            orderItem.setUnitPrice((Double) item.get("unitPrice"));
                            orderItem.setSubtotal((Double) item.get("subtotal"));

                            orderDAO.addOrderItem(orderItem);
                        }
                    }

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", orderId > 0);
                    response.put("orderId", orderId);
                    response.put("message", orderId > 0 ? "Order placed successfully" : "Failed to place order");
                    sendJsonResponse(exchange, orderId > 0 ? 201 : 400, response);

                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Method not allowed");
                    sendJsonResponse(exchange, 405, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }

    static class OrderByIdHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("id=")) {
                        int id = Integer.parseInt(query.split("=")[1]);
                        Order order = orderDAO.getOrderById(id);

                        if (order != null) {
                            sendJsonResponse(exchange, 200, order);
                        } else {
                            Map<String, String> error = new HashMap<>();
                            error.put("error", "Order not found");
                            sendJsonResponse(exchange, 404, error);
                        }
                    } else {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "ID parameter required");
                        sendJsonResponse(exchange, 400, error);
                    }
                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Method not allowed");
                    sendJsonResponse(exchange, 405, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }

    static class OrderItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("orderId=")) {
                        int orderId = Integer.parseInt(query.split("=")[1]);
                        List<OrderItem> orderItems = orderDAO.getOrderItems(orderId);
                        sendJsonResponse(exchange, 200, orderItems);
                    } else {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "orderId parameter required");
                        sendJsonResponse(exchange, 400, error);
                    }
                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Method not allowed");
                    sendJsonResponse(exchange, 405, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }

    static class OrderStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCORSPreflight(exchange)) return;

            try {
                if ("PUT".equals(exchange.getRequestMethod())) {
                    String body = readRequestBody(exchange);
                    Map<String, Object> data = gson.fromJson(body, Map.class);

                    int orderId = ((Double) data.get("orderId")).intValue();
                    String status = (String) data.get("status");

                    boolean success = orderDAO.updateOrderStatus(orderId, status);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", success ? "Order status updated" : "Failed to update status");
                    sendJsonResponse(exchange, success ? 200 : 400, response);

                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Method not allowed");
                    sendJsonResponse(exchange, 405, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, error);
            }
        }
    }
}