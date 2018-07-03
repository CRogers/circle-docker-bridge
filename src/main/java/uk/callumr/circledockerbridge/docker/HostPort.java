package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

@Value.Immutable
public interface HostPort {
    int portNumber();

    static HostPort of(int portNumber) {
        return ImmutableHostPort.builder()
                .portNumber(portNumber)
                .build();
    }

    static HostPort fromString(String portNumberString) {
        return of(Integer.parseInt(portNumberString));
    }
}
