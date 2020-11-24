package client;

import client.UserInputHandler;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * @FileName: ChatClient.java
 * @Description: ChatClient.java类说明
 * @Author: camille
 * @Date: 2020/11/24 22:27
 */
public class ChatClient {

    private final static String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final static int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";
    private final int BUFFER = 1024;


    private String host;
    private int port;
    private SocketChannel client;

    // 伪异步的线程池 -> selector
    private Selector selector;
    // 通道的写入读出需要buffer
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    // 解决乱码问题
    private Charset charset = StandardCharsets.UTF_8;


    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public boolean readyToQuit(String msg) {
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

    private void start() {
        try {
            client = SocketChannel.open();
            client.configureBlocking(false); // 改为非阻塞模式

            selector = Selector.open();
            // 服务器端接受了客户端连接情况，注册一个connected事件
            client.register(selector, SelectionKey.OP_CONNECT);
            client.connect(new InetSocketAddress(host, port)); // 正式向服务器端发送连接请求
            while (true) {
                // 监听事件
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    handles(key); // 处理所有key
                }
                // 清空selectionKey
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClosedSelectorException e) {
            // 其实不算异常，只是因为客户输入了退出指令，算用户正常退出
        } finally {
            // 调用已经关闭的对象，不会报错。只是不会执行任何操作
            close(selector);
        }
    }

    private void handles(SelectionKey key) throws IOException {
        // 连接就绪事件，connect事件
        if (key.isConnectable()) {
            SocketChannel client = (SocketChannel) key.channel();
            // 连接就绪，isConnectionPending=true
            // 正在建立连接，isConnectionPending=false需要等待
            if (client.isConnectionPending()) {
                client.finishConnect(); // 正式地建立好连接
                // 处理用户的输入信息，需要一个额外的线程处理
                new Thread((new UserInputHandler(this))).start();
            }
            // 注册监听read事件
            client.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            // read事件，服务器转发消息到客户端，触发socketChannel的read事件
            SocketChannel client = (SocketChannel) key.channel();
            String msg = receive(client); // 获取数据
            if (msg.isEmpty()) {
                // 连接出现异常，服务器那边出现异常
                close(selector); // 客户端退出
            } else {
                System.out.println(msg);
            }
        }
    }

    private String receive(SocketChannel client) throws IOException {
        // channel -> buffer
        rBuffer.clear();
        while (client.read(rBuffer) > 0);
        rBuffer.flip(); // 转为读模式
        return String.valueOf(charset.decode(rBuffer));
    }

    public void send(String msg) throws IOException {
        // 从客户端发送到服务器端
        if (msg.isEmpty()) {
            return;
        }
        // buffer -> channel
        wBuffer.clear();
        wBuffer.put(charset.encode(msg));
        wBuffer.flip();
        while (wBuffer.hasRemaining()) {
            client.write(wBuffer);
        }

        // 检查用户是否准备退出
        if (readyToQuit(msg)) {
            close(selector);
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }


}
