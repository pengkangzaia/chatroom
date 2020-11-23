package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * @FileName: ChatHandler.java
 * @Description: ChatHandler.java类说明
 * @Author: camille
 * @Date: 2020/11/17 21:04
 */
public class ChatHandler implements Runnable {

    private ChatServer chatServer;
    private Socket socket;

    public ChatHandler(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }


    @Override
    public void run() {
        try {
            // 连接起客户端和服务器端
            chatServer.addClient(socket);
            // 读取客户端输入信息
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = null;
            while ((msg = reader.readLine()) != null) {
                System.out.println("服务器接受客户端" + socket.getPort() + "消息：" + msg);
                // 转发给其他的客户端
                chatServer.forwordMessage(socket, msg + "\n");
                if (chatServer.readerToQuit(msg)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                chatServer.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}
