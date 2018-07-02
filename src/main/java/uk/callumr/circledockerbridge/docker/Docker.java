package uk.callumr.circledockerbridge.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Stream;

public class Docker {
    private static Logger log = LoggerFactory.getLogger(Docker.class);

    public Stream<ContainerEvent> createdAndDestroyedContainers() throws IOException {
        Process process = new ProcessBuilder()
                .command(
                        "docker",
                        "events",
                        "--format", "{{.Status}},{{.ID}}",
                        "-f", "event=create",
                        "-f", "event=destroy",
                        "-f", "type=container")
                .start();

        return new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines()
                .flatMap(line -> {
                    String[] splitEvent = line.split(",");
                    String type = splitEvent[0];
                    ContainerId containerId = ContainerId.of(splitEvent[1]);
                    switch (type) {
                        case "create":
                            return Stream.of(ContainerEvents.created(containerId));
                        case "destroy":
                            return Stream.of(ContainerEvents.destroyed(containerId));
                        default:
                            log.warn("Ignoring event of type {}", type);
                            return Stream.empty();
                    }
                });
    }
}
