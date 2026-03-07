import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class subAndPub {
    public static String subscribe(ArrayList<String> words, Socket clientSocket){
        String response;
        int numOfChannelSubscribed=1;
        String key=words.get(3);
        if(Main.subs.containsKey(clientSocket)){
            Main.subs.get(clientSocket).add(key);
            numOfChannelSubscribed=Main.subs.get(clientSocket).size();
        }
        else{
            Set<String> temp=new HashSet<>();
            temp.add(key);
            Main.subs.put(clientSocket,temp);
        }
        if(Main.channels.containsKey(key)){
            Main.channels.get(key).add(clientSocket);
        }
        else{
            Set<Socket> temp=new HashSet<>();
            temp.add(clientSocket);
            Main.channels.put(key,temp);
        }
    StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("*3\r\n$9\r\nsubscribe\r\n");
        cmdBuilder.append("$");
        cmdBuilder.append(key.length());
        cmdBuilder.append("\r\n");
        cmdBuilder.append(key);
        cmdBuilder.append("\r\n");
        cmdBuilder.append(":");
        cmdBuilder.append(numOfChannelSubscribed);
        cmdBuilder.append("\r\n");
        response=cmdBuilder.toString();
        return  response;
    }
    public static String publish(ArrayList<String> words) throws IOException {
        String response;
        String key=words.get(3);
        String msg=words.get(5);
        int numOfsubs=0;
        if(Main.channels.containsKey(key)){
            Set<Socket>temp=Main.channels.get(key);
            numOfsubs=temp.size();
            StringBuilder cmdBuilder=new StringBuilder();
            cmdBuilder.append("*3\r\n$7\r\nmessage\r\n$");
            cmdBuilder.append(key.length());
            cmdBuilder.append("\r\n");
            cmdBuilder.append(key);
            cmdBuilder.append("\r\n$");
            cmdBuilder.append(msg.length());
            cmdBuilder.append("\r\n");
            cmdBuilder.append(msg);
            cmdBuilder.append("\r\n");
            response=cmdBuilder.toString();

            for(Socket clientSocket:temp){
                OutputStream output = clientSocket.getOutputStream();
                output.write(response.getBytes());
            }

        }

        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append(":");
        cmdBuilder.append(numOfsubs);
        cmdBuilder.append("\r\n");
        response=cmdBuilder.toString();
        return  response;
    }
    public static String unsubscribe(ArrayList<String> words, Socket clientSocket) {
        String response;
        String key=words.get(3);
        int numOfChannelSubscribed=0;
        if(Main.subs.containsKey(clientSocket)){
            Main.subs.get(clientSocket).remove(key);
            numOfChannelSubscribed=Main.subs.get(clientSocket).size();
        }
        if(Main.channels.containsKey(key)){
            Main.channels.get(key).remove(clientSocket);
        }
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("*3\r\n$11\r\nunsubscribe\r\n");
        cmdBuilder.append("$");
        cmdBuilder.append(key.length());
        cmdBuilder.append("\r\n");
        cmdBuilder.append(key);
        cmdBuilder.append("\r\n");
        cmdBuilder.append(":");
        cmdBuilder.append(numOfChannelSubscribed);
        cmdBuilder.append("\r\n");
        response=cmdBuilder.toString();
        return  response;
    }
}
