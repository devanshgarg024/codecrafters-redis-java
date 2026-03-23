import java.util.*;

class MemberScore implements Comparable<MemberScore> {
    public String member;
    public double score;

    public MemberScore(String member, double score) {
        this.member = member;
        this.score = score;
    }

    @Override
    public int compareTo(MemberScore other) {
        int scoreComparison = Double.compare(this.score, other.score);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        return this.member.compareTo(other.member);
    }
}
class RedisSortedSet {
    private Map<String, Double> members;
    private TreeSet<MemberScore> sortedSet;
    public RedisSortedSet() {
        this.members = new HashMap<>();
        this.sortedSet = new TreeSet<>();
    }
    public int add(double score, String member) {
        if (members.containsKey(member)) {
            double oldScore = members.get(member);
            if (oldScore == score) {
                return 0;
            }
            sortedSet.remove(new MemberScore(member, oldScore));
            members.put(member, score);
            sortedSet.add(new MemberScore(member, score));
            return 0;
        }
        members.put(member, score);
        sortedSet.add(new MemberScore(member, score));

        return 1;
    }
    public int rank(String member){
        if(!members.containsKey(member)){
            return -1;
        }
        double score=members.get(member);
        return sortedSet.headSet(new MemberScore(member,score)).size();
    }
    public ArrayList<String> range(int start,int end){
        ArrayList<String> emp=new ArrayList<>();
        if(members.size()-1<end)end= members.size()-1;
        if(start<0)start=0;
        if(start>end){
            return emp;
        }
        List<MemberScore> list = new ArrayList<>(sortedSet);
        List<MemberScore> indexedRange = list.subList(start, end+1);
        ArrayList<String> ind=new ArrayList<>();
        for(MemberScore it:indexedRange){
            ind.add(it.member);
        }
        return ind;

    }
}
public class sortedSet {
    public static String zadd(ArrayList<String> words){
        String key=words.get(3);
        String member=words.get(7);
        double score=Double.parseDouble(words.get(5));
        int isNew;
        if(Main.zset.containsKey(key)){
            isNew=Main.zset.get(key).add(score,member);
        }
        else{
            RedisSortedSet st=new  RedisSortedSet();
            isNew=st.add(score,member);
            Main.zset.put(key,st);
        }
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append(":");
        cmdBuilder.append(isNew);
        cmdBuilder.append("\r\n");
        return cmdBuilder.toString();
    }
    public static String zrank(ArrayList<String> words){
        String key=words.get(3);
        String member=words.get(5);
        StringBuilder cmdBuilder = new StringBuilder();
        if(Main.zset.containsKey(key)){
            RedisSortedSet st=Main.zset.get(key);
            int rank=st.rank(member);
            if(rank==-1){
                cmdBuilder.append("$-1\r\n");
                return cmdBuilder.toString();
            }
            cmdBuilder.append(":");
            cmdBuilder.append(rank);
            cmdBuilder.append("\r\n");
        }
        else{
            cmdBuilder.append("$-1\r\n");
        }
        return cmdBuilder.toString();
    }
    public static String zrange(ArrayList<String> words){
        String key=words.get(3);
        int  start=Integer.parseInt(words.get(5));
        int  end=Integer.parseInt(words.get(7));
        StringBuilder cmdBuilder = new StringBuilder();
        if(!Main.zset.containsKey(key)){
            cmdBuilder.append("*0\r\n");
            return cmdBuilder.toString();
        }
        RedisSortedSet st=Main.zset.get(key);
        ArrayList<String> range=st.range(start,end);
        cmdBuilder.append("*");
        cmdBuilder.append(range.size());
        cmdBuilder.append("\r\n");
        for(String ind:range){
            cmdBuilder.append("$");
            cmdBuilder.append(ind.length());
            cmdBuilder.append("\r\n");
            cmdBuilder.append(ind);
            cmdBuilder.append("\r\n");
        }
        return cmdBuilder.toString();
    }
}
