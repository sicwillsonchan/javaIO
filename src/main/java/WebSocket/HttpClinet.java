package WebSocket;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class HttpClinet {
    public static void main(String[] args)  {
        try {
            Socket socketClient = new Socket("localhost",8803);
            PrintWriter pw = null;
            BufferedReader br =  new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            String s="";
            while (true){
                s  = br.readLine();
                if(s != null){
                    break;
                }
            }
            System.out.println(s);
            br.close();
            pw.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

}
