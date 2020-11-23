package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @FileName: UserInputHandler.java
 * @Description: 采用额外的线程来处理来自用户控制台的请求，实现runnable接口
 * @Author: camille
 * @Date: 2020/11/23 21:38
 */
public class UserInputHandler implements Runnable {

    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        // 等待用户输入信息
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String input = null;
            try {
                // 用户输入
                input = consoleReader.readLine();
                // 像服务器发送控制台输入
                chatClient.send(input);
                // 检查是否需要退出
                if (chatClient.readyToQuit(input)) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
