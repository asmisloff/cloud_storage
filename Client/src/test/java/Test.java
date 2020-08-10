import ru.asmisloff.cloudStorage.client.core.NetworkClient;

import java.io.File;

public class Test {

    public static void main(String[] args) {
        try {
            NetworkClient c = new NetworkClient();
            c.start("localhost", 8181);
            while (!c.isConnected()) {
                Thread.sleep(100);
            }

            c.downloadFile(new File("w.txt"));
            c.uploadFile(new File("record_60102.mp4"));
            //c.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
