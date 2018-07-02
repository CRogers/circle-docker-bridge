package uk.callumr.circledockerbridge.docker;

import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerShould {
    private final Docker docker = new Docker();

    @Test
    public void produce_events_when_a_container_is_created_and_destroyed() throws InterruptedException, IOException, TimeoutException {
        Stream<ContainerEvent> events = docker.createdAndDestroyedContainers();

        ProcessResult processResult = new ProcessExecutor()
                .command("docker", "run", "-d", "--rm", "busybox", "true")
                .readOutput(true)
                .execute();

        ContainerId containerId = ContainerId.of(processResult.outputUTF8().trim());

        assertThat(events.limit(2)).containsExactly(
                ContainerEvents.created(containerId),
                ContainerEvents.destroyed(containerId)
        );
    }
}