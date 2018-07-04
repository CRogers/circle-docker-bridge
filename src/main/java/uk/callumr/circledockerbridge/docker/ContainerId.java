package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

@Value.Immutable
public interface ContainerId {
    String id();

    default String shortId() {
        return id().substring(0, 6);
    }

    static ContainerId of(String id) {
        return ImmutableContainerId.builder()
                .id(id)
                .build();
    }
}
