package uk.callumr.circledockerbridge;

import uk.callumr.circledockerbridge.docker.HostPort;

public class LocallyRunCircleDockerBridgeShould extends CircleDockerBridgeShould {
    private static final int PORT_DIFFERENCE = 10000;
    private final CircleDockerBridge circleDockerBridge = new CircleDockerBridge(HostPort.map(this::portMadeFor));

    @Override
    protected void startBridge() {
        new Thread(circleDockerBridge::start).start();
    }

    @Override
    protected int portMadeFor(int port) {
        return port - PORT_DIFFERENCE;
    }
}
