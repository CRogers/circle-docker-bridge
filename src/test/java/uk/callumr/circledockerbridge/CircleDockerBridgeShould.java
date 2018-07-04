package uk.callumr.circledockerbridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import uk.callumr.circledockerbridge.docker.ContainerId;
import uk.callumr.circledockerbridge.docker.HostPort;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class CircleDockerBridgeShould {
    private static final int PORT_DIFFERENCE = 10000;

    private final CircleDockerBridge circleDockerBridge = new CircleDockerBridge(HostPort.map(port -> port - PORT_DIFFERENCE));

    @Test
    public void expose_a_port_with_a_host_port_exposed_that_can_be_used_to_talk_to_the_container() throws InterruptedException {
        new Thread(circleDockerBridge::start).start();

        Thread.sleep(100);

        ContainerId containerId = DockerTestUtils.dockerRun("-p", "39888:8000", "skyscanner/httpbin");

        DockerTestUtils.definitelyKillContainerAfter(containerId, () -> {
            try {
                Thread.sleep(100000);

                HttpURLConnection httpGet = (HttpURLConnection) new URL("http://localhost:29888/get").openConnection();
                String response = IOUtils.toString(httpGet.getInputStream());
                String yoloValue = new ObjectMapper().readTree(response)
                        .get("args")
                        .get("yolo")
                        .asText();

                assertThat(yoloValue).isEqualTo("hi");
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}