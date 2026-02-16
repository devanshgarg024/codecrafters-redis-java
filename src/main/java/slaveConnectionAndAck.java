import java.util.Vector;

public class slaveConnectionAndAck {
    public static String replconf(Vector<String> words){
    String response="";
        if(words.get(3).toLowerCase().equals("listening-port")){
            return "+OK\r\n";
        }
        else if(words.get(3).toLowerCase().equals("capa")){
            return "+OK\r\n";
        }
        else if(words.get(3).toLowerCase().equals("getack")){
            response=("*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$1\r\n0\r\n");
        }
        return response;
    }
}
