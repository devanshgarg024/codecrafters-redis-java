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
    public static String publish(ArrayList<String> words, Socket clientSocket){
        String response;
        String key=words.get(3);
        int numOfsubs=0;
        if(Main.channels.containsKey(key)){
            numOfsubs=Main.channels.get(key).size();
        }
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append(":");
        cmdBuilder.append(numOfsubs);
        cmdBuilder.append("\r\n");
        response=cmdBuilder.toString();
        return  response;
    }
}
