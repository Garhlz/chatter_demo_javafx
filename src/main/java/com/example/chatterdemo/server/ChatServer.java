package com.example.chatterdemo.server;

import com.example.chatterdemo.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {
  private static final int PORT = 12345;
  private static final Logger logger =
      Logger.getLogger(ChatServer.class.getName());
  private final ServerSocket serverSocket;
  private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
  private final Map<String, JsonObject> users = new ConcurrentHashMap<>();
  private final Gson gson = new Gson();
  private final ExecutorService executorService;

  private volatile boolean running = true;
  private final String usersFilePath = getUsersFilePath();

  public ChatServer() throws IOException {
    serverSocket = new ServerSocket(PORT);
    executorService = Executors.newCachedThreadPool();
    loadUsers();
    logger.info("Server initialized on port " + PORT);
  }

  public void startServer() {
    logger.info("Server started and listening on port " + PORT);
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

    try {
      while (running) {
        Socket clientSocket = serverSocket.accept();
        logger.info("New client connected: " + clientSocket.getInetAddress());
        ClientHandler handler = new ClientHandler(clientSocket);
        executorService.submit(handler::run);
      }
    } catch (IOException e) {
      if (running) {
        logger.log(Level.SEVERE, "Error accepting client connection", e);
      }
    } finally {
      shutdown();
    }
  }

  private void shutdown() {
    logger.info("Shutting down server...");
    running = false;
    for (ClientHandler handler : clients.values()) {
      handler.close();
    }
    clients.clear();
    saveUsers();
    executorService.shutdown();
    try {
      if (!serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error closing server socket", e);
    }
    logger.info("Server shutdown complete");
  }

  private synchronized void loadUsers() {
    File file = new File(usersFilePath);
    if (!file.exists()) {
      // 初始化测试数据
      users.clear();
      addTestUser("1111", "1111", "1111");
      addTestUser("2222", "2222", "2222");
      addTestUser("3333", "3333", "3333");
      saveUsers();
      logger.info("Initialized " + users.size() + " test users and saved to " +
                  usersFilePath);
      return;
    }
    try {
      String content = new String(Files.readAllBytes(Paths.get(usersFilePath)));
      JsonObject json = gson.fromJson(content, JsonObject.class);
      if (json != null) {
        users.clear();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
          users.put(entry.getKey(), entry.getValue().getAsJsonObject());
        }
        logger.info("Loaded " + users.size() + " users from " + usersFilePath);
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error loading users from file", e);
      // 加载失败时初始化测试数据
      users.clear();
      addTestUser("1111", "1111", "1111");
      addTestUser("2222", "2222", "2222");
      addTestUser("3333", "3333", "3333");
      saveUsers();
    }
  }

  private void addTestUser(String username, String password, String nickname) {
    JsonObject user = new JsonObject();
    user.addProperty("password", password);
    user.addProperty("nickname", nickname);
    users.put(username, user);
  }

  private synchronized void saveUsers() {
    JsonObject json = new JsonObject();
    for (Map.Entry<String, JsonObject> entry : users.entrySet()) {
      json.add(entry.getKey(), entry.getValue());
    }
    try (FileWriter writer = new FileWriter(usersFilePath)) {
      gson.toJson(json, writer);
      logger.info("Saved " + users.size() + " users to " + usersFilePath);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error saving users to file", e);
    }
  }

  private void broadcastMessage(Message message, String senderUsername) {
    String jsonMessage = gson.toJson(message);
    logger.info("Broadcasting message from " + senderUsername + ": " +
                message.getText());
    clients.forEach((username, handler) -> {
      if (!username.equals(senderUsername)) {
        handler.sendRawMessage(jsonMessage);
      }
    });
  }

  // ClientHandler 类保持不变，以下为简洁起见省略，实际代码中保留完整实现
  private class ClientHandler {
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private volatile boolean connected = true;

    public ClientHandler(Socket socket) { this.clientSocket = socket; }

    public void run() {
      try {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));
        String inputLine;
        while (connected && (inputLine = in.readLine()) != null) {
          try {
            logger.info("Received message: " + inputLine);
            Message message = gson.fromJson(inputLine, Message.class);
            handleMessage(message);
          } catch (Exception e) {
            logger.log(Level.WARNING, "Error processing message", e);
            sendMessage(Message.createResponse("error", "error",
                                               "Invalid message format"));
          }
        }
      } catch (IOException e) {
        if (connected) {
          logger.log(Level.WARNING, "Client connection error", e);
        }
      } finally {
        close();
      }
    }

    private void handleMessage(Message message) {
      if (message == null || message.getType() == null) {
        sendMessage(
            Message.createResponse("error", "error", "Invalid message"));
        return;
      }
      switch (message.getType()) {
      case "register":
        handleRegister(message);
        break;
      case "login":
        handleLogin(message);
        break;
      case "message":
        if (username != null) {
          broadcastMessage(message, username);
        } else {
          sendMessage(
              Message.createResponse("error", "error", "Not logged in"));
        }
        break;
      case "logout":
        close();
        break;
      default:
        sendMessage(
            Message.createResponse("error", "error", "Unknown message type"));
      }
    }

    private void handleRegister(Message message) {
      String usernameRegister = message.getMessage();
      String password = message.getText();
      String nickname = message.getNickname();
      if (usernameRegister == null || password == null || nickname == null) {
        sendMessage(Message.createResponse("register", "error",
                                           "Missing registration information"));
        return;
      }
      synchronized (users) {
        if (users.containsKey(usernameRegister)) {
          sendMessage(Message.createResponse("register", "error",
                                             "Username already exists"));
        } else {
          JsonObject user = new JsonObject();
          user.addProperty("password", password);
          user.addProperty("nickname", nickname);
          users.put(usernameRegister, user);
          saveUsers();
          sendMessage(Message.createResponse("register", "success",
                                             "Registration successful"));
          logger.info("New user registered: " + usernameRegister);
        }
      }
    }

    private void handleLogin(Message message) {
      String usernameLogin = message.getNickname();
      String password = message.getText();
      if (usernameLogin == null || password == null) {
        sendMessage(Message.createResponse("login", "error",
                                           "Missing login information"));
        return;
      }
      if (username != null) {
        sendMessage(
            Message.createResponse("login", "error", "Already logged in"));
        return;
      }
      synchronized (users) {
        if (clients.containsKey(usernameLogin)) {
          sendMessage(Message.createResponse("login", "error",
                                             "User already logged in"));
        } else if (users.containsKey(usernameLogin)) {
          JsonObject user = users.get(usernameLogin);
          if (password.equals(user.get("password").getAsString())) {
            this.username = usernameLogin;
            clients.put(usernameLogin, this);
            Message loginSuccess =
                Message.createResponse("login", "success", "Login successful");
            loginSuccess.setNickname(user.get("nickname").getAsString());
            sendMessage(loginSuccess);
            logger.info("User logged in: " + usernameLogin);
            Message userJoinedMessage = new Message(
                "system", null,
                user.get("nickname").getAsString() + " joined the chat");
            broadcastMessage(userJoinedMessage, usernameLogin);
          } else {
            sendMessage(
                Message.createResponse("login", "error", "Invalid password"));
          }
        } else {
          sendMessage(
              Message.createResponse("login", "error", "User not found"));
        }
      }
    }

    public void sendMessage(Message message) {
      sendRawMessage(gson.toJson(message));
    }

    public void sendRawMessage(String rawMessage) {
      if (connected && out != null) {
        out.println(rawMessage);
        if (out.checkError()) {
          logger.warning("Error sending message to client");
          close();
        }
      }
    }

    public void close() {
      if (!connected)
        return;
      connected = false;
      if (username != null) {
        clients.remove(username);
        JsonObject user = users.get(username);
        if (user != null) {
          String nickname = user.get("nickname").getAsString();
          Message userLeftMessage =
              new Message("system", null, nickname + " left the chat");
          broadcastMessage(userLeftMessage, username);
        }
        logger.info("User disconnected: " + username);
        username = null;
      }
      try {
        if (out != null)
          out.close();
        if (in != null)
          in.close();
        clientSocket.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, "Error closing client resources", e);
      }
    }
  }

  public static void main(String[] args) {
    try {
      ChatServer server = new ChatServer();
      server.startServer();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Server startup error", e);
    }
  }

  private String getUsersFilePath() { // 动态获取jar包所在位置
    try {
      String jarPath = new File(ChatServer.class.getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI())
                           .getParentFile()
                           .getAbsolutePath();
      return Paths.get(jarPath, "users.json").toString();
    } catch (Exception e) {
      logger.log(Level.SEVERE,
                 "Failed to determine jar directory, fallback to current dir",
                 e);
      return "users.json";
    }
  }
}