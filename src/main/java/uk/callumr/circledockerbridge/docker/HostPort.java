package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

import java.util.function.Function;

@Value.Immutable
public interface HostPort {
    int portNumber();

    static HostPort of(int portNumber) {
        return ImmutableHostPort.builder()
                .portNumber(portNumber)
                .build();
    }

    static Function<HostPort, HostPort> map(Function<Integer, Integer> function) {
        return hostPort -> HostPort.of(function.apply(hostPort.portNumber()));
    }

    static HostPort fromString(String portNumberString) {
        return of(Integer.parseInt(portNumberString));
    }
}
