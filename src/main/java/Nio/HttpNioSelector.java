package Nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;

public class HttpNioSelector {
    private ServerSocketChannel serverSocketChannel=null;
    private  Selector selector=null;
    final int port = 9002;
    public  String getByteBufferString(ByteBuffer buffer){
        Charset charset = null;
        CharsetDecoder decoder = null;
        CharBuffer charBuffer = null;
        try {
            charset = Charset.forName("UTF-8");
            decoder = charset.newDecoder();
            charBuffer = decoder.decode(buffer.asReadOnlyBuffer());
            return charBuffer.toString();

        } catch (CharacterCodingException e) {
            e.printStackTrace();
            return "";
        }
    }
    public void initServer(){
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            // 约等于在 epoll 模型下 进行了 open ,create.
            selector = Selector.open();
            // open之后,service 处于 listen 状态 ,假设creat 返回了 fd4
            // 如果select poll 模式下,在jvm开辟了一个数组,把fd4放进去。
            // 在epoll 模式下,调用内核的 poll_ctl(fd3,add,epollin


            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void acceptHandler(SelectionKey key){
        try{
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

            SocketChannel client = serverSocketChannel.accept();// 调用accept,接收客户端，返回一个FD8

            client.configureBlocking(false);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024*10);

            // select ,poll模型,:JVM 开辟需要的内存,把FD8丢进去
            // 在epoll 模式下,调用pool_ctl(fd3,fd7,epoolin
            client.register(selector,SelectionKey.OP_READ,byteBuffer);

            System.out.println("----------------");
            System.out.println("new Client is connected "+client.getRemoteAddress());
            System.out.println("----------------");

        } catch (IOException e) {
            e.printStackTrace();
        }finally {

        }
    }
    public void readHandler(SelectionKey key){
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer= (ByteBuffer) key.attachment();
        byteBuffer.clear();
        int read =0 ;
        try {
            while (true){
                read = socketChannel.read(byteBuffer);
                if(read>0){
                    byteBuffer.flip();
                    while (byteBuffer.hasRemaining()){
                      //  String tmp = new StringBuffer(getByteBufferString(byteBuffer)).reverse().toString();
                        socketChannel.write(byteBuffer);
                    }
                    byteBuffer.clear();
                }else if(read == 0){
                    break;
                }else{
                    socketChannel.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void star(){
        initServer();
        System.out.println("Server Star..Port:"+ serverSocketChannel.socket().getLocalPort());
        try {
            while (true){
                // select()
                // select ,poll 模式下等同于，调用了内核的select(fd4) poll(fd4)
                // epoll 模式下,等同于调用了epool_wait()
                while (selector.select(500)>0){
                    //从selector中 拿到有效的连接
                    //返回所有有状态的fds
                    Set<SelectionKey> selectionKeySet =selector.selectedKeys();// 从selector中 拿到有效的连接
                    Iterator<SelectionKey> selectionKeyIterator = selectionKeySet.iterator();
                    // 开始遍历这些IO
                    while (selectionKeyIterator.hasNext()){
                        SelectionKey selectionKey=selectionKeyIterator.next();
                        selectionKeyIterator.remove();
                        if(selectionKey.isAcceptable()){
                            //select, poll 内核没有空间,使用的JVM内存
                            //epoll 内存下有空间就可以调用epoll下的epoll_cli 写入内核空间
                            acceptHandler(selectionKey);
                        }else if(selectionKey.isReadable()){
                            readHandler(selectionKey);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {

        HttpNioSelector selector = new HttpNioSelector();
        selector.star();

    }

}
