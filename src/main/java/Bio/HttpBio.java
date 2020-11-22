package Bio;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpBio {
    public static void main(String[] args) throws IOException {
        ServerSocket server=new ServerSocket(9000);
        System.out.println("step1:Create socket port:9000");
        Socket client = server.accept();
        System.out.println("step2:clint connection,port:\t"+client.getPort());
        InputStream inputStream= client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        System.out.println(reader.readLine());
        while (true){

        }

    }
}
