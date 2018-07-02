package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

@Value.Immutable
public interface ContainerId {
    String id();

    static ContainerId of(String id) {
        return ImmutableContainerId.builder()
                .id(id)
                .build();
    }
}
