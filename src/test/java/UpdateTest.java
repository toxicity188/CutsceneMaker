import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UpdateTest {
    @Test
    public void testUpdate() {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(new HttpGet("https://api.github.com/repos/toxicity188/CutsceneMaker/tags"))) {
            System.out.println(new JsonParser().parse(new BufferedReader(new InputStreamReader(response.getEntity().getContent()))).getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString());
        } catch (Exception e) {
            System.out.println("An error has occurred.");
        }
    }
}
