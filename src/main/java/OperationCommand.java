import java.util.Vector;

public class OperationCommand {

    public static String incr(Vector<String> words) {
        String response="";
        String key=words.get(3);
        int value=Integer.parseInt(Main.db.get(key))+1;
        Main.db.put(key,String.valueOf(value));
        response+=(":"+String.valueOf(value)+"\r\n");
        return response;
    }
}
