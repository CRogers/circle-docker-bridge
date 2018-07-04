package uk.callumr.circledockerbridge;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.callumr.circledockerbridge.docker.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class CircleDockerBridge {
    private static Logger log = LoggerFactory.getLogger(CircleDockerBridge.class);

    private final Function<HostPort, HostPort> portMappingFunction;
    private final Docker docker = new Docker();
    private final ExecutorService tcpServers = Executors.newCachedThreadPool();

    public CircleDockerBridge(Function<HostPort, HostPort> portMappingFunction) {
        this.portMappingFunction = portMappingFunction;
    }

    public void start() {
        log.info("Starting " + CircleDockerBridge.class.getSimpleName());
        docker.createdAndDestroyedContainers().forEach(this::onContainerEvent);
    }

    private void onContainerEvent(ContainerEvent containerEvent) {
        ContainerEvents.caseOf(containerEvent)
                .created(containerId -> {
                    PortMapping portMapping = docker.exposedPortsForContainer(containerId);
                    log.info("Container {} created - it's ports are: {}", containerId.shortId(), portMapping);

                    portMapping.ports().forEach((hostPort, containerPort) ->
                            tcpServers.submit(() -> tcpServerForPort(hostPort, containerPort)));
                    return null;
                })
                .destroyed(containerId -> {
                    log.info("Container {} destroyed", containerId.shortId());
                    return null;
                });
    }

    private void tcpServerForPort(HostPort hostPort, ContainerPort containerPort) {
        HostPort mappedPort = portMappingFunction.apply(hostPort);
        System.out.println("mappedPort = " + mappedPort);
        try (ServerSocket waitSocket = new ServerSocket(mappedPort.portNumber())) {
            while (true) {
                try (Socket socket = waitSocket.accept()) {
                    IOUtils.copy(socket.getInputStream(), System.out);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
