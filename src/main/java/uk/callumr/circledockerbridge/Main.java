package uk.callumr.circledockerbridge;

public class Main {
    public static void main(String... args) {
        System.out.println("hi");
    }
}

/*
docker run -it -v $(pwd):/project --rm tenshi/graalvm-native-image \
  --verbose \
  -cp build/libs/circle-docker-bridge.jar \
  -H:Name=app \
  -H:Class=uk.callumr.circledockerbridge.Main \
  --static
 */