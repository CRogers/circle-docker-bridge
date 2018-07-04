package uk.callumr.circledockerbridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import uk.callumr.circledockerbridge.docker.ContainerId;
import uk.callumr.circledockerbridge.docker.Docker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class CircleDockerBridgeShould {


    protected abstract void startBridge();

    protected abstract int portMadeFor(int port);

    @Test
    public void expose_a_port_with_a_host_port_exposed_that_can_be_used_to_talk_to_the_container() throws InterruptedException {
        startBridge();

        Thread.sleep(1000);

        ContainerId containerId = DockerTestUtils.dockerRun("-p", "8000", "skyscanner/httpbin");

        int originalHostPort = new Docker().exposedPortsForContainer(containerId).ports().keySet().iterator().next().portNumber();

        DockerTestUtils.definitelyKillContainerAfter(containerId, () -> {
            try {
                Thread.sleep(1000);

                HttpURLConnection httpGet = (HttpURLConnection) new URL(String.format("http://localhost:%d/get?yolo=hi", portMadeFor(originalHostPort))).openConnection();
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