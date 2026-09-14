package com.rockandhardplaces.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Separate loopback listener: never registered on the application's public MVC server. */
@Component
@Profile("local-gemini-diagnostics")
@ConditionalOnProperty(name = "rhp.planning.diagnostics.enabled", havingValue = "true")
class LocalGeminiSchemaDiagnostics {
    private final GeminiProjectPlanningAiClient client;
    private final ObjectMapper mapper;
    private final int port;
    private final com.rockandhardplaces.catalog.TradeRepository trades;
    private HttpServer server;

    LocalGeminiSchemaDiagnostics(GeminiProjectPlanningAiClient client, ObjectMapper mapper,
            @Value("${rhp.planning.diagnostics.port:8091}") int port,
            com.rockandhardplaces.catalog.TradeRepository trades) {
        this.client = client; this.mapper = mapper; this.port = port; this.trades = trades;
    }

    @PostConstruct void start() throws IOException {
        // Deliberately not configurable: this listener must never bind a public interface.
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/diagnostics/gemini/schema/", this::handle);
        server.start();
    }

    @PreDestroy void close() { if (server != null) server.stop(0); }

    int port() { return server.getAddress().getPort(); }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            // Reject browser-origin requests and DNS rebinding; no forwarded-header trust or CORS.
            String host = exchange.getRequestHeaders().getFirst("Host");
            if (!exchange.getRemoteAddress().getAddress().isLoopbackAddress()
                    || !("127.0.0.1:" + port()).equals(host)
                    || exchange.getRequestHeaders().containsKey("Origin")) {
                reply(exchange, 403, new SchemaDiagnosticResult("", "FORBIDDEN", "Local command-line access only"));
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                reply(exchange, 405, new SchemaDiagnosticResult("", "METHOD_NOT_ALLOWED", "Use POST"));
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String stageName = path.substring("/diagnostics/gemini/schema/".length());
            if (!stageName.matches("[A-L]") || exchange.getRequestURI().getRawQuery() != null
                    || exchange.getRequestBody().read() != -1) {
                reply(exchange, 400, new SchemaDiagnosticResult("", "BAD_REQUEST", "Select A through L without a body or query"));
                return;
            }
            if (stageName.charAt(0) <= 'G') {
                reply(exchange, 200, client.diagnose(SchemaDiagnosticStage.valueOf(stageName)));
            } else {
                // Same ordered catalog query as ProjectPlanningService; scalar read only.
                var catalogNames = trades.findAll().stream().map(com.rockandhardplaces.catalog.Trade::getName).toList();
                reply(exchange, 200, client.diagnose(RequestDiagnosticStage.valueOf(stageName), catalogNames));
            }
        }
    }

    private void reply(HttpExchange exchange, int status, SchemaDiagnosticResult result) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(result);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
