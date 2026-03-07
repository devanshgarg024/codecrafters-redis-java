import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class subAndPub {
    public static String subscribe(ArrayList<String> words, Socket clientSocket){
    String response;
    int numOfSubs=1;
    if(Main.subs.containsKey(clientSocket)){
        Main.subs.get(clientSocket).add(words.get(3));
        numOfSubs=Main.subs.get(clientSocket).size();
    }
    else{
        Set<String> temp=new HashSet<>();
        temp.add(words.get(3));
        Main.subs.put(clientSocket,temp);

    }
    StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("*3\r\n$9\r\nsubscribe\r\n");
        cmdBuilder.append("$");
        cmdBuilder.append(words.get(3).length());
        cmdBuilder.append("\r\n");
        cmdBuilder.append(words.get(3));
        cmdBuilder.append("\r\n");
        cmdBuilder.append(":");
        cmdBuilder.append(numOfSubs);
        cmdBuilder.append("\r\n");
        response=cmdBuilder.toString();
        return  response;
    }
}
