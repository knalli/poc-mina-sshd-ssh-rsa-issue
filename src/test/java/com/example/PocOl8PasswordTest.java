package com.example;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;

import static org.testcontainers.containers.wait.strategy.Wait.forListeningPort;

@Testcontainers
class PocOl8PasswordTest extends BaseTest {

    static final Path BASE = Path.of("src/test/resources/ol8pw");

    @Container
    @SuppressWarnings("resource")
    GenericContainer<?> container = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withDockerfile(BASE.resolve("Dockerfile")))
            .waitingFor(forListeningPort())
            .withExposedPorts(22);

    @Test
    void run() throws Exception {
        try (final var client = buildClient()) {
            var connectFuture = awaitConnect(client, container);

            var session = connectFuture.getSession();
            try {
                session.addPasswordIdentity("root");
                session.auth().verify();
                executeStuff(session);
            } finally {
                session.close(true);
            }
        }
    }

}