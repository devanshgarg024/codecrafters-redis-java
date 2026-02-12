import java.util.Vector;

public class typeCommand {
    public static String type(Vector<String> words){
        String response="";
        String key=words.get(3);
        if(Main.db.containsKey(key))response+="+string\r\n";
        else if (Main.streamdb.containsKey(key))response+="+stream\r\n";
        else response+="+none\r\n";
        return response;
    }
}

