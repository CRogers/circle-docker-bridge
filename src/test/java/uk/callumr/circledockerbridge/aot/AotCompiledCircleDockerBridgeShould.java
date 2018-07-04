package uk.callumr.circledockerbridge.aot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jInfoOutputStream;
import uk.callumr.circledockerbridge.CircleDockerBridgeShould;

import java.io.IOException;

public class AotCompiledCircleDockerBridgeShould extends CircleDockerBridgeShould {
    private final Logger log = LoggerFactory.getLogger(AotCompiledCircleDockerBridgeShould.class);

    @Override
    protected void startBridge() {
        try {
            new ProcessExecutor()
                    .command("build/native/circle-docker-bridge")
                    .exitValue(0)
                    .redirectOutput(new Slf4jInfoOutputStream(log))
                    .redirectError(new Slf4jInfoOutputStream(log))
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int portMadeFor(int port) {
        return port;
    }
}
