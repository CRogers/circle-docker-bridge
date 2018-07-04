package uk.callumr.circledockerbridge.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public class Docker {
    private static Logger log = LoggerFactory.getLogger(Docker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Stream<ContainerEvent> createdAndDestroyedContainers() {
        Process process;

        try {
            process = new ProcessBuilder()
                    .command(
                            "docker",
                            "events",
                            "--format", "{{.Status}},{{.ID}}",
                            "-f", "event=create",
                            "-f", "event=destroy",
                            "-f", "type=container")
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

    public PortMapping exposedPortsForContainer(ContainerId containerId) {
        return PortMapping.fromDockerJson(readTree(inspectContainer(containerId, "{{json .NetworkSettings.Ports}}")));
    }

    public NetworkAlias networkForContainer(ContainerId containerId) {
        String output = inspectContainer(containerId, "{{json .NetworkSettings.Networks}}");

        String networkAlias = readTree(output)
                .fields().next()
                .getKey();

        return NetworkAlias.of(networkAlias);

    }

    public void createNetwork(NetworkAlias networkAlias) {
        try {
            new ProcessExecutor()
                    .command("docker", "network", "create", networkAlias.alias())
                    .exitValue(0)
                    .readOutput(true)
                    .execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private String inspectContainer(ContainerId containerId, String format) {
        try {
            ProcessResult processResult = new ProcessExecutor()
                    .command(
                            "docker",
                            "inspect",
                            "--format", format,
                            containerId.id())
                    .readOutput(true)
                    .exitValue(0)
                    .execute();

            return processResult.outputUTF8();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode readTree(String output) {
        try {
            return OBJECT_MAPPER.readTree(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
