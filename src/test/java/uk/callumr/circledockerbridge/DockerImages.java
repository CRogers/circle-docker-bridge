package uk.callumr.circledockerbridge;

import uk.callumr.circledockerbridge.docker.ImageName;

import java.util.stream.Stream;

public enum DockerImages {
    ;

    public static final ImageName CROGERS_EXPOSED_PORT = ImageName.of("crogers/exposed-port-not-opened-behind-it");
    public static final ImageName BUSYBOX = ImageName.of("busybox");
    public static final ImageName SKYSCANNER_HTTPBIN = ImageName.of("skyscanner/httpbin");

    public static void pullAll() {
        Stream.of(CROGERS_EXPOSED_PORT, BUSYBOX, SKYSCANNER_HTTPBIN)
                .parallel()
                .forEach(DockerImages::pull);
    }

    public static String pull(ImageName imageName) {
        return DockerTestUtils.docker(true, "pull", imageName.name());
    }
}
