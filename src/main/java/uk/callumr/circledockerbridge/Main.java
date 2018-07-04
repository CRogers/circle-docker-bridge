package uk.callumr.circledockerbridge;

import java.util.function.Function;

public class Main {
    public static void main(String... args) {
        new CircleDockerBridge(Function.identity()).start();
    }
}
