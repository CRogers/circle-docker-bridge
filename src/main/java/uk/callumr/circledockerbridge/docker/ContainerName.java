package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

@Value.Immutable
public interface ContainerName {
    String name();

    static ContainerName of(String name) {
        return ImmutableContainerName.builder()
                .name(name)
                .build();
    }
}
