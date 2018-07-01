package uk.callumr.circledockerbridge;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;

import java.util.List;

public class Main {
    public static void main(String... args) throws DockerCertificateException, DockerException, InterruptedException {
        System.out.println("hi");

        DockerClient dockerClient = DefaultDockerClient.fromEnv().build();

        List<Container> containers = dockerClient.listContainers();
        System.out.println("containers = " + containers);
    }
}

/*
docker run -it -v $(pwd):/project --rm tenshi/graalvm-native-image \
  --verbose \
  -cp build/libs/circle-docker-bridge.jar \
  -H:Name=app \
  -H:Class=uk.callumr.circledockerbridge.Main \
  --static
 */