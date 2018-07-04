package uk.callumr.circledockerbridge.docker;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerShould {
    private static final Logger log = LoggerFactory.getLogger(DockerShould.class);

    private final Docker docker = new Docker();

    @Test
    public void produce_events_when_a_container_is_created_and_destroyed() throws InterruptedException, IOException, TimeoutException {
        Stream<ContainerEvent> events = docker.createdAndDestroyedContainers();

        ContainerId containerId = dockerRun("busybox", "true");

        assertThat(events.limit(2)).containsExactly(
                ContainerEvents.created(containerId),
                ContainerEvents.destroyed(containerId)
        );
    }

    @Test
    public void get_the_exposed_ports_for_a_container() throws InterruptedException, TimeoutException, IOException {
        ContainerId containerId = dockerRun(
                "-p", "23499:4777",
                "-p", "41119:2000",
                "-p", "31313:2000",
                "crogers/exposed-port-not-opened-behind-it",
                "sleep", "999999999");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> killContainer(containerId)));

        try {
            PortMapping portMapping = docker.exposedPortsForContainer(containerId);

            assertThat(portMapping).isEqualTo(PortMapping.builder()
                    .putPorts(HostPort.of(23499), ContainerPort.of(4777))
                    .putPorts(HostPort.of(41119), ContainerPort.of(2000))
                    .putPorts(HostPort.of(31313), ContainerPort.of(2000))
                    .build());
        } finally {
            killContainer(containerId);
        }
    }

    @Test
    public void get_the_network_used_by_a_docker_container() {
        NetworkAlias networkAlias = NetworkAlias.of(randomString());

        withContainerConnectedToNetwork(networkAlias, containerId ->
                assertThat(docker.networkForContainer(containerId)).isEqualTo(networkAlias));
    }

    @Test
    public void create_a_network() {
        NetworkAlias networkAlias = NetworkAlias.of(randomString());
        docker.createNetwork(networkAlias);

        try {
            assertThat(docker(true, "network", "ls")).contains(networkAlias.alias());
        } finally {
            removeNetwork(networkAlias);
        }
    }

    @Test
    public void connect_a_container_to_the_network() {
        NetworkAlias networkAlias = NetworkAlias.of(randomString());
        docker.createNetwork(networkAlias);

        ContainerId containerId = null;
        try {
            containerId = dockerRun("busybox", "sleep", "99999999");
            docker.connectContainerToNetwork(containerId, networkAlias);
        } finally {
            if (containerId != null) {
                killContainer(containerId);
            }
            removeNetwork(networkAlias);
        }
    }

    @Test
    public void see_what_ip_address_a_container_has_for_a_network() {
        NetworkAlias networkAlias = NetworkAlias.of(randomString());

        withContainerConnectedToNetwork(networkAlias, containerId -> {
            NetworkScopedIpAddress networkScopedIpAddress = docker.ipAddressForContainerInNetwork(containerId, networkAlias);
            System.out.println("networkScopedIpAddress = " + networkScopedIpAddress);
        });
    }

    @Test
    public void remove_a_network() {
        NetworkAlias networkAlias = NetworkAlias.of(randomString());
        docker.createNetwork(networkAlias);
        docker.removeNetwork(networkAlias);

        assertThat(docker(true, "network", "ls")).doesNotContain(networkAlias.alias());
    }

    private void withContainerConnectedToNetwork(NetworkAlias networkAlias, Consumer<ContainerId> containerConsumer) {
        docker(true, "network", "create", networkAlias.alias());
        ContainerId containerId = null;
        try {
            containerId = dockerRun("--network", networkAlias.alias(), "busybox", "sleep", "999999");
            containerConsumer.accept(containerId);
        } finally {
            if (containerId != null) {
                killContainer(containerId);
            }
            removeNetwork(networkAlias);
        }
    }

    private String removeNetwork(NetworkAlias networkAlias) {
        return docker(true, "network", "rm", networkAlias.alias());
    }

    private String randomString() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    private ContainerId dockerRun(String... args) {
        List<String> command = Stream.concat(
                Stream.of("run", "-d", "--rm"),
                Arrays.stream(args)
        ).collect(Collectors.toList());

        ContainerId containerId = ContainerId.of(docker(false, command));

        log.info("Created container with id '{}'", containerId.id());

        return containerId;
    }

    private String docker(boolean includeError, List<String> args) {
        List<String> command = Stream.concat(
                Stream.of("docker"),
                args.stream()
        ).collect(Collectors.toList());

        ProcessResult processResult;

        try {
            processResult = new ProcessExecutor()
                    .command(command)
                    .readOutput(true)
                    .redirectErrorStream(includeError)
                    .exitValue(0)
                    .execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        return processResult.outputUTF8().trim();
    }

    private String docker(boolean includeError, String... args) {
        return docker(includeError, Arrays.asList(args));
    }

    private void killContainer(ContainerId containerId) {
        docker(true, "kill", containerId.id());

        log.info("Kill container with id '{}'", containerId.id());
    }
}