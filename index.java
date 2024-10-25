import io.supabase.Postgrest;
import io.supabase.SupabaseClient;
import io.supabase.auth.gotrue.GotrueSession;
import io.supabase.auth.gotrue.GotrueUser;
import io.supabase.exceptions.SupabaseException;
import io.supabase.gotrue.GotrueClient;
import io.supabase.gotrue.GotrueUser;
import io.supabase.gotrue.GotrueSession;
import io.supabase.realtime.RealtimeChannel;
import io.supabase.realtime.RealtimeClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseSync {

    private static final String SUPABASE_URL = ""; 
    private static final String SUPABASE_KEY = ""; 
    private static final String LOCAL_DATA_FILE = "local_data.json";  static final String TABLE_NAME = ""; 

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static void main(String[] args) throws IOException, InterruptedException {
        SupabaseClient supabase = new SupabaseClient(SUPABASE_URL, SUPABASE_KEY);
        
        JsonObject myData = new JsonObject();
        myData.addProperty("name", "John Doe");
        myData.addProperty("age", 30);
        saveToLocalStorage(myData);

        synchronizeData(myData);

        JsonObject storedData = getFromLocalStorage();
        System.out.println("Data from local storage: " + gson.toJson(storedData));

        listenForChanges(supabase);

        executor.shutdown();
        executor.awaitTermination(1, java.util.concurrent.TimeUnit.HOURS);
    }

    public static void saveToLocalStorage(JsonObject data) throws IOException {
        Path path = Paths.get(LOCAL_DATA_FILE);
        Files.write(path, gson.toJson(data).getBytes());
    }

    public static JsonObject getFromLocalStorage() throws IOException {
        Path path = Paths.get(LOCAL_DATA_FILE);
        String json = Files.readString(path);
        return gson.fromJson(json, JsonObject.class);
    }

    public static void synchronizeData(JsonObject data) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                Postgrest postgrest = new SupabaseClient(SUPABASE_URL, SUPABASE_KEY).getRealtime().getPostgrest();
                JsonElement response = postgrest.from(TABLE_NAME).select("*").eq("key", "user_data").execute();

                if (response.getAsJsonArray().size() > 0) {
                    postgrest.from(TABLE_NAME).update(data).eq("key", "user_data").execute();
                } else {
                    postgrest.from(TABLE_NAME).insert(data).execute();
                }
            } catch (SupabaseException e) {
                System.err.println("Error synchronizing data: " + e.getMessage());
            }
        }, executor);
    }

    public static void listenForChanges(SupabaseClient supabase) throws IOException, InterruptedException {
        RealtimeClient realtime = supabase.getRealtime();
        RealtimeChannel channel = realtime.channel(TABLE_NAME).on("*", (payload) -> {
            System.out.println("Data changed: " + payload.new);

            if (payload.new.getAsJsonObject().get("key").getAsString().equals("user_data")) {
                saveToLocalStorage(payload.new.getAsJsonObject());
            }
        }).subscribe();
    }
}