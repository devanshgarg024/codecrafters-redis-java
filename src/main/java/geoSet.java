import java.util.*;

public class geoSet {
    public static String geoadd(ArrayList<String> words){
        String key=words.get(3);
        String member=words.get(9);
        double longitude=Double.parseDouble(words.get(5));
        double latitude=Double.parseDouble(words.get(7));
        int isNew;
        if(longitude>180||longitude<-180||latitude>85.05112878||latitude<-85.05112878){
            isNew=-1;
        }
        else{
            double score=Encode.encode(latitude,longitude);
            if(Main.zset.containsKey(key)){
                isNew=Main.zset.get(key).add(score,member);
            }
            else{
                RedisSortedSet st=new  RedisSortedSet();
                isNew=st.add(score,member);
                Main.zset.put(key,st);
            }
        }
        StringBuilder cmdBuilder = new StringBuilder();
        if(isNew==-1){
            cmdBuilder.append("-ERR invalid longitude,latitude pair ");
            cmdBuilder.append(longitude).append(",").append(latitude).append("\r\n");
            return cmdBuilder.toString();
        }
        cmdBuilder.append(":");
        cmdBuilder.append(isNew);
        cmdBuilder.append("\r\n");
        return cmdBuilder.toString();
    }
    public static String geopos(ArrayList<String> words){
        String key=words.get(3);
        List<String>  members=new ArrayList<>();
        for(int i=5;i<words.size();i+=2){
            members.add(words.get(i));
        }
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("*").append(members.size()).append("\r\n");
        if(Main.zset.containsKey(key)){
                RedisSortedSet st=Main.zset.get(key);
            for(String member:members){
                String score=st.score(member);
                if(score.isEmpty()){
                    cmdBuilder.append("*-1\r\n");
                }
                else{
                    long geoCode = (long) Double.parseDouble(score);
                    Decode.Coordinates coordinates = Decode.decode(geoCode);
                    String longitude=String.valueOf(coordinates.longitude);
                    String latitude=String.valueOf(coordinates.latitude);
                    cmdBuilder.append("*2\r\n");
                    cmdBuilder.append("$").append(longitude.length()).append("\r\n");
                    cmdBuilder.append(longitude).append("\r\n");
                    cmdBuilder.append("$").append(latitude.length()).append("\r\n");
                    cmdBuilder.append(latitude).append("\r\n");
                }
            }
        }
        else{
            for(String member:members) {
                cmdBuilder.append("*-1\r\n");
            }
        }
        return cmdBuilder.toString();
    }
}
