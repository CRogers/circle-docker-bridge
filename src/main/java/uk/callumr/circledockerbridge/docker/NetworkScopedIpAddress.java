package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

@Value.Immutable
public interface NetworkScopedIpAddress {
    String ipAddress();

    static NetworkScopedIpAddress of(String ipAddress) {
        return ImmutableNetworkScopedIpAddress.builder()
                .ipAddress(ipAddress)
                .build();
    }
}
