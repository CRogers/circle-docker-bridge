package uk.callumr.circledockerbridge.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.immutables.value.Value;

@Value.Immutable
public interface ContainerPort {
    int portNumber();

    static ContainerPort of(int portNumber) {
        return ImmutableContainerPort.builder()
                .portNumber(portNumber)
                .build();
    }

    @JsonCreator
    static ContainerPort fromBindingSpec(String bindingSpec) {
        return of(Integer.parseInt(bindingSpec.split("/")[0]));
    }
}
