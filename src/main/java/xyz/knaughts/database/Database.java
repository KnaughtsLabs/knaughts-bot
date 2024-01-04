package xyz.knaughts.database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.goterl.lazysodium.exceptions.SodiumException;
import xyz.knaughts.exceptions.NoteNotFoundException;
import xyz.knaughts.exceptions.NotesListNotFoundException;
import xyz.knaughts.models.Note;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import xyz.knaughts.models.NotesResponse;
import xyz.knaughts.utils.Cryptography;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static xyz.knaughts.KnaughtsBot.LOG;

/**
 * A wrapper around the Knaughts bot database. Knaughts bot uses <a href="https://pocketbase.io/">PocketBase</a> as its
 * database. As of late 2023, there is no Java SDK so the REST API is used.
 */
public final class Database {

    /**
     * The HTTP client used to make requests to the database. Knaughts bot uses <a href="https://square.github.io/okhttp/">OkHttp</a>
     * for HTTP-related functionality.
     */
    private static final OkHttpClient client = new OkHttpClient();

    /**
     * The base URL of the database.
     */
    private static final String BASE_URL = System.getenv("KNAUGHTS_DB_BASE_URL");

    /**
     * The admin auth token for the database. Currently, all requests are made via an admin account so that all
     * users are able to use the database for their notes securely.
     */
    private static String AUTH_TOKEN;

    /**
     * The Gson instance used to parse JSON responses from the database.
     */
    private static final Gson gson = new Gson();

    private Database() {
    }

    /**
     * Sends an initial authentication request to the database and starts a {@link TokenRefresh} thread to refresh it
     * periodically
     */
    public static void init() {
        LOG.info("Initialising database...");

        if (BASE_URL == null || BASE_URL.isEmpty())
            throw new RuntimeException("KNAUGHTS_DB_BASE_URL environment variable not found/set.");

        AUTH_TOKEN = authenticate(false);

        new TokenRefresh().start();

        LOG.info("Database initialised.");
    }

    /**
     * Authenticates an admin account with the database and returns the auth token.
     * @param reAuth whether to re-authenticate with the database.
     * @return the admin auth token.
     */
    private static String authenticate(boolean reAuth) {
        String token;

        final RequestBody body = new FormBody.Builder()
                .add("identity", System.getenv("KNAUGHTS_DB_IDENTITY"))
                .add("password", System.getenv("KNAUGHTS_DB_PASSWORD"))
                .build();

        final Request r = reAuth
                ? new Request.Builder()
                    .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                    .url(buildUrl("/api/admins/auth-refresh"))
                    .post(RequestBody.create(new byte[0]))
                    .build()
                : new Request.Builder()
                    .url(buildUrl("/api/admins/auth-with-password"))
                    .addHeader("Content-Type", "multipart/form-data")
                    .post(body)
                    .build();

        try (Response resp = client.newCall(r).execute()) {
            if (resp.body() != null && resp.code() == 200) {
                final JsonObject json = new Gson().fromJson(resp.body().string(), JsonObject.class);
                token = json.get("token").getAsString();
                LOG.info("Admin auth token successfully generated.");
            } else {
                throw new RuntimeException("Response for admin auth token not okay.");
            }
        } catch (Exception e) {
            throw new RuntimeException("There was an error getting the admin auth token: " + e);
        }

        return token;
    }

    /**
     * Sends a request to the database to create a new note with an encrypted content and title.
     * @param userId the Discord user ID of the user creating the note.
     * @param title the plaintext title of the note.
     * @param content the plaintext content of the note.
     * @return a {@link CompletableFuture} that completes with the ID of the note.
     */
    public static CompletableFuture<String> postNote(String userId, String title, String content) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        String encryptedContent, encryptedTitle;
        try {
            encryptedContent = Cryptography.encrypt(content);
            encryptedTitle = Cryptography.encrypt(title);
        } catch (SodiumException e) {
            future.completeExceptionally(errorOf("Failed encrypting note: " + e));
            return future;
        }

