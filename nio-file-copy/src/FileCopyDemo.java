import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @FileName: FileCopyDemo.java
 * @Description: 四种IO方式实现文件拷贝
 * @Author: camille
 * @Date: 2020/11/24 0:17
 */
public class FileCopyDemo {

    private static final int ROUNDS = 5;

    private static void benchmark(FileCopyRunner test, File source, File target) {
        long elapsed = 0L;
        for (int i = 0; i < ROUNDS; i++) {
            long startTime = System.currentTimeMillis();
            test.copyFile(source, target);
            elapsed += System.currentTimeMillis() - startTime;
            target.delete();
        }
        System.out.println(test + ":" + elapsed / ROUNDS);
    }


    // 只要是实现了closeable接口的方法都可以调用这个方法
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // 第一种，使用最原始的字节流
        FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                InputStream fin = null;
                OutputStream fout = null;
                try {
                    fin = new FileInputStream(source);
                    fout = new FileOutputStream(target);
                    int result;
                    while ((result = fin.read()) != 1) {
                        fout.write(result);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fout);
                    close(fin);
                }
            }

            @Override
            public String toString() {
                return "noBufferStreamCopy";
            }
        };


        // 第二种，使用buffer+流
        FileCopyRunner bufferSteamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                BufferedInputStream fin = null;
                BufferedOutputStream fout = null;
                try {
                    fin = new BufferedInputStream(new FileInputStream(source));
                    fout = new BufferedOutputStream(new FileOutputStream(target));
                    byte[] buffer = new byte[1024];
                    int result;
                    while ((result = fin.read(buffer)) != -1) {
                        // 指定写的范围
                        fout.write(buffer, 0, result);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fout);
                    close(fin);
                }
            }


            @Override
            public String toString() {
                return "bufferSteamCopy";
            }

        };

        // 第三种，使用channel，buffer作为中介
        FileCopyRunner nioBufferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;

                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    // 读通道中的信息，对应的是写buffer中的数据
                    while (fin.read(buffer) != -1) {
                        buffer.flip(); // buffer转为读模式
                        while (buffer.hasRemaining()) {
                            // 写入到通道中，对应的是读buffer中的数据
                            fout.write(buffer); // 确保读完所有buffer中的数据
                        }
                        buffer.clear(); // 调整buffer为写模式
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "nioBufferCopy";
            }


        };


        // 第四种，直接使用通道直连
        FileCopyRunner nioTransferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;

                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();
                    long transferred = 0L; // 已经拷贝了多少个字节
                    while (transferred != fin.size()) {
                        // 只有当转移的字节为输入的所有字节时，才退出循环
                        transferred += fin.transferTo(0, fin.size(), fout);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fout);
                    close(fin);
                }
            }


            @Override
            public String toString() {
                return "nioTransferCopy";
            }

        };


        File myFile = new File("C:\\Fun\\moive\\拯救大兵瑞.mkv");
        File myFileCopy = new File("C:\\Fun\\moive\\copy.mkv");

        benchmark(noBufferStreamCopy, myFile, myFileCopy);
        benchmark(bufferSteamCopy, myFile, myFileCopy);
        benchmark(nioBufferCopy, myFile, myFileCopy);
        benchmark(nioTransferCopy, myFile, myFileCopy);



    }


}
