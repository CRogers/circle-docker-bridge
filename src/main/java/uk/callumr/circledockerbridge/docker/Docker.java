package uk.callumr.circledockerbridge.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
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
        docker("network", "create", networkAlias.alias());
    }

    public void removeNetwork(NetworkAlias networkAlias) {
        docker("network", "remove", networkAlias.alias());
    }

    public void connectContainerToNetwork(ContainerId containerId, NetworkAlias networkAlias) {
        docker("network", "connect", networkAlias.alias(), containerId.id());
    }

    public NetworkScopedIpAddress ipAddressForContainerInNetwork(ContainerId containerId, NetworkAlias networkAlias) {
        return NetworkScopedIpAddress.of(inspectContainer(containerId,
                String.format("{{ (index .NetworkSettings.Networks \"%s\").IPAddress }}", networkAlias.alias())));
    }

    public void exec(ContainerId containerId, OutputStream stdout, InputStream stdin, String... args) {
        try {
            List<String> command = Stream.concat(
                    Stream.of("docker", "exec", "-i", containerId.id()),
                    Arrays.stream(args)
            ).collect(Collectors.toList());

            new ProcessExecutor()
                    .command(command)
                    .exitValue(0)
                    .redirectOutput(stdout)
                    .redirectInput(stdin)
                    .readOutput(true)
                    .executeNoTimeout();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String inspectContainer(ContainerId containerId, String format) {
        return docker(
                "inspect",
                "--format", format,
                containerId.id()).outputUTF8().trim();
    }

    private ProcessResult docker(String... args) {
        return docker(Arrays.stream(args));
    }

    private ProcessResult docker(Stream<String> args) {
        try {
            List<String> command = Stream.concat(
                    Stream.of("docker"),
                    args
            ).collect(Collectors.toList());

            return new ProcessExecutor()
                    .command(command)
                    .exitValue(0)
                    .readOutput(true)
                    .execute();
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
