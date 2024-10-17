package dolozimm.apoapsis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.text.Text;

import com.sun.net.httpserver.HttpServer;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.lwjgl.glfw.GLFW;

public class ApoapsisClient implements ClientModInitializer {
    private static final int PORT = 8000;
    private final MinecraftClient minecraft = MinecraftClient.getInstance();
    private static String lastCommand = "";
    private static KeyBinding openKeyBinding;
    private static String htmlContent = "";

    @Override
    public void onInitializeClient() {
    	openKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
    			"key.apoapsis.open",
    			InputUtil.Type.KEYSYM,
    			GLFW.GLFW_KEY_RIGHT_SHIFT,
    			"Apoapsis"
    			));
        try {
            loadHtmlContent();
            startHttpServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openKeyBinding.wasPressed()) { final RendererBrowser screen = new RendererBrowser(Text.literal("Basic Browser"));
                minecraft.setScreen(screen);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void loadHtmlContent() throws IOException {
        String htmlFilePath = "assets/apoapsis/index.html";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(htmlFilePath)) {
            if (is == null) {
                throw new IOException("index.html not found");
            }
            htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void startHttpServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new MyHandler());
        server.createContext("/assets/", new AssetHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("HTTP Server started on port " + PORT);
        
    }
    static class AssetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String requestedAsset = t.getRequestURI().getPath().substring("/assets/".length());
            InputStream is = getClass().getClassLoader().getResourceAsStream("assets/" + requestedAsset);
            
            if (is == null) {
                String response = "Asset not found";
                t.sendResponseHeaders(404, response.length());
                try (OutputStream os = t.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                byte[] bytes = is.readAllBytes();
                t.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = t.getResponseBody()) {
                    os.write(bytes);
                }
                is.close();
            }
        }
    }
    private void onClientTick(MinecraftClient client) {
        if (!lastCommand.isEmpty() && client.player != null) {
            ClientPlayerEntity player = client.player;
            player.networkHandler.sendChatCommand(lastCommand);
            lastCommand = "";
        }
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("GET".equals(t.getRequestMethod())) {
                handleGet(t);
            } else if ("POST".equals(t.getRequestMethod())) {
                handlePost(t);
            }
        }

        private void handleGet(HttpExchange t) throws IOException {
            byte[] responseBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            t.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = t.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        private void handlePost(HttpExchange t) throws IOException {
            String body = new String(t.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String[] pairs = body.split("&");
            String command = "";
            String prefix = "";
            
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.toString());
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.toString());
                    
                    if (key.equals("command")) {
                        command = value;
                    } else if (key.equals("prefix")) {
                        prefix = value;
                    }
                }
            }

            if (!command.isEmpty() && !prefix.isEmpty()) {
                if (!prefix.equals("nenhum")) {
                    lastCommand = prefix + " " + command;
                } else {
                    lastCommand = command;
                }
                
                String response = "Command received: " + lastCommand;
                t.sendResponseHeaders(200, response.length());
                try (OutputStream os = t.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                String response = "Invalid command or prefix";
                t.sendResponseHeaders(400, response.length());
                try (OutputStream os = t.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
