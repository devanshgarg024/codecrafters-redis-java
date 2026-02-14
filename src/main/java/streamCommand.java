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
        long enmil = -1;
        int stseq = 0;
        int enseq = -1;
        if(!st.equals("-")){
//            System.out.println("fd");
            if (st.contains("-")) {
                String[] parts = st.split("-");
                stmil = Long.parseLong(parts[0]);
                stseq = Integer.parseInt(parts[1]);
            } else {
                stmil = Long.parseLong(st);
                stseq = 0;
            }
        }
        if(!en.equals("+")){
            if (en.contains("-")) {
                String[] parts = en.split("-");
                enmil = Long.parseLong(parts[0]);
                enseq = Integer.parseInt(parts[1]);
            } else {
                enmil = Long.parseLong(en);
                enseq = -1;
            }
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
            boolean beforeEnd = (lastmilliseconds < enmil || enmil==-1) ||
                    (lastmilliseconds == enmil && (enseq == -1 || lastSeq <= enseq));

            if (afterStart && beforeEnd) {
                matches.add(it);
            }
        }
        StringBuilder response = new StringBuilder();
        response.append("*").append(matches.size()).append("\r\n");
        synchronized (temp){
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
        temp.notifyAll();

        }

        return response.toString();
    }
    public static String xread(Vector<String> words) {
        String response="";

        if(words.get(3).toUpperCase().equals("BLOCK")){
        response+=("*1"+"\r\n");
        response+=("*2\r\n");
            int timeinmill=Integer.parseInt(words.get(5));
            String key=words.get(9);
            String st=words.get(11);
            if(!st.equals("-")&&st.contains("-")){
                String[] parts = st.split("-");

                String seq= String.valueOf(Integer.parseInt(parts[1])+1);
                st=parts[0]+"-"+seq;
            }
            ArrayList<LinkedHashMap<String, String>> temp = Main.streamdb.get(key);
            response+=("$"+String.valueOf(key.length())+"\r\n"+key+"\r\n");
            Vector<String> v=new Vector<>();
            v.add("XRANGE");
            v.add("XRANGE");
            v.add("XRANGE");
            v.add(key);
            v.add("XRANGE");
            v.add(st);
            v.add("XRANGE");
            v.add("+");
            String firstres=xrange(v);

            if(!firstres.equals("*0\r\n")){
//                System.out.println(key);

                response+=firstres;
                return response;
            }
                synchronized (temp){
                    try{
                        temp.wait(timeinmill);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                        return ""; // Or handle appropriately
                    }

                }

            v=new Vector<>();
            v.add("XRANGE");
            v.add("XRANGE");
            v.add("XRANGE");
            v.add(key);
            v.add("XRANGE");
            v.add(st);
            v.add("XRANGE");
            v.add("+");
            firstres=xrange(v);
            if(!firstres.equals("*0\r\n")){
                response+=firstres;
            }
            else{
                response="*-1\r\n";
            }
        }
        else{
        int n=(words.size()-4)/4;
        response+=("*"+String.valueOf(n)+"\r\n");
        for(int i=0;i<n;i++){
            response+=("*2\r\n");
            String key=words.get(5+2*i);
            String st=words.get(5+2*n+2*i);
            if(!st.equals("-")&&st.contains("-")){
            String[] parts = st.split("-");

            String seq= String.valueOf(Integer.parseInt(parts[1])+1);
            st=parts[0]+"-"+seq;
            }

            response+=("$"+String.valueOf(key.length())+"\r\n"+key+"\r\n");
            Vector<String> temp=new Vector<>();
            temp.add("XRANGE");
            temp.add("XRANGE");
            temp.add("XRANGE");
            temp.add(key);
            temp.add("XRANGE");
            temp.add(st);
            temp.add("XRANGE");
            temp.add("+");
            response+=xrange(temp);
        }
        }

//            System.out.println(response);
        return response;

    }
    }
