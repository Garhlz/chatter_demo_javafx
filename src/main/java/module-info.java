module com.example.chatterdemo {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires javafx.base;
  requires com.google.gson;
  requires java.logging;

  opens com.example.chatterdemo to javafx.fxml;
  opens com.example.chatterdemo.client to javafx.fxml;
  opens com.example.chatterdemo.model to
      com.google.gson; // 为 Gson 开放 model 包

  exports com.example.chatterdemo;
  exports com.example.chatterdemo.client;
  exports com.example.chatterdemo.server;
  exports com.example.chatterdemo.model;
  exports com.example.chatterdemo.utils;
}