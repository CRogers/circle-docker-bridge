package uk.callumr.circledockerbridge.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.immutables.value.Value;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
public abstract class PortMapping {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module());

    protected abstract Map<HostPort, ContainerPort> ports();

    public static PortMapping fromDockerJson(String json) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
            Map<HostPort, ContainerPort> portMapping = EntryStream.of(jsonNode.fields())
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder extends ImmutablePortMapping.Builder {}

    public static Builder builder() {
        return new Builder();
    }
}
