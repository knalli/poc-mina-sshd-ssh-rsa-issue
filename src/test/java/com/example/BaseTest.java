package com.example;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.common.signature.Signature;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.testcontainers.containers.GenericContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.function.Predicate.not;

class BaseTest {

    static final ServerKeyVerifier ACCEPT_ALL_SERVER_KEY_VERIFIER = new AcceptAllServerKeyVerifier();

    static ConnectFuture awaitConnect(final SshClient client,
            final GenericContainer<?> container) throws IOException {
        var connectFuture = client.connect("root", container.getHost(), container.getMappedPort(22));
        connectFuture.verify();
        return connectFuture;
    }

    static SshClient buildClient() {
        return buildClient(false);
    }

    static SshClient buildClient(final boolean useWorkaround) {
        final var client = ClientBuilder.builder()
                .serverKeyVerifier(ACCEPT_ALL_SERVER_KEY_VERIFIER)
                .build();

        // Disable auto-read user keys in $HOME/.ssh
        client.setKeyIdentityProvider(KeyIdentityProvider.EMPTY_KEYS_PROVIDER);

        // Allow all factories (default excludes deprecated ones)
        // client.setKeyExchangeFactories(NamedFactory.setUpTransformedFactories(false, BuiltinDHFactories.VALUES, ClientBuilder.DH2KEX));

        if (useWorkaround) {
            // push back these options to the end
            final var deprecated = List.of(
                    BuiltinSignatures.dsa,
                    BuiltinSignatures.dsa_cert,
                    BuiltinSignatures.rsa,
                    BuiltinSignatures.rsa_cert
            );
            client.setSignatureFactories(
                    Stream.concat(
                                    BuiltinSignatures.VALUES.stream()
                                            .filter(not(deprecated::contains)),
                                    deprecated.stream()
                            )
                            .map(s -> (NamedFactory<Signature>) s)
                            .toList()
            );
        } else {
            // regular one
            client.setSignatureFactories(List.copyOf(BuiltinSignatures.VALUES));
        }

        client.start();

        return client;
    }

    static Stream<KeyPair> readKeyPairs(final String name,
            final byte[] identity,
            final char[] passphrase) {
        try (final var is = new ByteArrayInputStream(identity)) {
            final var keyPairs = SecurityUtils.loadKeyPairIdentities(null,
                    NamedResource.ofName(name),
                    is,
                    (session, resourceKey, retryIndex) -> passphrase != null
                            ? String.valueOf(passphrase)
                            : null);
            return StreamSupport.stream(keyPairs.spliterator(), false);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to load private key", e);
        }
    }

    static void readAndApplyKey(Path file,
            ClientSession session) throws IOException {
        readKeyPairs(UUID.randomUUID().toString(), Files.readAllBytes(file), null)
                .forEach(session::addPublicKeyIdentity);
    }

    static void executeStuff(final ClientSession session) throws IOException {
        try (
                var out = new ByteArrayOutputStream();
                var channel = session.createExecChannel("sshd -h || true")
        ) {
            channel.setOut(out);
            channel.setErr(out);
            try {
                channel.open()
                        .verify();
                channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                        2000);
            } finally {
                System.out.println(out.toString(StandardCharsets.US_ASCII));
            }
        }
    }

    static class AcceptAllServerKeyVerifier implements ServerKeyVerifier {
        @Override
        public boolean verifyServerKey(final ClientSession clientSession, final SocketAddress remoteAddress, final PublicKey serverKey) {
            // ~ StrictHostKeyChecking = no
            return true;
        }
    }
}