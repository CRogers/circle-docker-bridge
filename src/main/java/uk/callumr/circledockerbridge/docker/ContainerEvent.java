package uk.callumr.circledockerbridge.docker;

import org.derive4j.Data;

@Data
public abstract class ContainerEvent {
    interface Cases<R> {
        R created(ContainerId containerId);
        R destroyed(ContainerId containerId);
    }

    public abstract <R> R match(Cases<R> cases);

    @Override
    public abstract int hashCode();
    @Override
    public abstract boolean equals(Object obj);
    @Override
    public abstract String toString();
}
