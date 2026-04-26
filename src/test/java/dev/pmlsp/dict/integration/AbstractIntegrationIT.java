package dev.pmlsp.dict.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Boots the application with the {@code simulator} profile so the {@link
 * dev.pmlsp.dict.infrastructure.simulator.DictSimulatorController} runs in the same context.
 *
 * <p>The HTTP gateway is pointed at the same random server port that Spring Boot binds to —
 * this is what makes "client → real HTTP → simulator" work without two JVMs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "simulator"})
public abstract class AbstractIntegrationIT {

    private static final int PORT;

    static {
        try (ServerSocket socket = new ServerSocket(0)) {
            PORT = socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("could not allocate ephemeral port", e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
        registry.add("dict.endpoint.base-url", () -> "http://localhost:" + PORT + "/dict/v1");
    }

    protected int port() {
        return PORT;
    }
}
