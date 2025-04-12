package com.example.chatterdemo;

import com.example.chatterdemo.client.ChatClient;
import com.example.chatterdemo.client.ChatWindow;
import com.example.chatterdemo.client.LoginWindow;
import com.example.chatterdemo.client.RegisterWindow;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
  private ChatClient client;
  private Stage primaryStage; // 主窗口

  @Override
  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    client = new ChatClient();
    client.connectToServer("localhost", 12345);

    showLoginWindow(); // 初始显示登录窗口

    primaryStage.setTitle("Chat Client");
    primaryStage.setOnCloseRequest(event -> {
      client.disconnect(); // 关闭时断开连接
    });
    primaryStage.show();
  }

  private void showLoginWindow() {
    LoginWindow loginWindow =
        new LoginWindow(client, this::showChatWindow, this::showRegisterWindow);
    VBox root = new VBox();
    root.setPadding(new Insets(10));
    root.getChildren().add(loginWindow);

    Scene scene = new Scene(root, 500, 500);
    scene.getStylesheets().add(
        getClass().getResource("/styles.css").toExternalForm());
    primaryStage.setScene(scene);
  }

  private void showChatWindow(String nickname) {
    System.out.println("Showing chat window for: " + nickname);
    Scene chatScene =
        ChatWindow.createScene(client, nickname, this::showLoginWindow);
    primaryStage.setScene(chatScene);
    primaryStage.setTitle("Chat Window - " + nickname);
  }

  private void showRegisterWindow() {
    RegisterWindow registerWindow =
        new RegisterWindow(client,
                           v
                           -> showLoginWindow(), // 注册成功后返回登录
                           this::showLoginWindow // 返回按钮切换到登录
        );
    VBox root = new VBox();
    root.setPadding(new Insets(10));
    root.getChildren().add(registerWindow);

    Scene scene = new Scene(root, 500, 600);
    scene.getStylesheets().add(
        getClass().getResource("/styles.css").toExternalForm());
    primaryStage.setScene(scene);
    primaryStage.setTitle("Register Window");
  }

  public static void main(String[] args) { launch(args); }
}