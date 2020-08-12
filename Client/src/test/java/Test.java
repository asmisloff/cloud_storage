import ru.asmisloff.cloudStorage.client.core.NetworkClient;

public class Test {

    public static void main(String[] args) {
        try {
            NetworkClient c = new NetworkClient();
            c.start("localhost", 8181);
            while (!c.isConnected()) {
                Thread.sleep(100);
            }

            c.uploadFile("Файл для закачивания.txt");
            c.downloadFile("Файл для скачивания.txt");
            //c.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
