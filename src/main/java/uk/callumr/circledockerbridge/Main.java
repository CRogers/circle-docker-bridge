package uk.callumr.circledockerbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.callumr.circledockerbridge.docker.ContainerEvents;
import uk.callumr.circledockerbridge.docker.Docker;
import uk.callumr.circledockerbridge.docker.PortMapping;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) throws IOException {
        log.info("Started");

        try (ServerSocket waitSocket = new ServerSocket(6789);
             Socket socket = waitSocket.accept()) {

            new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();

            new DataOutputStream(socket.getOutputStream()).writeUTF("yay!\n");
        }

        Docker docker = new Docker();

        docker.createdAndDestroyedContainers().forEach(containerEvent -> {
            ContainerEvents.caseOf(containerEvent)
                    .created(containerId -> {
                        PortMapping portMapping = docker.exposedPortsForContainer(containerId);
                        log.info("Ports for newly created container {}: {}", containerId.id(), portMapping);
                        return null;
                    })
                    .destroyed(containerId -> {
                        log.info("Container {} destroyed", containerId.id());
                        return null;
                    });
        });

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