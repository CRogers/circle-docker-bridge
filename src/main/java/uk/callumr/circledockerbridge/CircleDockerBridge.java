package uk.callumr.circledockerbridge;

import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.callumr.circledockerbridge.docker.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class CircleDockerBridge {
    private static Logger log = LoggerFactory.getLogger(CircleDockerBridge.class);

    private static final ContainerName BUSYBOX = ContainerName.of("busybox");

    private final Function<HostPort, HostPort> portMappingFunction;
    private final Docker docker = new Docker();
    private final ExecutorService tcpServers = Executors.newCachedThreadPool();

    private ContainerId execContainer;

    public CircleDockerBridge(Function<HostPort, HostPort> portMappingFunction) {
        this.portMappingFunction = portMappingFunction;
    }

    public void start() {
        if (execContainer != null) {
            throw new IllegalStateException("Already started!");
        }

        log.info("Starting " + CircleDockerBridge.class.getSimpleName());

        execContainer = docker.run(BUSYBOX, "sleep", "999999999");
        log.info("Created exec container with id {}", execContainer.shortId());

        docker.createdAndDestroyedContainers().forEach(this::onContainerEvent);

        log.info("Finished");
    }

    private void onContainerEvent(ContainerEvent containerEvent) {
        ContainerEvents.caseOf(containerEvent)
                .created(containerId -> {
                    log.info("Container {} created", containerId);

                    PortMapping portMapping = docker.exposedPortsForContainer(containerId);
                    log.info("Container {} created - it's ports are: {}", containerId.shortId(), portMapping);

                    NetworkAlias originalNetwork = docker.networkForContainer(containerId);
                    NetworkAlias ourNetwork = originalNetwork.append("-circle-bridge" + UUID.randomUUID().toString().substring(0, 6));
                    docker.createNetwork(ourNetwork);

                    docker.connectContainerToNetwork(containerId, ourNetwork);
                    docker.connectContainerToNetwork(execContainer, ourNetwork);

                    NetworkScopedIpAddress ipAddress = docker.ipAddressForContainerInNetwork(containerId, ourNetwork);

                    portMapping.ports().forEach((hostPort, containerPort) ->
                            tcpServers.submit(() -> tcpServerForPort(hostPort, ipAddress, containerPort)));
                    return null;
                })
                .destroyed(containerId -> {
                    log.info("Container {} destroyed", containerId.shortId());
                    return null;
                });
    }

    private void tcpServerForPort(HostPort hostPort, NetworkScopedIpAddress ipAddress, ContainerPort containerPort) {
        HostPort mappedPort = portMappingFunction.apply(hostPort);
        System.out.println("mappedPort = " + mappedPort);
        try (ServerSocket waitSocket = new ServerSocket(mappedPort.portNumber())) {
            while (true) {
                Socket socket = waitSocket.accept();
                tcpServers.submit(() -> {
                    try {
                        log.info("Accepted connection");

                        docker.exec(execContainer,
                                    new TeeOutputStream(socket.getOutputStream(), System.out),
                                    new TeeInputStream(socket.getInputStream(), System.out),
                                    "nc", ipAddress.ipAddress(), containerPort.asString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
