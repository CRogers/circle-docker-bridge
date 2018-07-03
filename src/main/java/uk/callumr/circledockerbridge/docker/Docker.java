package uk.callumr.circledockerbridge.docker;

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
        ProcessResult processResult;

        try {
            processResult = new ProcessExecutor()
                    .command(
                            "docker",
                            "inspect",
                            "--format", "{{json .NetworkSettings.Ports}}",
                            containerId.id())
                    .readOutput(true)
                    .exitValue(0)
                    .execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        return PortMapping.fromDockerJson(processResult.outputUTF8());
    }

    public NetworkAlias networkForContainer(ContainerId containerId) {
        try {
            ProcessResult processResult = new ProcessExecutor()
                    .command(
                            "docker",
                            "inspect",
                            "--format", "{{json .NetworkSettings.Networks}}",
                            containerId.id())
                    .readOutput(true)
                    .exitValue(0)
                    .execute();

            String networkAlias = new ObjectMapper().readTree(processResult.outputUTF8())
                    .fields().next()
                    .getKey();

            return NetworkAlias.of(networkAlias);
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }

    }
}
