package ru.gravit.launchermodules.legacysupport;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.HTTPRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Pattern;

public final class MojangAuthProvider extends AuthProvider {
    private static final Pattern UUID_REGEX = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    private static final URL URL;
    private static final Gson gson = new Gson();

    static {
        try {
            URL = new URL("https://authserver.mojang.com/authenticate");
        } catch (MalformedURLException e) {
            throw new InternalError(e);
        }
    }

    public static class mojangAgent {
        public String name;
        public int version;
    }

    public static class mojangAuth {
        public mojangAuth(String username, String password) {
            this.username = username;
            this.password = password;
            agent = new mojangAgent();
            agent.name = "Minecraft";
            agent.version = 1;
        }

        public mojangAgent agent;
        public String username;
        public String password;
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws Exception {
        mojangAuth mojangAuth = new mojangAuth(login, password);
        JsonElement request = gson.toJsonTree(mojangAuth);

        // Verify there's no error
        JsonObject response = HTTPRequest.jsonRequest(request, URL).getAsJsonObject();
        if (response == null)
            authError("Empty com.mojang response");
        JsonElement errorMessage = response.get("errorMessage");
        if (errorMessage != null)
            authError(errorMessage.getAsString());

        // Parse JSON data
        JsonObject selectedProfile = response.get("selectedProfile").getAsJsonObject();
        String username = selectedProfile.get("name").getAsString();
        String accessToken = response.get("accessToken").getAsString();
        UUID uuid = UUID.fromString(UUID_REGEX.matcher(selectedProfile.get("id").getAsString()).replaceFirst("$1-$2-$3-$4-$5"));
        String launcherToken = response.get("clientToken").getAsString();

        // We're done
        return new MojangAuthProviderResult(username, accessToken, uuid, launcherToken);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
