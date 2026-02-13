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
            milliseconds=System.currentTimeMillis();
            if (!temp.isEmpty()) {
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
        else{
        String[] parts = id.split("-");
        milliseconds = Long.parseLong(parts[0]);
            if(parts[1].equals("*")){
                if(milliseconds==0){
                    if (!temp.isEmpty()) {
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
                    if (!temp.isEmpty()) {
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
        if (!temp.isEmpty()) {
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
    public static String xrange(Vector<String> words) {
        String key = words.get(3);
        String st = words.get(5);
        String en = words.get(7);

        long stmil = 0;
        long enmil = 0;
        int stseq = 0;
        int enseq = 0;
        if(st=="-"){
            stmil=0;
            stseq=0;
        }
        else{
            if (st.contains("-")) {
                String[] parts = st.split("-");
                stmil = Long.parseLong(parts[0]);
                stseq = Integer.parseInt(parts[1]);
            } else {
                stmil = Long.parseLong(st);
                stseq = 0;
            }
        }
        if (en.contains("-")) {
            String[] parts = en.split("-");
            enmil = Long.parseLong(parts[0]);
            enseq = Integer.parseInt(parts[1]);
        } else {
            enmil = Long.parseLong(en);
            enseq = -1;
        }
        ArrayList<LinkedHashMap<String, String>> temp = Main.streamdb.get(key);
        if (temp == null) {
            return "*0\r\n";
        }
        ArrayList<LinkedHashMap<String, String>> matches = new ArrayList<>();

        for (var it : temp) {
            String[] parts2 = it.get("id").split("-");
            long lastmilliseconds = Long.parseLong(parts2[0]);
            int lastSeq = Integer.parseInt(parts2[1]);
            boolean afterStart = (lastmilliseconds > stmil) ||
                    (lastmilliseconds == stmil && lastSeq >= stseq);
            boolean beforeEnd = (lastmilliseconds < enmil) ||
                    (lastmilliseconds == enmil && (enseq == -1 || lastSeq <= enseq));

            if (afterStart && beforeEnd) {
                matches.add(it);
            }
        }
        StringBuilder response = new StringBuilder();
        response.append("*").append(matches.size()).append("\r\n");

        for (var match : matches) {
            response.append("*2\r\n");
            String id = match.get("id");
            response.append("$").append(id.length()).append("\r\n").append(id).append("\r\n");
            int fieldCount = (match.size() - 1) * 2;
            response.append("*").append(fieldCount).append("\r\n");
            for (Map.Entry<String, String> entry : match.entrySet()) {
                if (entry.getKey().equals("id")) {
                    continue;
                }
                String k = entry.getKey();
                String v = entry.getValue();
                response.append("$").append(k.length()).append("\r\n").append(k).append("\r\n");
                response.append("$").append(v.length()).append("\r\n").append(v).append("\r\n");
            }
        }

        return response.toString();
    }
    }