        final Map<String, String> parts = Map.of(
                "discord_user_id", userId,
                "title", encryptedTitle,
                "content", encryptedContent
        );
        final String url = "/api/collections/notes/records";

        client.newCall(postRequest(url, parts)).enqueue(
            new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    future.completeExceptionally(errorOf("Failed posting to `notes` on the database: " + e));
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.code() != 200 || response.body() == null) {
                        future.completeExceptionally(errorOf("There was an error posting to `notes` on the database."));
                    } else {
                        final String json = response.body().string();
                        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                        future.complete(jsonObject.get("id").getAsString());
                    }
                }
            }
        );
        return future;
    }

    /**
     * Sends a request to the database to update a note with an encrypted content and title.
     * @param userId the Discord user ID of the user updating the note.
     * @param noteId the ID of the note to update.
     * @param title the plaintext title of the note.
     * @param content the plaintext content of the note.
     * @return a {@link CompletableFuture} that completes with the updated note.
     */
    public static CompletableFuture<Note> patchNote(String userId, String noteId, String title, String content) {
        final CompletableFuture<Note> future = new CompletableFuture<>();
        final String encryptedContent, encryptedTitle;
        try {
            encryptedContent = Cryptography.encrypt(content);
            encryptedTitle = Cryptography.encrypt(title);
        } catch (SodiumException e) {
            future.completeExceptionally(errorOf("Failed encrypting note: " + e));
            return future;
        }

        final Map<String, String> parts = Map.of(
                "discord_user_id", userId,
                "title", encryptedTitle,
                "content", encryptedContent
        );
        final String url = "/api/collections/notes/records/" + noteId + "?fields=id,title,content,created,updated";

        client.newCall(patchRequest(url, parts)).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        future.completeExceptionally(errorOf("Failed patching to `notes` on the database: " + e));
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if (response.code() != 200 || response.body() == null) {
                            future.completeExceptionally(errorOf("There was an error patching to `notes` on the database."));
                        } else {
                            final String json = response.body().string();
                            final JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

                            // using the edited title and content param values to save having to decrypt
                            future.complete(new Note(
                                    jsonObject.get("id").getAsString(),
                                    title,
                                    content,
                                    jsonObject.get("created").getAsString(),
                                    jsonObject.get("updated").getAsString())
                            );
                        }
                    }
                }
        );

        return future;
    }

    /**
     * Sends a request to the database to create a new server record with `bot_in_server` set to true.
     * @param serverId the ID of the server to create.
     */
    public static void postServer(String serverId) {
        final String url = "/api/collections/servers/records";
        final Map<String, String> parts = Map.of(
                "guild_id", serverId,
                "bot_in_server", String.valueOf(true)
        );

        client.newCall(postRequest(url, parts)).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        LOG.error("Failed posting to `servers` on the database: " + e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        int code = response.code();
                        if (code != 200 && code != 400) { // 400 means the server already exists
                            LOG.error("There was an error posting to `servers` on the database with server id " + serverId);
                        } else if (code == 400) {
                            Database.patchServer(Long.parseLong(serverId), true);
                        }
                    }
                }
        );
    }

    /**
     * Sends a request to the database to update a server record.
     * @param serverId the ID of the server to update.
     */
    public static void patchServer(long serverId, boolean botInServer) {
        final String url = "/api/collections/servers/records/" + serverId;
        final Map<String, String> parts = Map.of(
                "bot_in_server", String.valueOf(botInServer)
        );

        client.newCall(patchRequest(url, parts)).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        LOG.error("Failed patching to `servers` on the database: " + e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        if (response.code() != 200) {
                            LOG.error("There was an error patching to `servers` on the database with id " + serverId);
                        }
                    }
                }
        );
    }

    /**
     * Sends a request to the database to get a list of notes for a user.
     * @param userId the Discord user ID of the user to get notes for.
     * @param page the page of notes to get.
     * @return a {@link CompletableFuture} that completes with a {@link NotesResponse} containing the notes.
     */
    public static CompletableFuture<NotesResponse> getNotes(String userId, int page) {
        final String url = "/api/collections/notes/records?perPage=3&page=" + page + "&filter=(discord_user_id='" + userId + "')&fields=id,title,content,created,updated&sort=-created";
        final List<Note> notes = new ArrayList<>();
        final CompletableFuture<NotesResponse> future = new CompletableFuture<>();

        client.newCall(getRequest(url)).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        future.completeExceptionally(errorOf("Failed getting to `notes` on the database: " + e));
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if (response.code() != 200 || response.body() == null) {
                            future.completeExceptionally(errorOf("There was an error getting to `notes` on the database."));
                            return;
                        }

                        final String json = response.body().string();
                        final Map<String, Object> responseMap = gson.fromJson(json, Map.class);
                        final List<Map<String, String>> items = (List<Map<String, String>>) responseMap.get("items");

                        for (final Map<String, String> item : items) {
                            final String decryptedContent, decryptedTitle;
                            try {
                                decryptedContent = Cryptography.decrypt(item.get("content"));
                                decryptedTitle = Cryptography.decrypt(item.get("title"));
                            } catch (SodiumException e) {
                                future.completeExceptionally(errorOf("Failed decrypting note: " + e));
                                return;
                            }
                            notes.add(new Note(item.get("id"), decryptedTitle, decryptedContent, item.get("created"), item.get("updated")));
                        }

                        if (items.isEmpty())
                            future.completeExceptionally(new NotesListNotFoundException(("No notes found.")));
                        else
                            future.complete(new NotesResponse(notes, (int) ((double) responseMap.get("page")),  (int) ((double) responseMap.get("totalPages"))));
                    }
                }
        );

        return future;
    }

    /**
     * Sends a request to the database to get a single note for a user.
     * @param noteId the ID of the note to get.
     * @param userId the Discord user ID of the user to get the note for.
     * @return a {@link CompletableFuture} that completes with a {@link Note}.
     */
    public static CompletableFuture<Note> getNote(String noteId, String userId) {
        final String url = "/api/collections/notes/records?filter=(id='" + noteId + "' && discord_user_id='" + userId + "')&fields=id,title,content,created,updated";
        final CompletableFuture<Note> future = new CompletableFuture<>();

        client.newCall(getRequest(url)).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        future.completeExceptionally(errorOf("Failed getting a note on `notes` on the database: " + e));
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if (response.code() != 200 || response.body() == null) {
                            future.completeExceptionally(errorOf("There was an error getting a note on the database."));
                        } else {
                            final String json = response.body().string();
                            final Map<String, Object> responseMap = gson.fromJson(json, Map.class);
                            final List<Map<String, String>> items = (List<Map<String, String>>) responseMap.get("items");

                            if (items.isEmpty()) {
                                future.completeExceptionally(new NoteNotFoundException("Note not found."));
                            } else {
                                final String decryptedContent, decryptedTitle;
                                try {
                                    decryptedContent = Cryptography.decrypt(items.get(0).get("content"));
                                    decryptedTitle = Cryptography.decrypt(items.get(0).get("title"));
                                } catch (SodiumException e) {
                                    future.completeExceptionally(errorOf("Failed decrypting note: " + e));
                                    return;
                                }

                                future.complete(new Note(
                                        items.get(0).get("id"),
                                        decryptedTitle,
                                        decryptedContent,
                                        items.get(0).get("created"),
                                        items.get(0).get("updated")
                                ));
                            }
                        }

                    }
                }
        );

        return future;
    }

    /**
     * Sends a request to the database to delete a note.
     * @param noteId the ID of the note to delete.
     * @param userId the Discord user ID of the user to delete the note for.
     * @return a {@link CompletableFuture} that completes with a boolean indicating whether the note was successfully deleted.
     */
    public static CompletableFuture<Boolean> deleteNote(String noteId, String userId) {
        final String url = "/api/collections/notes/records/" + noteId + "?filter=(discord_user_id='" + userId + "')";
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        client.newCall(deleteRequest(url)).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        LOG.error("Failed deleting a note on `notes` on the database: " + e);
                        future.complete(false);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        if (response.code() != 204) {
                            LOG.error("There was an error deleting a note on the database.");
                            future.complete(false);
                        } else {
                            future.complete(true);
                        }
                    }
                }
        );

        return future;
    }

    /**
     * Returns a new {@link RuntimeException} with a given message.
     * @param msg the message for the exception.
     * @return a new {@link RuntimeException}.
     */
    private static RuntimeException errorOf(String msg) {
        LOG.error(msg);
        return new RuntimeException(msg);
    }

    /**
     * Returns a generic HTTP POST request with the given parts and a default Content-Type header.
     * @param url the URL to send the request to.
     * @param parts the multipart parts to send with the request.
     * @return a {@link Request}.
     */
    private static Request postRequest(String url, Map<String, String> parts) {
        return defaultRequestBuilder(url).addHeader("Content-Type", "multipart/form-data").post(defaultMultipartReq(parts)).build();
    }

    /**
     * Returns a generic HTTP PATCH request with the given parts and a default Content-Type header.
     * @param url the URL to send the request to.
     * @param parts the multipart parts to send with the request.
     * @return a {@link Request}.
     */
    private static Request patchRequest(String url, Map<String, String> parts) {
        return defaultRequestBuilder(url).addHeader("Content-Type", "multipart/form-data").patch(defaultMultipartReq(parts)).build();
    }

    /**
     * Returns a generic HTTP GET request.
     * @param url the URL to send the request to.
     * @return a {@link Request}.
     */
    private static Request getRequest(String url) {
        return defaultRequestBuilder(url).get().build();
    }

    /**
     * Returns a generic HTTP DELETE request.
     * @param url the URL to send the request to.
     * @return a {@link Request}.
     */
    private static Request deleteRequest(String url) {
        return defaultRequestBuilder(url).delete().build();
    }

    /**
     * Returns a default multipart {@link RequestBody} with the given parts.
     * @param parts the multipart parts to send with the request.
     * @return a {@link RequestBody}.
     */
    private static RequestBody defaultMultipartReq(Map<String, String> parts) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        parts.forEach(builder::addFormDataPart);
        return builder.build();
    }

    /**
     * Returns a default {@link Request.Builder} with the given URL and a default authorization header.
     * @param url the URL path to send the request to (excluding the base part).
     * @return a {@link Request.Builder}.
     */
    private static Request.Builder defaultRequestBuilder(String url) {
        return new Request.Builder()
                .url(buildUrl(url))
                .addHeader("Authorization", "Bearer " + AUTH_TOKEN);
    }

    /**
     * Returns the full database URL given a path.
     * @param path the path to append to the base URL, beginning with a slash.
     * @return the full database URL.
     */
    private static String buildUrl(String path) {
        return BASE_URL + path.replaceAll("&&", "%26%26");
    }

    private static synchronized void setAuthToken(String token) {
        AUTH_TOKEN = token;
    }

    /**
     * A class that handles refreshing the admin auth token every X amount of time. This is configured inside your
     * database. For PocketBase, see the <a href="https://pocketbase.io/docs/api-admins/#auth-refresh">refresh docs</a>.
     */
    private static class TokenRefresh {
        private final long REFRESH_INTERVAL = Long.parseLong(System.getenv("KNAUGHTS_DB_ADMIN_REFRESH_INTERVAL"));
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        private void start() {
            final Runnable task = this::refreshToken;
            scheduler.scheduleAtFixedRate(task, REFRESH_INTERVAL, REFRESH_INTERVAL, TimeUnit.MILLISECONDS);
        }

        private void refreshToken() {
            final String newAuthToken = authenticate(true);
            setAuthToken(newAuthToken);
        }
    }
}
