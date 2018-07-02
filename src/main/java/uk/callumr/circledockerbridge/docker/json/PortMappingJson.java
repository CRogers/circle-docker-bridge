package uk.callumr.circledockerbridge.docker.json;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import one.util.streamex.EntryStream;
import org.immutables.value.Value;
import uk.callumr.circledockerbridge.docker.ContainerPort;
import uk.callumr.circledockerbridge.docker.HostPort;
import uk.callumr.circledockerbridge.docker.PortMapping;

import java.io.IOException;
import java.util.*;

@Value.Immutable
@JsonDeserialize(as = ImmutablePortMappingJson.class)
public abstract class PortMappingJson {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module());

    @JsonValue
    protected abstract Map<ContainerPort, Optional<List<HostPortWrapperJson>>> ports();

    @Value.Lazy
    protected PortMapping portMapping() {
        Map<HostPort, ContainerPort> portMapping = EntryStream.of(ports())
                .mapValues(optionalList -> optionalList.orElse(Collections.emptyList()))
                .invert()
                .flatMapKeys(Collection::stream)
                .mapKeys(HostPortWrapperJson::hostPort)
                .toMap();

        return PortMapping.builder()
                .ports(portMapping)
                .build();
    }

    public static PortMapping fromDockerJson(String json) {
        System.out.println("json = " + json);

        try {
            PortMappingJson portMappingJson = OBJECT_MAPPER.readValue(json, PortMappingJson.class);
            return portMappingJson.portMapping();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
