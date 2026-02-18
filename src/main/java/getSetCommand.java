import java.time.LocalTime;
import java.util.ArrayList;

public class getSetCommand {
    public static String set(ArrayList<String> words) {
        LocalTime now = LocalTime.now();
        String response="";
        if(words.size()==6){
            Main.db.put(words.get(3),words.get(5));
            response+="+OK\r\n";
            Main.exp.remove(words.get(3));
        }
        else if(words.size()==10){
            LocalTime expTime=now;
            if(words.get(7).equalsIgnoreCase("ex")){
                expTime = now.plusSeconds(Integer.parseInt(words.get(9)));
            }
            else if(words.get(7).equalsIgnoreCase("px")){
                long nanoSecToAdd=Integer.parseInt(words.get(9));
                nanoSecToAdd=nanoSecToAdd*1000000;
                expTime = now.plusNanos(nanoSecToAdd);
            }
            Main.db.put(words.get(3),words.get(5));
            Main.exp.put(words.get(3),expTime);
            response+="+OK\r\n";
        }
        return response;
    }

    public static String get(ArrayList<String> words) {
        LocalTime now = LocalTime.now();
        String response="";
        if(Main.db.containsKey(words.get(3))&&(!Main.exp.containsKey(words.get(3))||Main.exp.get(words.get(3)).isAfter(now))){
            String val=Main.db.get(words.get(3));
            response+=("$"+val.length()+"\r\n"+val+"\r\n");
        }
        else{
            response+="$-1\r\n";
        }
        return response;
    }
}