package uk.callumr.circledockerbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import uk.callumr.circledockerbridge.docker.ContainerId;
import uk.callumr.circledockerbridge.docker.NetworkAlias;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum DockerTestUtils {
    ;


    private static final Logger log = LoggerFactory.getLogger(DockerTestUtils.class);

    public static String removeNetwork(NetworkAlias networkAlias) {
        return docker(true, "network", "rm", networkAlias.alias());
    }

    public static ContainerId dockerRun(String... args) {
        List<String> command = Stream.concat(
                Stream.of("run", "-d", "--rm"),
                Arrays.stream(args)
        ).collect(Collectors.toList());

        ContainerId containerId = ContainerId.of(docker(false, command));

        log.info("Created container with id '{}'", containerId.shortId());

        return containerId;
    }

    private static String docker(boolean includeError, List<String> args) {
        List<String> command = Stream.concat(
                Stream.of("docker"),
                args.stream()
        ).collect(Collectors.toList());

        ProcessResult processResult;

        try {
            processResult = new ProcessExecutor()
                    .command(command)
                    .readOutput(true)
                    .redirectErrorStream(includeError)
                    .exitValue(0)
                    .execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        return processResult.outputUTF8().trim();
    }

    public static String docker(boolean includeError, String... args) {
        return docker(includeError, Arrays.asList(args));
    }

    public static void killContainer(ContainerId containerId) {
        docker(true, "kill", containerId.id());

        log.info("Kill container with id '{}'", containerId.shortId());
    }

    public static void definitelyKillContainerAfter(ContainerId containerId, Runnable runnable) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                killContainer(containerId);
            } catch (RuntimeException e) {
                // ignore
            }
        }));

        try {
            runnable.run();
        } finally {
            killContainer(containerId);
        }
    }
}
