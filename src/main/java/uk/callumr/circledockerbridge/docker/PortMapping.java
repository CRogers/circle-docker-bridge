package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
public abstract class PortMapping {
    protected abstract Map<HostPort, ContainerPort> ports();

    public static class Builder extends ImmutablePortMapping.Builder {}

    public static Builder builder() {
        return new Builder();
    }
}
