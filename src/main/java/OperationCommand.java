import java.util.Vector;

public class OperationCommand {

    public static String incr(Vector<String> words) {
        String response="";
        String key=words.get(3);
        if(!Main.db.containsKey(key)){
            Main.db.put(key,"0");
        }
        try{
            Integer.parseInt(Main.db.get(key));
        }
        catch(NumberFormatException e){
            response+=("-ERR value is not an integer or out of range\r\n");
            return response;
        }
        int value=Integer.parseInt(Main.db.get(key))+1;
        Main.db.put(key,String.valueOf(value));
        response+=(":"+String.valueOf(value)+"\r\n");
        return response;
    }
}
