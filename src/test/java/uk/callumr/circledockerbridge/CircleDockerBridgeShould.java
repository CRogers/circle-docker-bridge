package uk.callumr.circledockerbridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.awaitility.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.callumr.circledockerbridge.docker.ContainerId;
import uk.callumr.circledockerbridge.docker.Docker;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static uk.callumr.circledockerbridge.DockerImages.SKYSCANNER_HTTPBIN;

public abstract class CircleDockerBridgeShould {
    private static final Logger log = LoggerFactory.getLogger(CircleDockerBridgeShould.class);

    protected abstract void startBridge();

    protected abstract int portMadeFor(int port);

    @BeforeClass
    public static void pullImages() {
        DockerImages.pull(SKYSCANNER_HTTPBIN);
    }

    @Test
    public void expose_a_port_with_a_host_port_exposed_that_can_be_used_to_talk_to_the_container() throws InterruptedException {
        startBridge();

        Thread.sleep(1000);

        ContainerId containerId = DockerTestUtils.dockerRun("-p", "8000", SKYSCANNER_HTTPBIN.name());

        int originalHostPort = new Docker().exposedPortsForContainer(containerId).ports().keySet().iterator().next().portNumber();
        int mappedPort = portMadeFor(originalHostPort);

        log.info("Original host port: {}, mapped host port: {}", originalHostPort, mappedPort);

        System.out.println("mappedPort = " + mappedPort);

        DockerTestUtils.definitelyKillContainerAfter(containerId, () -> await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ONE_HUNDRED_MILLISECONDS)
                .ignoreExceptions()
                .until(() -> {
                    HttpURLConnection httpGet = (HttpURLConnection) new URL(String.format("http://localhost:%d/get?yolo=hi", mappedPort)).openConnection();
                    String response = IOUtils.toString(httpGet.getInputStream());
                    return new ObjectMapper().readTree(response)
                            .get("args")
                            .get("yolo")
                            .asText();
                }, equalTo("hi")));
    }
}