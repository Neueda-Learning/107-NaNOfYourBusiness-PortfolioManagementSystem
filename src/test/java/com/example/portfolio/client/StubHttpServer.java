package com.example.portfolio.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Minimal JDK-only HTTP stub server used by client unit tests (MFAPIClient,
 * TwelveDataClient) to simulate external API responses without introducing a
 * WireMock/MockWebServer dependency.
 */
final class StubHttpServer implements AutoCloseable {

    private final HttpServer server;

    private StubHttpServer(HttpServer server) {
        this.server = server;
    }

    static StubHttpServer startReturning(int statusCode, String contentType, String body) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> respond(exchange, statusCode, contentType, body));
            server.start();
            return new StubHttpServer(server);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start stub HTTP server", e);
        }
    }

    private static void respond(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
        }
    }
}


