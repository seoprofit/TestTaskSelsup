import org.example.CrptApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class CrptApiTest {

    @Test
    public void blockingCreateDocumentsByLimitTest() {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 2);

        CrptApi.DocumentGoods documentGoods = CrptApi.DocumentGoods.builder().build();
        String s1 = "1212";
        Assertions.assertThrows(RuntimeException.class, () -> {
            for (int i = 0; i < 3; i++) {
                crptApi.createDocument(documentGoods, s1);
            }
        });
    }
}
