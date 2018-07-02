package uk.callumr.circledockerbridge.docker.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import uk.callumr.circledockerbridge.docker.HostPort;

@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(as = ImmutableHostPortWrapperJson.class)
public interface HostPortWrapperJson {
    @JsonProperty("HostPort")
    HostPort hostPort();
}
