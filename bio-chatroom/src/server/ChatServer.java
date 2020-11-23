package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @FileName: ChatServer.java
 * @Description: ChatServer.java类说明
 * @Author: camille
 * @Date: 2020/11/17 20:05
 */
public class ChatServer {

    private final int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";
    private ServerSocket serverSocket;
    // key为端口号，value为服务器对应的输出流
    private Map<Integer, Writer> connectedClients;

    public ChatServer() {
        this.connectedClients = new HashMap<>();
    }

    /**
     * 增加连接的客户端信息
     * @param socket
     */
    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            connectedClients.put(port, writer);
        }
    }

    /**
     * 移除客户端
     * @param socket
     * @throws IOException
     */
    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                Writer writer = connectedClients.get(port);
                writer.close();
                connectedClients.remove(port);
            }
        }
    }

    /**
     * 转发消息给其他客户端
     * @param socket
     * @param msg
     * @throws IOException
     */
    public synchronized void forwordMessage(Socket socket, String msg) throws IOException {
        if (socket != null) {
            for (Integer port : connectedClients.keySet()) {
                if (!port.equals(socket.getPort())) {
                    // 转发消息给其他客户端
                    Writer writer = connectedClients.get(port);
                    writer.write(msg);
                    writer.flush(); // 刷新writer缓冲区，确保消息被发出
                    System.out.println("消息" + msg + "被转发到端口" + port);
                }
            }
        }
    }

    public void start() {
        try {
            // 按照默认端口启动服务器
            serverSocket = new ServerSocket(DEFAULT_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("连接上客户端，端口：" + socket.getPort());
                // 调用charHandler处理客户端事件
                new Thread(new ChatHandler(this, socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }

    }

    private synchronized void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean readerToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }



}
