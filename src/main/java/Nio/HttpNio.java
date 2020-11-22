package Nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class HttpNio {
    // NIO ,N :NoneBlocking
    // NIO ,N : new IO (channel byteBuffer selector:多路复用器)
    public static void main(String[] args) {
        LinkedList<SocketChannel> socketChannelLinkedList=new LinkedList<>();
        try {
            ServerSocketChannel serverSocketChannel=ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(9001));
            serverSocketChannel.configureBlocking(false);// 阻塞False
            while (true){
                Thread.sleep(2000);
                SocketChannel socketChannelClient =serverSocketChannel.accept();
                if(socketChannelClient == null){
                    // 没有连接
                    System.out.println("nothing connection");
                }else{
                    socketChannelClient.configureBlocking(false);// 客户端不阻塞
                    int port = socketChannelClient.socket().getPort();
                    System.out.println("client port is = " + port);
                    socketChannelLinkedList.add(socketChannelClient);
                }
                ByteBuffer buffer = ByteBuffer.allocateDirect(4096);//堆內？堆外？
                for (SocketChannel socketChannel :socketChannelLinkedList){
                    int num=socketChannel.read(buffer);
                    if(num>0){
                        buffer.flip();
                        byte read[] = new byte[buffer.limit()];
                        System.out.println(buffer.limit());
                        buffer.get(read);
                        System.out.println(socketChannel.socket().getPort()+":Console :"+read.toString());
                        buffer.clear();

                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
