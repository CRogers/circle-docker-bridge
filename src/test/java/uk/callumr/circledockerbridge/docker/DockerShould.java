package uk.callumr.circledockerbridge.docker;

import org.junit.Test;
import uk.callumr.circledockerbridge.DockerTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerShould {

    private final Docker docker = new Docker();

    @Test
    public void produce_events_when_a_container_is_created_and_destroyed() throws InterruptedException, IOException, TimeoutException {
        Stream<ContainerEvent> events = docker.createdAndDestroyedContainers();

        ContainerId containerId = DockerTestUtils.dockerRun("busybox", "true");

        assertThat(events.limit(2)).containsExactly(
                ContainerEvents.created(containerId),
                ContainerEvents.destroyed(containerId)
        );
    }

    @Test
    public void get_the_exposed_ports_for_a_container() throws InterruptedException, TimeoutException, IOException {
        ContainerId containerId = DockerTestUtils.dockerRun(
                "-p", "23499:4777",
                "-p", "41119:2000",
                "-p", "31313:2000",
                "crogers/exposed-port-not-opened-behind-it",
                "sleep", "999999999");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> DockerTestUtils.killContainer(containerId)));

        try {
            PortMapping portMapping = docker.exposedPortsForContainer(containerId);

            assertThat(portMapping).isEqualTo(PortMapping.builder()
                    .putPorts(HostPort.of(23499), ContainerPort.of(4777))
                    .putPorts(HostPort.of(41119), ContainerPort.of(2000))
                    .putPorts(HostPort.of(31313), ContainerPort.of(2000))
                    .build());
        } finally {
            DockerTestUtils.killContainer(containerId);
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
            assertThat(DockerTestUtils.docker(true, "network", "ls")).contains(networkAlias.alias());
        } finally {
            DockerTestUtils.removeNetwork(networkAlias);
        }
    }

    @Test
    public void connect_a_container_to_the_network() {
        NetworkAlias networkAlias = NetworkAlias.of(randomString());
        docker.createNetwork(networkAlias);

        ContainerId containerId = null;
        try {
            containerId = DockerTestUtils.dockerRun("busybox", "sleep", "99999999");
            docker.connectContainerToNetwork(containerId, networkAlias);
        } finally {
            if (containerId != null) {
                DockerTestUtils.killContainer(containerId);
            }
            DockerTestUtils.removeNetwork(networkAlias);
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

        assertThat(DockerTestUtils.docker(true, "network", "ls")).doesNotContain(networkAlias.alias());
    }

    @Test
    public void exec_a_command_in_a_container_and_get_back_stdout_stdin() {
        ContainerId containerId = DockerTestUtils.dockerRun("busybox", "sleep", "999999");

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayInputStream input = new ByteArrayInputStream("yolo".getBytes(StandardCharsets.UTF_8));
            docker.exec(containerId, output, input, "cat");
            assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("yolo");
        } finally {
            DockerTestUtils.killContainer(containerId);
        }
    }

    private void withContainerConnectedToNetwork(NetworkAlias networkAlias, Consumer<ContainerId> containerConsumer) {
        DockerTestUtils.docker(true, "network", "create", networkAlias.alias());
        ContainerId containerId = null;
        try {
            containerId = DockerTestUtils.dockerRun("--network", networkAlias.alias(), "busybox", "sleep", "999999");
            containerConsumer.accept(containerId);
        } finally {
            if (containerId != null) {
                DockerTestUtils.killContainer(containerId);
            }
            DockerTestUtils.removeNetwork(networkAlias);
        }
    }

    private String randomString() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

}