import ru.asmisloff.cloudStorage.client.core.NetworkClient;

public class Test {

    public static void main(String[] args) {
        for (int i = 0; i < 1; i++) {
            try {
                NetworkClient c = new NetworkClient();
                c.start("localhost", 8181);
                while (!c.isConnected()) {
                    Thread.sleep(100);
                }

                c.login("user2", "pwd2");
                c.uploadFile("Файл для закачивания.txt");
                c.downloadFile("Файл для скачивания.txt");
//                c.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
