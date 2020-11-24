package server;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * @FileName: ChatServer.java
 * @Description: ChatServer.java类说明
 * @Author: camille
 * @Date: 2020/11/24 21:24
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    // 对应BIO的实现 serverSocket
    private ServerSocketChannel server; // 处理服务器的IO通道
    // 伪异步的线程池 -> selector
    private Selector selector;
    // 通道的写入读出需要buffer.
    // 从通道里读取消息，写入buffer
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    // 转发给其他用户，写入其他客户端的socketChannel
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    // 解决乱码问题
    private Charset charset = StandardCharsets.UTF_8;
    private int port; // 用户自定义的端口

    // 根据用户自定义的端口创建服务器
    public ChatServer(int port) {
        this.port = port;
    }

    // 复用上面的方法
    public ChatServer() {
        this(DEFAULT_PORT);
    }

    private void start() {
        try {
            server = ServerSocketChannel.open(); // 创建一个serverSocket通道，默认为阻塞式调用模式
            server.configureBlocking(false); // 配置取消阻塞状态
            // 通道关联的serverSocket绑定到监听端口
            server.socket().bind(new InetSocketAddress(port));

            selector = Selector.open(); // 返回一个selector对象
            // selector开始监听serverSocketChannel的accept状态
            // 一旦服务器通道接受了新的客户端的连接请求时，selector会返回相关信息（放在selectionKey）
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口：" + port);

            // select函数是阻塞的，如果没有selector监听的事件发生，会一直阻塞。一旦发生了事件，就会返回
            while (true) {
                selector.select(); // 返回被触发事件的个数，会不停的被调用，一直监听
                // 有事件发生了，监听到的被触发的事件：信息被包装到selectionKey中
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    // 对每一个被触发的事件，处理它
                    handles(key);
                }
                // 把处理过的keys手动清空。
                // 如果不清空，会将新发生的事件加到selectionKeys集合里面，又重复执行一遍
                selectionKeys.clear();
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 实际中，会自动把在selector上注册的事件和连接的通道关闭
            close(selector);
        }
    }

    private void handles(SelectionKey key) throws IOException {
        // key中包含了我们所要的所有信息
        // key有四种，accept，read...
        // accept事件，和客户端建立了连接 serverSocket上面的
        if (key.isAcceptable()) {
            // 是accept事件
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false); // 默认阻塞调用模式，改为非阻塞模式调用
            client.register(selector, SelectionKey.OP_READ); // 注册一个read事件到selector
            System.out.println("客户端" + client.socket().getPort() + "已连接");
        } else if (key.isReadable()) {
            // read事件，客户端发送了消息给服务端
            // 从客户端读消息，转发到其他客户端中
            SocketChannel client = (SocketChannel) key.channel();
            String fwdMsg = receive(client); // 从通道中获取消息
            // 转发消息，如果消息为空，就不转发
            if (fwdMsg.isEmpty()) {
                // 客户端出现异常，不再监听客户端上的事件
                key.cancel(); // 取消key对应的通道和事件
                // 如果当前有selector的被阻塞的方法，那么我们更新了监听的事件状态，
                // 让selector将当前被阻塞的状态重新返回，在多线程中有更大的意义
                selector.wakeup();
            } else {
                // 通过selector获得连接的客户端
                forwardMessage(client, fwdMsg);
                // 检查用户是否退出
                if (readyToQuit(fwdMsg)) {
                    key.cancel();
                    selector.wakeup();
                    System.out.println("客户端" + client.socket().getPort() + "断开连接");
                }
            }
        }
    }

    private void forwardMessage(SocketChannel client, String fwdMsg) throws IOException {
        // 所有注册的key
        for (SelectionKey key : selector.keys()) {
           // 排除serverSocketChannel的事件
           if (key.channel() instanceof ServerSocketChannel) {
               continue;
           }
           // 不是发送消息的客户端
           if (key.isValid() && !client.equals(key.channel())) {
               // 将数据写入到wBuffer
               wBuffer.clear();
               // 确保不会乱码
               wBuffer.put(charset.encode(getClientName(client) + ":" + fwdMsg));
               wBuffer.flip(); // 写模式转为读模式
               while (wBuffer.hasRemaining()) {
                   SocketChannel channel = (SocketChannel) key.channel();
                   channel.write(wBuffer);
               }
           }
        }


    }

    private String getClientName(SocketChannel client) {
        return String.valueOf(client.socket().getPort());
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear(); // 清空之前输入信息
        while (client.read(rBuffer) > 0); // 直到读不出任何字节才退出
        rBuffer.flip(); // 转为写模式
        return  String.valueOf(charset.decode(rBuffer));
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }



}
