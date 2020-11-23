package client;

import java.io.*;
import java.net.Socket;
import java.util.PrimitiveIterator;

/**
 * @FileName: ChatClient.java
 * @Description: 主线程，负责和服务器建立连接，处理来自服务器的请求
 * @Author: camille
 * @Date: 2020/11/23 21:34
 */
public class ChatClient {

    private final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    // 发送消息给服务器
    public void send(String msg) throws IOException {
        if (!socket.isOutputShutdown()) {
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    // 从服务器接收消息
    public String reveive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()) {
            msg = reader.readLine();
        }
        return msg;
    }

    // 检查用户是否要退出
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void close() {
        // 从最后开启的一个资源关闭，会自动关闭之前所有的资源
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        // 初始化客户端各个属性
        try {
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // 处理来自控制台的输入
            new Thread(new UserInputHandler(this)).start();

            // 处理来自服务器的输入，转发来自其他用户的消息
            String msg = null;
            while ((msg = reader.readLine()) != null) {
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 释放资源
            close();
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }


}
