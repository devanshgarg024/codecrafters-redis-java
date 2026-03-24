import java.util.ArrayList;

public class Authentication {
    public static String acl(ArrayList<String>words){
        if(words.get(3).equals("WHOAMI")){
            return "$7\r\ndefault\r\n";
        }
        else if(words.get(3).equals("GETUSER")){
            StringBuilder cmdBuilder=new StringBuilder();
            cmdBuilder.append("*4\r\n");
            cmdBuilder.append("$5\r\nflags\r\n");
            cmdBuilder.append("*1\r\n$6\r\nnopass\r\n");
            cmdBuilder.append("$8\r\npassword\r\n");
            cmdBuilder.append("*0\r\n");
            return cmdBuilder.toString();
        }
        return "";
    }
}
