package uk.callumr.circledockerbridge.docker.json;

import com.google.gson.annotations.SerializedName;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public interface HostPortWrapperJson {
    @SerializedName("HostPort")
    String hostPort();
}
