package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

@Value.Immutable
public interface NetworkAlias {
    String alias();

    static NetworkAlias of(String id) {
        return ImmutableNetworkAlias.builder()
                .alias(id)
                .build();
    }
}
