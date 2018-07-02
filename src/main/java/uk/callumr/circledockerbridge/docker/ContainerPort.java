package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

@Value.Immutable
public interface ContainerPort {
    int portNumber();

    static ContainerPort of(int portNumber) {
        return ImmutableContainerPort.builder()
                .portNumber(portNumber)
                .build();
    }

    static ContainerPort fromBindingSpec(String bindingSpec) {
        return of(Integer.parseInt(bindingSpec.split("/")[0]));
    }
}
