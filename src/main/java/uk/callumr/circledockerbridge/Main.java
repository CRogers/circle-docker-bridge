package uk.callumr.circledockerbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.callumr.circledockerbridge.docker.Docker;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

        log.info("Stopped");
    }
}

// docker events -f 'event=create' -f event=destroy
// Get host ports   docker inspect --format='{{range $p, $conf := .NetworkSettings.Ports}}{{(index $conf 0).HostPort}} {{end}}'