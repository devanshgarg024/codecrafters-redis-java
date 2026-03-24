import java.util.*;

class geoMemberScore  {
    public String member;
    public double longitude;
    public double latitude;

    public geoMemberScore(String member, double longitude,double latitude) {
        this.member = member;
        this.longitude = longitude;
        this.latitude=latitude;
    }

//    @Override
//    public int compareTo(geoMemberScore other) {
//        int scoreComparison = Double.compare(this.score, other.score);
//        if (scoreComparison != 0) {
//            return scoreComparison;
//        }
//        return this.member.compareTo(other.member);
//    }
}
class geoSortedSet {
    private Map<String, List<Double>> members=new HashMap<>() ;
    public int add(double longitude,double latitude, String member) {
        if(longitude>180||longitude<-180||latitude>85.05112878||latitude<-85.05112878){
            return -1;
        }
        if (members.containsKey(member)) {
            List<Double> st=new ArrayList<>();
            st.add(longitude);
            st.add(latitude);
            members.put(member, st);
            return 0;
        }
        List<Double> st=new ArrayList<>();
        st.add(longitude);
        st.add(latitude);
        members.put(member, st);
        return 1;
    }
}
public class geoSet {
    public static String geoadd(ArrayList<String> words){
        String key=words.get(3);
        String member=words.get(9);
        double longitude=Double.parseDouble(words.get(5));
        double latitude=Double.parseDouble(words.get(7));
        int isNew;
        if(Main.geoset.containsKey(key)){
            isNew=Main.geoset.get(key).add(longitude,latitude,member);
        }
        else{
            geoSortedSet st=new  geoSortedSet();
            isNew=st.add(longitude,latitude,member);
            Main.geoset.put(key,st);
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
}
