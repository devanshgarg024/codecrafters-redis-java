import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.time.temporal.ChronoUnit;

import static java.lang.Math.min;

public class popCommand {

    public static String lpop(Vector<String> words){
        String response="";

        if(words.size()==4){
            if(Main.elementList.containsKey(words.get(3))){
                String key3=words.get(3);
                String removedElement=Main.elementList.get(key3).remove(0);
                response+=(("$"+removedElement.length()+"\r\n"+removedElement+"\r\n"));
            }
            else{
                response+=(("$-1\r\n"));
            }
        }
        else{

            if(Main.elementList.containsKey(words.get(3))){
                String key3=words.get(3);
                List<String> l=Main.elementList.get(key3);
                int num=min(l.size(),Integer.parseInt(words.get(5)));
                response+=(("*"+String.valueOf(num)+"\r\n"));

                for(int i=0;i<num;i++){
                    String removedElement=l.remove(0);
                    response+=(("$"+removedElement.length()+"\r\n"+removedElement+"\r\n"));
                }
            }
            else{
                response+=(("$-1\r\n"));
            }
        }
        return response;
    }
    public static String blpop(Vector<String> words, Socket clientSocket){
        LocalTime now = LocalTime.now();
        String response="";
        String key3=words.get(3);
        float timeToBlockinf = Float.parseFloat(words.get(5))*1000;
        int timeToBlock=(int)timeToBlockinf;
        if(!Main.elementList.containsKey(key3)){
            List<String> l=new ArrayList<>();
            Main.elementList.put(key3,l);
        }
        List<String> l=Main.elementList.get(key3);
        if(l.isEmpty()){
            if(!Main.PopExp.containsKey(key3)){
                List<Main.user> temp=new ArrayList<>();
                Main.PopExp.put(key3,temp);
            }
            LocalTime expTime=now.plusNanos(timeToBlock*1000000);
            System.out.println(timeToBlock);
            synchronized (l){
            if(timeToBlock==0){
                Main.user a=new Main.user(clientSocket,now,now,false);
                Main.PopExp.get(key3).add(a);
            }
            else{
                Main.user a=new Main.user(clientSocket,now,expTime,true);
                Main.PopExp.get(key3).add(a);
            }
                try {
                    if(timeToBlock==0){
                        Main.user u=null;
                        while(true){
                            l.wait();
                            u=check(key3,clientSocket);
                            if(u!=null)break;
                        }
                        Main.PopExp.remove(u);
                        response+=("*2"+"\r\n");
                        response+=("$"+key3.length()+"\r\n"+key3+"\r\n");
                        String removedElement=l.remove(0);
                        response+=("$"+removedElement.length()+"\r\n"+removedElement+"\r\n");
                    }
                    else {
                        Main.user u=null;
                        while(true){
                        now=LocalTime.now();
                            long duration =ChronoUnit.MILLIS.between(now,expTime);
                            if(duration<=0)break;
                            l.wait(duration);
//                            System.out.println(now);
//                            System.out.println(expTime);
                            u=check(key3,clientSocket);
                            if(u!=null)break;
                        }
                        if(u==null){
                            response+=("*-1\r\n");
                        }
                        else{
                        Main.PopExp.remove(u);
                        response+=("*2"+"\r\n");
                        response+=("$"+key3.length()+"\r\n"+key3+"\r\n");
                        String removedElement=l.remove(0);
                        response+=("$"+removedElement.length()+"\r\n"+removedElement+"\r\n");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    return ""; // Or handle appropriately
                }

            }
        }
        else{
            response+=(("*2"+"\r\n"));
            response+=(("$"+key3.length()+"\r\n"+key3+"\r\n"));
            String removedElement=l.remove(0);
            response+=(("$"+removedElement.length()+"\r\n"+removedElement+"\r\n"));
        }

        return response;
    }
    public static Main.user check(String key,Socket clientSocket){
        List<Main.user> l =Main.PopExp.get(key);
        LocalTime now = LocalTime.now();
        boolean socketFound=false;
        Main.user temp=null;
        LocalTime mini=now;
        List<Main.user> removeList=new ArrayList<>();
        for(int i=0;i<l.size();i++){
            Main.user a=l.get(i);
            if(!a.willExp()||a.expTime().isAfter(now)){
                if(!socketFound){
                    socketFound=true;
                    mini=a.startTime();
                    temp=a;
                }
                else{
                    socketFound=true;
                    if(a.startTime().isBefore(mini)){
                        temp=a;
                        mini=a.startTime();

                    }
                }
            }
            else{
                removeList.add(a);
            }
        }
        for(int i=0;i<removeList.size();i++){
            l.remove(removeList.get(i));
        }
        if(socketFound && (clientSocket==temp.clientSocket())&& !Main.elementList.get(key).isEmpty())return temp;
        return null;
    }
}
