package com.example.chatterdemo.client;

import com.example.chatterdemo.model.Message;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ChatWindow extends BorderPane {
  private final ChatClient client;
  private final String nickname;
  private final TextArea chatDisplay;
  private final TextField messageInput;
  private final Label statusLabel;
  private final DateTimeFormatter timeFormatter;
  private final Runnable logoutListener; // 返回登录窗口的回调

  public ChatWindow(ChatClient client, String nickname,
                    Runnable logoutListener) {
    this.client = client;
    this.nickname = nickname;
    this.logoutListener = logoutListener;
    this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    getStyleClass().add("chat-root");

    // ===== 顶部区域 =====
    HBox header = new HBox();
    header.getStyleClass().add("chat-header");
    header.setPadding(new Insets(10));
    header.setSpacing(15);
    header.setAlignment(Pos.CENTER_LEFT);

    Label nicknameLabel = new Label("Chatting as: " + nickname);
    nicknameLabel.getStyleClass().add("chat-header-label");

    Button logoutButton = new Button("Logout ❀");
    logoutButton.getStyleClass().add("button");
    logoutButton.setOnAction(e -> handleLogout());

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    header.getChildren().addAll(nicknameLabel, spacer, logoutButton);
    setTop(header);

    // ===== 中部聊天显示 =====
    chatDisplay = new TextArea();
    chatDisplay.getStyleClass().add("chat-display");
    chatDisplay.setEditable(false);
    chatDisplay.setWrapText(true);
    chatDisplay.setPrefHeight(400);
    appendSystemMessage("Welcome to the my chat room!");
    appendSystemMessage("You are logged in as " + nickname + " ~");
    setCenter(chatDisplay);

    // ===== 底部输入区域 =====
    VBox bottomContainer = new VBox();
    bottomContainer.setSpacing(10);
    bottomContainer.setPadding(new Insets(10));

    HBox inputArea = new HBox(10);
    inputArea.setAlignment(Pos.CENTER);

    messageInput = new TextField();
    messageInput.getStyleClass().add("chat-input");
    messageInput.setPromptText("Type your cute message here ~");
    HBox.setHgrow(messageInput, Priority.ALWAYS);
    messageInput.setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.ENTER)
        sendMessage();
    });

    Button sendButton = new Button("Send ♥");
    sendButton.getStyleClass().add("button");
    sendButton.setOnAction(e -> sendMessage());

    Button clearButton = new Button("Clear ✿");
    clearButton.getStyleClass().add("secondary-button");
    clearButton.setOnAction(e -> chatDisplay.clear());

    Button exitButton = new Button("Exit ❀");
    exitButton.getStyleClass().add("button");
    exitButton.setOnAction(e -> handleLogout());

    inputArea.getChildren().addAll(messageInput, sendButton, clearButton,
                                   exitButton);

    statusLabel = new Label("Connected to server ~");
    statusLabel.getStyleClass().add("status-bar");

    bottomContainer.getChildren().addAll(inputArea, statusLabel);
    setBottom(bottomContainer); // 修复变量名

    // ===== 消息监听器 =====
    client.setMessageListener(this::handleMessage);
  }

  private void sendMessage() {
    String text = messageInput.getText().trim();
    if (!text.isEmpty()) {
      Message message = new Message("message", nickname, text);
      client.sendMessage(message);
      appendUserMessage(nickname, text);
      messageInput.clear();
    }
  }

  private void handleMessage(Message message) {
    if (message == null)
      return;
    switch (message.getType()) {
    case "message":
      if (!nickname.equals(message.getNickname())) {
        appendUserMessage(message.getNickname(), message.getText());
      }
      break;
    case "system":
      appendSystemMessage(message.getText());
      break;
    case "error":
      appendErrorMessage(message.getMessage());
      break;
    }
  }

  private void appendUserMessage(String nickname, String text) {
    Platform.runLater(() -> {
      String timestamp = LocalTime.now().format(timeFormatter);
      chatDisplay.appendText("[" + timestamp + "] " + nickname + ": " + text +
                             "\n");
    });
  }

  private void appendSystemMessage(String text) {
    Platform.runLater(() -> {
      String timestamp = LocalTime.now().format(timeFormatter);
      chatDisplay.appendText("[" + timestamp + "] >>> " + text +
                             " <<< ฅ(>ω<*ฅ)\n");
    });
  }

  private void appendErrorMessage(String text) {
    Platform.runLater(() -> {
      String timestamp = LocalTime.now().format(timeFormatter);
      chatDisplay.appendText("[" + timestamp + "] ERROR: " + text + " (T_T)\n");
    });
  }

  private void handleLogout() {
    Message logoutMessage = new Message("logout", nickname, null);
    client.sendMessage(logoutMessage);
    client.disconnect();
    logoutListener.run(); // 返回登录窗口
  }

  public static Scene createScene(ChatClient client, String nickname,
                                  Runnable logoutListener) {
    ChatWindow root = new ChatWindow(client, nickname, logoutListener);
    Scene scene = new Scene(root, 600, 500);
    scene.getStylesheets().add("/chatstyles.css"); // 修正资源路径
    return scene;
  }
}