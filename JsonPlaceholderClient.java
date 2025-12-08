package ua.opnu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.stream.Collectors;
import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class JsonPlaceholderClient {
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/";
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {

        List<JsonObject> users = getAllUsers();
        System.out.println("Усі користувачі: " + users);

        JsonObject user = getUserById(1);
        System.out.println("Користувач із id 1: " + user);

        List<JsonObject> userByUsername = getUserByUsername("Bret");
        System.out.println("Користувач із username 'Bret': " + userByUsername);

        JsonObject newUser = new JsonObject();
        newUser.addProperty("name", "John Doe");
        newUser.addProperty("username", "johndoe");
        newUser.addProperty("email", "johndoe@example.com");
        JsonObject createdUser = createUser(newUser);
        System.out.println("Новий користувач: " + createdUser);

        createdUser.addProperty("email", "newemail@example.com");
        JsonObject updatedUser = updateUser(createdUser.get("id").getAsInt(), createdUser);
        System.out.println("Оновлений користувач: " + updatedUser);

        boolean isDeleted = deleteUser(createdUser.get("id").getAsInt());
        System.out.println("Користувач видалений: " + isDeleted);


        fetchAndSaveCommentsForLastPost(1);


        List<JsonObject> openTodos = getOpenTodos(1);
        System.out.println("Відкриті задачі для користувача 1: " + openTodos);
    }


    public static List<JsonObject> getAllUsers() throws IOException {
        HttpURLConnection connection = createConnection("users", "GET");
        String response = getResponse(connection);
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        return gson.fromJson(response, listType);
    }


    public static JsonObject getUserById(int id) throws IOException {
        HttpURLConnection connection = createConnection("users/" + id, "GET");
        String response = getResponse(connection);
        return gson.fromJson(response, JsonObject.class);
    }


    public static List<JsonObject> getUserByUsername(String username) throws IOException {
        HttpURLConnection connection = createConnection("users?username=" + username, "GET");
        String response = getResponse(connection);
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        return gson.fromJson(response, listType);
    }


    public static JsonObject createUser(JsonObject newUser) throws IOException {
        HttpURLConnection connection = createConnection("users", "POST");
        sendRequestPayload(connection, newUser.toString());
        String response = getResponse(connection);
        return gson.fromJson(response, JsonObject.class);
    }


    public static JsonObject updateUser(int userId, JsonObject updatedUser) throws IOException {
        HttpURLConnection connection = createConnection("users/" + userId, "PATCH");
        sendRequestPayload(connection, updatedUser.toString());

        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            // Успішна відповідь
            String response = getResponse(connection);
            return gson.fromJson(response, JsonObject.class);
        } else {
            // Помилка
            String errorResponse = getResponse(connection);
            System.err.println("Error updating user. HTTP Code: " + responseCode);
            System.err.println("Server response: " + errorResponse);
            throw new IOException("Failed to update user. HTTP response code: " + responseCode);
        }
    }

    public static boolean deleteUser(int userId) throws IOException {
        HttpURLConnection connection = createConnection("users/" + userId, "DELETE");
        int responseCode = connection.getResponseCode();
        return responseCode >= 200 && responseCode < 300;
    }


    public static void fetchAndSaveCommentsForLastPost(int userId) throws IOException {
        // Отримати всі пости користувача
        HttpURLConnection connection = createConnection("users/" + userId + "/posts", "GET");
        String response = getResponse(connection);
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        List<JsonObject> posts = gson.fromJson(response, listType);


        JsonObject lastPost = posts.stream()
                .max((p1, p2) -> Integer.compare(p1.get("id").getAsInt(), p2.get("id").getAsInt()))
                .orElseThrow(() -> new RuntimeException("No posts found for user " + userId));


        int postId = lastPost.get("id").getAsInt();
        connection = createConnection("posts/" + postId + "/comments", "GET");
        response = getResponse(connection);
        JsonArray comments = gson.fromJson(response, JsonArray.class);


        String filename = "user-" + userId + "-post-" + postId + "-comments.json";
        try (FileWriter fileWriter = new FileWriter(filename)) {
            gson.toJson(comments, fileWriter);
            System.out.println("Comments saved to " + filename);
        }
    }


    public static List<JsonObject> getOpenTodos(int userId) throws IOException {
        HttpURLConnection connection = createConnection("users/" + userId + "/todos", "GET");
        String response = getResponse(connection);
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        List<JsonObject> todos = gson.fromJson(response, listType);


        return todos.stream()
                .filter(todo -> !todo.get("completed").getAsBoolean())
                .collect(Collectors.toList());

    }


    private static HttpURLConnection createConnection(String endpoint, String method) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();


        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);


        if ("PATCH".equalsIgnoreCase(method)) {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        } else {
            connection.setRequestMethod(method);
        }

        return connection;
    }

    private static String getResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static void sendRequestPayload(HttpURLConnection connection, String payload) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }


}