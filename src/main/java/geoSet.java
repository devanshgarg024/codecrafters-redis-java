import com.sun.jdi.request.StepRequest;

import java.util.*;

public class geoSet {
    private static final double EARTH_RADIUS_IN_METERS = 6372797.560856;
    private static double geohashGetLatDistance(double lat1d, double lat2d) {
        return EARTH_RADIUS_IN_METERS * Math.abs(Math.toRadians(lat2d) - Math.toRadians(lat1d));
    }
    public static double geohashGetDistance(double lon1d, double lat1d, double lon2d, double lat2d) {
        double lat1r, lon1r, lat2r, lon2r, u, v, a;

        lon1r = Math.toRadians(lon1d);
        lon2r = Math.toRadians(lon2d);

        v = Math.sin((lon2r - lon1r) / 2.0);
        if (v == 0.0) {
            return geohashGetLatDistance(lat1d, lat2d);
        }
        lat1r = Math.toRadians(lat1d);
        lat2r = Math.toRadians(lat2d);
        u = Math.sin((lat2r - lat1r) / 2.0);
        a = u * u + Math.cos(lat1r) * Math.cos(lat2r) * v * v;
        return 2.0 * EARTH_RADIUS_IN_METERS * Math.asin(Math.sqrt(a));
    }
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
    public static String geodist(ArrayList<String> words){
        String key=words.get(3);
        String member1=words.get(5);
        String member2=words.get(7);
        RedisSortedSet st=Main.zset.get(key);
        String score1=st.score(member1);
        String score2=st.score(member2);
        long geoCode1 = (long) Double.parseDouble(score1);
        Decode.Coordinates coordinates1 = Decode.decode(geoCode1);
        long geoCode2 = (long) Double.parseDouble(score2);
        Decode.Coordinates coordinates2 = Decode.decode(geoCode2);
        Double dist=geohashGetDistance(coordinates1.longitude,coordinates1.latitude,coordinates2.longitude,coordinates2.latitude);
        String cmdBuilder = "$" + String.valueOf(dist).length() + "\r\n" +
                dist + "\r\n";
        return cmdBuilder;
    }
    public static String geosearch(ArrayList<String> words){
        String key=words.get(3);
        Double longitude=Double.parseDouble(words.get(7));
        Double latitude=Double.parseDouble(words.get(9));
        Double rad=Double.parseDouble(words.get(13));
        List<String>res =new ArrayList<>();
        RedisSortedSet st=Main.zset.get(key);
            for (Map.Entry<String, Double> entry : st.members.entrySet()) {
                String member = entry.getKey();
                String score = String.valueOf(entry.getValue());
                long geoCode =(long) Double.parseDouble(score);
                Decode.Coordinates coordinates = Decode.decode(geoCode);

                Double dist=geohashGetDistance(coordinates.longitude,coordinates.latitude,longitude,latitude);
                if(dist<=rad){
                    res.add(member);
                }
            }
        StringBuilder cmdBuilder = new StringBuilder();
            cmdBuilder.append("*").append(res.size()).append("\r\n");
            for(String member: res){
                cmdBuilder.append("$").append(member.length()).append("\r\n");
                cmdBuilder.append(member).append("\r\n");
            }
        return cmdBuilder.toString();
    }
}
