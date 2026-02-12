import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public class streamCommand {
    public static String xadd(Vector<String> words){
        String response="";
        String key=words.get(3);
        if(!Main.streamdb.containsKey(key)){
            LinkedHashMap<String,String> temp=new LinkedHashMap<>();
            Main.streamdb.put(key,temp);
        }
        LinkedHashMap<String,String> temp=Main.streamdb.get(key);
        int numofstream=(words.size()-4)/4;
        temp.put("id",words.get(5));
        for(int i=0;i<numofstream;i++){
            temp.put(words.get(7+4*i),words.get(9+4*i));
        }
        response+=("$"+String.valueOf(words.get(5).length())+"\r\n"+words.get(5)+"\r\n");
        return response;

    }
}
