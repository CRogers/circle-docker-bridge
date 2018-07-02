package uk.callumr.circledockerbridge.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.immutables.value.Value;

@Value.Immutable
public interface HostPort {
    int portNumber();

    static HostPort of(int portNumber) {
        return ImmutableHostPort.builder()
                .portNumber(portNumber)
                .build();
    }

    @JsonCreator
    static HostPort fromString(String portNumberString) {
        return of(Integer.parseInt(portNumberString));
    }
}
