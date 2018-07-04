package uk.callumr.circledockerbridge.docker;

import com.fasterxml.jackson.databind.JsonNode;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
public interface PortMapping {
    Map<HostPort, ContainerPort> ports();

    static PortMapping fromDockerJson(JsonNode json) {
        Map<HostPort, ContainerPort> portMapping = EntryStream.of(json.fields())
                .mapKeys(ContainerPort::fromBindingSpec)
                .mapValues(possiblyNullHostPorts -> {
                    return Optional.ofNullable(possiblyNullHostPorts)
                            .map(hostPortsJson -> {
                                return StreamEx.of(hostPortsJson.iterator())
                                        .map(hostPort -> {
                                            JsonNode hostPort1 = hostPort.get("HostPort");
                                            return hostPort1.asText();
                                        })
                                        .map(HostPort::fromString)
                                        .collect(Collectors.toList());
                            })
                            .orElse(Collections.emptyList());
                })
                .invert()
                .flatMapKeys(Collection::stream)
                .toMap();

        return builder()
                .ports(portMapping)
                .build();
    }

    class Builder extends ImmutablePortMapping.Builder {}

    static Builder builder() {
        return new Builder();
    }
}
