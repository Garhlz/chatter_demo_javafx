package com.example.chatterdemo.client;

import com.example.chatterdemo.model.Message;
import com.example.chatterdemo.utils.JsonUtils;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class LoginWindow extends VBox {
  private final ChatClient client;
  private final TextField usernameField;
  private final PasswordField passwordField;
  private final Button loginButton;
  private final Button registerButton;
  private final Label statusLabel;
  private final Consumer<String> loginSuccessListener;

  public LoginWindow(ChatClient client, Consumer<String> loginSuccessListener,
                     Runnable showRegisterListener) {
    this.client = client;
    this.loginSuccessListener = loginSuccessListener;

    setPadding(new Insets(20));
    setSpacing(15);
    setAlignment(Pos.CENTER);
    setPrefSize(500, 500);
    getStyleClass().add("login-pane");

    Label titleLabel = new Label("Welcome to Chat App");
    titleLabel.getStyleClass().add("title-label");

    GridPane formGrid = new GridPane();
    formGrid.setHgap(10);
    formGrid.setVgap(15);
    formGrid.setAlignment(Pos.CENTER);

    Label usernameLabel = new Label("Username:");
    usernameField = new TextField();
    usernameField.setPromptText("Enter your username");
    usernameField.setPrefWidth(300);

    Label passwordLabel = new Label("Password:");
    passwordField = new PasswordField();
    passwordField.setPromptText("Enter your password");
    passwordField.setPrefWidth(300);

    formGrid.add(usernameLabel, 0, 0);
    formGrid.add(usernameField, 1, 0);
    formGrid.add(passwordLabel, 0, 1);
    formGrid.add(passwordField, 1, 1);

    passwordField.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER)
        attemptLogin();
    });

    HBox buttonBox = new HBox();
    buttonBox.setSpacing(15);
    buttonBox.setAlignment(Pos.CENTER);

    loginButton = new Button("Login");
    loginButton.setPrefWidth(150);
    loginButton.setOnAction(event -> attemptLogin());

    registerButton = new Button("Register");
    registerButton.setPrefWidth(150);
    registerButton.getStyleClass().add("secondary-button");
    registerButton.setOnAction(event -> showRegisterListener.run());

    buttonBox.getChildren().addAll(loginButton, registerButton);

    statusLabel = new Label("");
    statusLabel.setTextFill(Color.RED);
    statusLabel.setWrapText(true);
    statusLabel.setAlignment(Pos.CENTER);

    getChildren().addAll(titleLabel, new Separator(), formGrid, buttonBox,
                         statusLabel);

    client.setMessageListener(this::handleMessage);
  }

  private void attemptLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();

    if (username.isEmpty() || password.isEmpty()) {
      statusLabel.setText("Username and password cannot be empty");
      return;
    }

    statusLabel.setText("Logging in...");
    loginButton.setDisable(true);

    Message loginRequest = Message.createLoginRequest(username, password);
    client.sendMessage(loginRequest);
  }

  private void handleMessage(Message message) {
    System.out.println("LoginWindow received: " + JsonUtils.toJson(message));
    if (message != null && "login".equals(message.getType())) {
      Platform.runLater(() -> {
        if ("success".equals(message.getStatus())) {
          String nickname = message.getNickname();
          if (nickname != null) {
            statusLabel.setText("Login successful!");
            statusLabel.setTextFill(Color.GREEN);
            System.out.println("Calling loginSuccessListener with nickname: " +
                               nickname);
            new Thread(() -> {
              try {
                Thread.sleep(1000); // 短暂延迟显示成功信息
                Platform.runLater(() -> loginSuccessListener.accept(nickname));
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }).start();
          } else {
            statusLabel.setText("Nickname missing in response");
            statusLabel.setTextFill(Color.RED);
            loginButton.setDisable(false);
          }
        } else {
          statusLabel.setText(message.getMessage());
          statusLabel.setTextFill(Color.RED);
          loginButton.setDisable(false);
          passwordField.clear();
        }
      });
    }
  }
}