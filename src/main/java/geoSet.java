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
    private Map<String, List> members;
    public int add(double longitude,double latitude, String member) {
        if (members.containsKey(member)) {
//            double oldScore = members.get(member);
//            if (oldScore == score) {
//                return 0;
//            }
//            sortedSet.remove(new geoMemberScore(member, oldScore));
//            members.put(member, score);
//            sortedSet.add(new geoMemberScore(member, score));
//            return 0;
        }
        List<Double> st=new ArrayList<>();
        st.add(longitude);
        st.add(latitude);
        members.put(member, st);
        return 1;
    }
//    public int rank(String member){
//        if(!members.containsKey(member)){
//            return -1;
//        }
//        double score=members.get(member);
//        return sortedSet.headSet(new geoMemberScore(member,score)).size();
//    }
//    public ArrayList<String> range(int start,int end){
//        ArrayList<String> emp=new ArrayList<>();
//        if(start<-1*members.size()){
//            start=0;
//        }
//        if(start> members.size()-1){
//            start= members.size()-1;
//        }
//        if(end<-1*members.size()){
//            end=0;
//        }
//        if(end> members.size()-1){
//            end= members.size()-1;
//        }
//        start+= members.size();
//        start%= members.size();
//        end+= members.size();
//        end%= members.size();
//        if(members.size()-1<end)end= members.size()-1;
//        if(start<0)start=0;
//        if(start>end){
//            return emp;
//        }
//        List<geoMemberScore> list = new ArrayList<>(sortedSet);
//        List<geoMemberScore> indexedRange = list.subList(start, end+1);
//        ArrayList<String> ind=new ArrayList<>();
//        for(geoMemberScore it:indexedRange){
//            ind.add(it.member);
//        }
//        return ind;
//
//    }
//    public String score(String member){
//        if(!members.containsKey(member)){
//            return "";
//        }
//        String val=String.valueOf(members.get(member));
//        return val;
//    }
//    public int card(){
//        return sortedSet.size();
//    }
//    public int rem(String member) {
//        if (members.containsKey(member)) {
//            double oldScore = members.get(member);
//            sortedSet.remove(new geoMemberScore(member, oldScore));
//            members.remove(member);
//            return 1;
//        }
//        return 0;
//    }
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
        cmdBuilder.append(":");
        cmdBuilder.append(isNew);
        cmdBuilder.append("\r\n");
        return cmdBuilder.toString();
    }
}
