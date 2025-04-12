package com.example.chatterdemo.client;

import com.example.chatterdemo.model.Message;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class RegisterWindow extends VBox {
  private final ChatClient client;
  private final TextField usernameField;
  private final PasswordField passwordField;
  private final PasswordField confirmPasswordField;
  private final TextField nicknameField;
  private final Button registerButton;
  private final Button backButton;
  private final Label statusLabel;
  private final Consumer<Void> registerSuccessListener;
  private final Runnable backToLoginListener;

  public RegisterWindow(ChatClient client,
                        Consumer<Void> registerSuccessListener,
                        Runnable backToLoginListener) {
    this.client = client;
    this.registerSuccessListener = registerSuccessListener;
    this.backToLoginListener = backToLoginListener;

    setPadding(new Insets(20));
    setSpacing(15);
    setAlignment(Pos.CENTER);
    setPrefSize(500, 600);
    getStyleClass().add("register-pane");

    Label titleLabel = new Label("Create New Account");
    titleLabel.getStyleClass().add("title-label");

    GridPane formGrid = new GridPane();
    formGrid.setHgap(10);
    formGrid.setVgap(15);
    formGrid.setAlignment(Pos.CENTER);

    Label usernameLabel = new Label("Username:");
    usernameField = new TextField();
    usernameField.setPromptText("Choose a username");
    usernameField.setPrefWidth(300);

    Label passwordLabel = new Label("Password:");
    passwordField = new PasswordField();
    passwordField.setPromptText("Create a password");
    passwordField.setPrefWidth(300);

    Label confirmPasswordLabel = new Label("Confirm:");
    confirmPasswordField = new PasswordField();
    confirmPasswordField.setPromptText("Confirm your password");
    confirmPasswordField.setPrefWidth(300);

    Label nicknameLabel = new Label("Nickname:");
    nicknameField = new TextField();
    nicknameField.setPromptText("Choose a display name");
    nicknameField.setPrefWidth(300);

    formGrid.add(usernameLabel, 0, 0);
    formGrid.add(usernameField, 1, 0);
    formGrid.add(passwordLabel, 0, 1);
    formGrid.add(passwordField, 1, 1);
    formGrid.add(confirmPasswordLabel, 0, 2);
    formGrid.add(confirmPasswordField, 1, 2);
    formGrid.add(nicknameLabel, 0, 3);
    formGrid.add(nicknameField, 1, 3);

    HBox buttonBox = new HBox();
    buttonBox.setSpacing(15);
    buttonBox.setAlignment(Pos.CENTER);

    registerButton = new Button("Register");
    registerButton.setPrefWidth(150);
    registerButton.setOnAction(event -> attemptRegister());

    backButton = new Button("Back to Login");
    backButton.setPrefWidth(150);
    backButton.getStyleClass().add("secondary-button");
    backButton.setOnAction(event -> backToLoginListener.run());

    buttonBox.getChildren().addAll(registerButton, backButton);

    statusLabel = new Label("");
    statusLabel.setTextFill(Color.RED);
    statusLabel.setWrapText(true);
    statusLabel.setAlignment(Pos.CENTER);

    getChildren().addAll(titleLabel, new Separator(), formGrid, buttonBox,
                         statusLabel);

    client.setMessageListener(this::handleMessage);
  }

  private void attemptRegister() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();
    String confirmPassword = confirmPasswordField.getText();
    String nickname = nicknameField.getText().trim();

    if (username.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
      statusLabel.setText("All fields are required");
      return;
    }

    if (!password.equals(confirmPassword)) {
      statusLabel.setText("Passwords do not match");
      passwordField.clear();
      confirmPasswordField.clear();
      return;
    }

    if (username.length() < 3 || username.length() > 20) {
      statusLabel.setText("Username must be between 3-20 characters");
      return;
    }

    if (password.length() < 4) {
      statusLabel.setText("Password must be at least 4 characters");
      passwordField.clear();
      confirmPasswordField.clear();
      return;
    }

    statusLabel.setText("Registering...");
    registerButton.setDisable(true);

    Message registerRequest =
        Message.createRegisterRequest(username, password, nickname);
    client.sendMessage(registerRequest);
  }

  private void handleMessage(Message message) {
    if (message != null && "register".equals(message.getType())) {
      Platform.runLater(() -> {
        if ("success".equals(message.getStatus())) {
          statusLabel.setText(
              "Registration successful! Redirecting to login...");
          statusLabel.setTextFill(Color.GREEN);

          usernameField.clear();
          passwordField.clear();
          confirmPasswordField.clear();
          nicknameField.clear();

          new Thread(() -> {
            try {
              Thread.sleep(1500);
              Platform.runLater(() -> registerSuccessListener.accept(null));
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }).start();
        } else {
          statusLabel.setText(message.getMessage());
          statusLabel.setTextFill(Color.RED);
          registerButton.setDisable(false);
        }
      });
    }
  }
}