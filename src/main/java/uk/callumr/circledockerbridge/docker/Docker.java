package uk.callumr.circledockerbridge.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import uk.callumr.circledockerbridge.docker.json.PortMappingJson;

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
                    .execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        return PortMappingJson.fromDockerJson(processResult.outputUTF8());
    }
}
