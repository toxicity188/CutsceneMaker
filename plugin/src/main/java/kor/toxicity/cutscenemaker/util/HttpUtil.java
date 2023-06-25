package kor.toxicity.cutscenemaker.util;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.UUID;

public class HttpUtil {
    private HttpUtil() {
        throw new RuntimeException();
    }

    public static String callChatGPT(String apiKey, String job) {
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
                JsonObject object1 = getElement(response).getAsJsonObject();
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

    private static JsonElement getElement(CloseableHttpResponse response) throws IOException {
        InputStream stream = response.getEntity().getContent();
        JsonElement element = new JsonParser().parse(new BufferedReader(new InputStreamReader(stream)));
        stream.close();
        return element;
    }
    public static GameProfile getPlayerGameProfile(String name) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet("https://api.mojang.com/users/profiles/minecraft/"+name))) {
                JsonPrimitive uuid = getElement(response).getAsJsonObject().getAsJsonPrimitive("id");
                if (uuid != null) {
                    try (CloseableHttpResponse response1 = client.execute(new HttpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.getAsString() + "?unsigned=false"))) {
                        JsonObject object = getElement(response1).getAsJsonObject().getAsJsonArray("properties").get(0).getAsJsonObject();
                        GameProfile profile = new GameProfile(UUID.randomUUID(),name);
                        profile.getProperties().put("textures",new Property("textures",object.getAsJsonPrimitive("value").getAsString(),object.getAsJsonPrimitive("signature").getAsString()));
                        return profile;
                    } catch (Exception e) {
                        return null;
                    }
                } else {
                    return new GameProfile(UUID.randomUUID(),name);
                }
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}