import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class pushRangeCommand {
    public static String rpush(Vector<String> words){
        String response="";
        String key=words.get(3);
        int numOfElement=(words.size()-4)/2;
        if(Main.elementList.containsKey(key)){
            for(int i=0;i<numOfElement;i++){
                Main.elementList.get(key).add(words.get(5+i*2));
            }
        }
        else{
            List<String> l=new ArrayList<>();
            for(int i=0;i<numOfElement;i++){
                l.add(words.get(5+i*2));
            }
            Main.elementList.put(key,l);
        }
        int sizOfList=Main.elementList.get(key).size();
        response+=(":"+String.valueOf(sizOfList)+"\r\n");
        List<String> l=Main.elementList.get(key);
        synchronized (l){
        l.notifyAll();
        }
        return response;
    }
    public static String lpush(Vector<String> words){
        String response="";
        String key2=words.get(3);
        int numOfElement2=(words.size()-4)/2;
        if(Main.elementList.containsKey(key2)){
            for(int i=0;i<numOfElement2;i++){
                Main.elementList.get(key2).add(0,words.get(5+i*2));
            }
        }
        else{
            List<String> l=new ArrayList<>();
            for(int i=0;i<numOfElement2;i++){
                l.add(0,words.get(5+i*2));
            }
            Main.elementList.put(key2,l);
        }
        int sizOfList2=Main.elementList.get(key2).size();
        response+=(":"+String.valueOf(sizOfList2)+"\r\n");
        List<String> l=Main.elementList.get(key2);
        synchronized (l){
            l.notifyAll();
        }
        return response;
    }

    public static String lrange(Vector<String> words){
        String response="";

        if(Main.elementList.containsKey(words.get(3))){
            List<String> l=Main.elementList.get(words.get(3));
            int endInd=Integer.parseInt(words.get(7));
            int stInd=Integer.parseInt(words.get(5));
            int st=(max(-1*l.size(),stInd)+l.size())%l.size();

            int en=(min(l.size()-1,endInd)+l.size())%l.size();
            response+=("*"+String.valueOf(max(0,en-st+1)) +"\r\n");
            for(int i=st;i<=en;i++){
                response+=("$"+String.valueOf(l.get(i).length())+"\r\n");
                response+=(l.get(i)+"\r\n");
            }
        }
        else{
            response+=("*0\r\n");
        }
        return response;
    }
    public static String llen(Vector<String> words){
        String response="";

        if(Main.elementList.containsKey(words.get(3))){
            String key3=words.get(3);
            int sizOfList3=Main.elementList.get(key3).size();
            response+=(":"+String.valueOf(sizOfList3)+"\r\n");
        }
        else{
            response+=(":0\r\n");
        }
        return response;
    }
}
