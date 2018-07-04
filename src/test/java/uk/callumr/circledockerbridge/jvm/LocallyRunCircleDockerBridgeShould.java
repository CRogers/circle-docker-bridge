package uk.callumr.circledockerbridge.jvm;

import uk.callumr.circledockerbridge.CircleDockerBridge;
import uk.callumr.circledockerbridge.CircleDockerBridgeShould;
import uk.callumr.circledockerbridge.SocketUtils;
import uk.callumr.circledockerbridge.docker.HostPort;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocallyRunCircleDockerBridgeShould extends CircleDockerBridgeShould {
    private final ConcurrentMap<Integer, Integer> portMapping = new ConcurrentHashMap<>();
    private final CircleDockerBridge circleDockerBridge = new CircleDockerBridge(HostPort.map(this::portMadeFor));

    @Override
    protected void startBridge() {
        new Thread(circleDockerBridge::start).start();
    }

    @Override
    protected int portMadeFor(int port) {
        return portMapping.computeIfAbsent(port, absentPort -> SocketUtils.findFreePort());
    }
}
