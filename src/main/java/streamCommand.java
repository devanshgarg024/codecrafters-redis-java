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
        long milliseconds=0;
        int sequence=0;
        if(id.equals("*")){

        }
        else{
        String[] parts = id.split("-");
        milliseconds = Long.parseLong(parts[0]);
//            System.out.println(milliseconds);
//            System.out.println(parts[1]);
            if(parts[1].equals("*")){
                if(milliseconds==0){
                    if (temp.size() != 0) {
                        LinkedHashMap<String, String> lastEntry = temp.getLast();
                        String[] parts2 = lastEntry.get("id").split("-");
                        long lastmilliseconds=Long.parseLong(parts2[0]);
                        int lastSeq=Integer.parseInt(parts2[1]);
                        if(milliseconds<lastmilliseconds){
                            response+="-ERR The ID specified in XADD is equal or smaller than the target stream top item\r\n";
                            return response;
                        }
                        sequence=lastSeq+1;
                    }
                    else{
                        sequence=1;
                    }
                }
                else{
                    if (temp.size() != 0) {
                        LinkedHashMap<String, String> lastEntry = temp.getLast();
                        String[] parts2 = lastEntry.get("id").split("-");
                        long lastmilliseconds=Long.parseLong(parts2[0]);
                        int lastSeq=Integer.parseInt(parts2[1]);
                        if(milliseconds<lastmilliseconds){
                            response+="-ERR The ID specified in XADD is equal or smaller than the target stream top item\r\n";
                            return response;
                        }
                        else if(milliseconds==lastmilliseconds){
                            sequence=lastSeq+1;
                        }
                        else{
                            sequence=0;
                        }
                    }
                    else{
                        sequence=0;
                    }
                }
            }
        else{
        sequence = Integer.parseInt(parts[1]);
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
        }
        }
        LinkedHashMap<String, String> Entry=new LinkedHashMap<>();
            String EntryId=String.valueOf(milliseconds)+'-'+String.valueOf(sequence);
            int numofstream = (words.size() - 4) / 4;
            Entry.put("id",EntryId);
            for (int i = 0; i < numofstream; i++) {
                Entry.put(words.get(7 + 4 * i), words.get(9 + 4 * i));
            }
            temp.add(Entry);
            response += ("$" + String.valueOf(EntryId.length()) + "\r\n" + EntryId + "\r\n");
            return response;

        }
    }
