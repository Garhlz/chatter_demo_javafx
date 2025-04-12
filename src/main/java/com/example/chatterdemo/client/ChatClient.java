package com.example.chatterdemo.client;

import com.example.chatterdemo.model.Message;
import com.example.chatterdemo.utils.JsonUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;
import javafx.application.Platform;

public class ChatClient {
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private Consumer<Message> messageListener;

  public ChatClient() {}

  public void connectToServer(String host, int port) {
    try {
      socket = new Socket(host, port);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      Thread thread = new Thread(() -> {
        try {
          String inputLine;
          while ((inputLine = in.readLine()) != null) {
            System.out.println("Received message: " +
                               inputLine); // 添加接收日志
            Message message = JsonUtils.fromJsonObject(
                JsonUtils.toJsonObject(inputLine), Message.class);
            Platform.runLater(() -> {
              if (messageListener != null) {
                messageListener.accept(message);
              } else {
                System.out.println("No message listener set!");
              }
            });
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      thread.setDaemon(true); // 设置为守护线程，避免程序无法退出
      thread.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sendMessage(Message message) {
    String jsonMessage = JsonUtils.toJson(message);
    System.out.println("Sending message: " + jsonMessage);
    out.println(jsonMessage);
  }

  public void setMessageListener(Consumer<Message> messageListener) {
    this.messageListener = messageListener;
    System.out.println("Message listener set: " +
                       messageListener); // 添加设置日志
  }

  public void disconnect() {
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}