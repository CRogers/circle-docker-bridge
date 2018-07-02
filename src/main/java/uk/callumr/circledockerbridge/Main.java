package uk.callumr.circledockerbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    private static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) throws IOException, TimeoutException, InterruptedException {
        log.info("Started");

        new ProcessExecutor()
                .command(
                        "docker",
                        "events",
                        "--format", "{{.Status}},{{.ID}}",
                        "-f", "event=create",
                        "-f", "event=destroy",
                        "-f", "type=container")
                .redirectOutput(new LogOutputStream() {
                    @Override
                    protected void processLine(String line) {
                        log.info(line);
                    }
                })
                .destroyOnExit()
                .execute();

        log.info("Stopped");
    }
}

// docker events -f 'event=create' -f event=destroy
// Get host ports   docker inspect --format='{{range $p, $conf := .NetworkSettings.Ports}}{{(index $conf 0).HostPort}} {{end}}'

/*
docker run -it -v $(pwd):/project --rm tenshi/graalvm-native-image \
  --verbose \
  -cp build/libs/circle-docker-bridge.jar \
  -H:Name=app \
  -H:Class=uk.callumr.circledockerbridge.Main \
  --static
 */