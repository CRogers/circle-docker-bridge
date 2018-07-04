package uk.callumr.circledockerbridge.docker;

import org.immutables.value.Value;

@Value.Immutable
public interface ImageName {
    String name();

    static ImageName of(String name) {
        return ImmutableImageName.builder()
                .name(name)
                .build();
    }
}
