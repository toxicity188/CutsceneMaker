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
        System.out.println(httpGet("https://api.github.com/repos/toxicity188/CutsceneMaker/tags?per_page=1"));
    }

    private static String httpGet(String url) {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(new HttpGet(url))) {
            return new BufferedReader(new InputStreamReader(response.getEntity().getContent())).readLine();
        } catch (Exception e) {
            System.out.println("An error has occurred.");
            return null;
        }
    }
}