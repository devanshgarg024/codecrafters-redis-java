import java.sql.Array;
import java.util.*;

public class streamCommand {
    public static String xadd(Vector<String> words) {
        String response = "";
        String key = words.get(3);
        if (!Main.streamdb.containsKey(key)) {
            ArrayList<LinkedHashMap<String, String>> temp = new ArrayList<>();
            Main.streamdb.put(key, temp);
        }
        ArrayList<LinkedHashMap<String, String>> temp  = Main.streamdb.get(key);
        String id=words.get(5);
        String[] parts = id.split("-");
        long milliseconds = Long.parseLong(parts[0]);
        int sequence = Integer.parseInt(parts[1]);
        if(milliseconds==0&& sequence==0){
            response+="-ERR The ID specified in XADD must be greater than 0-0\r\n";
            return response;
        }
        if (temp.size() != 0) {
        LinkedHashMap<String, String> lastEntry = temp.getLast();
        String[] parts2 = lastEntry.get("id").split("-");
        long lastmilliseconds=Long.parseLong(parts2[0]);
        int lastSeq=Integer.parseInt(parts2[1]);
            if(milliseconds<lastmilliseconds||(milliseconds==lastmilliseconds&&sequence<=lastSeq)){
                response+="-ERR The ID specified in XADD is equal or smaller than the target stream top item\r\n";
                return response;
            }
        }
        LinkedHashMap<String, String> Entry=new LinkedHashMap<>();

            int numofstream = (words.size() - 4) / 4;
            Entry.put("id",words.get(5));
            for (int i = 0; i < numofstream; i++) {
                Entry.put(words.get(7 + 4 * i), words.get(9 + 4 * i));
            }
            temp.add(Entry);
            response += ("$" + String.valueOf(words.get(5).length()) + "\r\n" + words.get(5) + "\r\n");
            return response;

        }
    }
