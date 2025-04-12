package com.example.chatterdemo.model;

public class Message {
  private String type;     // 消息类型: login, register, message, logout
  private String nickname; // 用户昵称
  private String text;     // 消息文本
  private String status;   // 状态: success, error
  private String message;  // 附加消息内容

  public Message() {}

  // 聊天消息构造器
  public Message(String type, String nickname, String text) {
    this.type = type;
    this.nickname = nickname;
    this.text = text;
  }

  // 登录/注册响应构造器（移除重复的构造函数，改为静态工厂方法）
  public static Message createResponse(String type, String status,
                                       String message) {
    Message msg = new Message();
    msg.type = type;
    msg.status = status;
    msg.message = message;
    return msg;
  }

  // 注册请求构造器
  public static Message createRegisterRequest(String username, String password,
                                              String nickname) {
    Message msg = new Message();
    msg.type = "register";
    msg.nickname = nickname;
    msg.text = password;
    msg.message = username;
    return msg;
  }

  // 登录请求构造器
  public static Message createLoginRequest(String username, String password) {
    Message msg = new Message();
    msg.type = "login";
    msg.nickname = username;
    msg.text = password;
    return msg;
  }

  // Getters and Setters
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public String getText() { return text; }
  public void setText(String text) { this.text = text; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
}