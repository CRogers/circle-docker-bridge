package uk.callumr.circledockerbridge.docker.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import one.util.streamex.EntryStream;
import org.immutables.gson.Gson.TypeAdapters;
import org.immutables.value.Value;
import uk.callumr.circledockerbridge.docker.ContainerPort;
import uk.callumr.circledockerbridge.docker.HostPort;
import uk.callumr.circledockerbridge.docker.PortMapping;

import java.util.*;

@Value.Immutable
@TypeAdapters
public abstract class PortMappingJson {
    protected abstract Map<String, Optional<List<HostPortWrapperJson>>> ports();

    @Value.Lazy
    protected PortMapping portMapping() {
        Map<HostPort, ContainerPort> portMapping = EntryStream.of(ports())
                .mapValues(optionalList -> optionalList.orElse(Collections.emptyList()))
                .invert()
                .flatMapKeys(Collection::stream)
                .mapKeys(HostPortWrapperJson::hostPort)
                .mapKeys(HostPort::fromString)
                .mapValues(ContainerPort::fromBindingSpec)
                .toMap();

        return PortMapping.builder()
                .ports(portMapping)
                .build();
    }

    public static PortMapping fromDockerJson(String json) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
            gsonBuilder.registerTypeAdapterFactory(factory);
        }

        Gson gson = gsonBuilder.create();

        PortMappingJson portMappingJson = gson.fromJson(json, PortMappingJson.class);
        return portMappingJson.portMapping();
    }
}
