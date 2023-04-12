package kor.toxicity.cutscenemaker.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class TalkGenerator {
    private TalkGenerator() {
        throw new RuntimeException();
    }

    public static String generate(String apiKey, String job) {
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();

        JsonObject message = new JsonObject();
        message.addProperty("role","user");
        message.addProperty("content","Make me one thing a " + job + " would say in RPG game.");
        object.addProperty("model","gpt-3.5-turbo-0301");
        array.add(message);
        object.add("messages",array);

        HttpPost post = new HttpPost("https://api.openai.com/v1/chat/completions");
        post.setHeader("Authorization", "Bearer " + apiKey);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");
        try {
            post.setEntity(new StringEntity(object.toString()));
            try (CloseableHttpClient client = HttpClients.createDefault(); CloseableHttpResponse response = client.execute(post)) {
                JsonObject object1 = new JsonParser().parse(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))).readLine()).getAsJsonObject();
                return object1
                        .getAsJsonArray("choices")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("message")
                        .getAsJsonPrimitive("content")
                        .toString()
                        .replace("\"","")
                        .replace("\\","");
            } catch (Exception ignored) {}
        } catch (UnsupportedEncodingException ignored) {
        }
        return null;
    }
}