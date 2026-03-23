import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
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
            return cmdBuilder.toString();
        }
        return cmdBuilder.toString();
    }
}
