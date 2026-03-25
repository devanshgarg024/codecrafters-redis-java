import java.net.Socket;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Authentication {

    public static String generateSHA256Hash(String input) {
        try {
            // Get an instance of the SHA-256 MessageDigest
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert the input string to bytes using UTF-8 encoding
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                // Format each byte as a two-digit hexadecimal number
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // Handle the exception if the algorithm is not available
            e.printStackTrace();
            return null;
        }
    }


    public static String acl(ArrayList<String>words, Socket clientSocket){
            StringBuilder cmdBuilder=new StringBuilder();
        if(words.get(3).equals("WHOAMI")){
            return "$7\r\ndefault\r\n";
        }
        else if(words.get(3).equals("GETUSER")){
            cmdBuilder.append("*4\r\n");
            cmdBuilder.append("$5\r\nflags\r\n");
            if(Main.password.containsKey(clientSocket)){
                cmdBuilder.append("*0\r\n");
                cmdBuilder.append("$9\r\npasswords\r\n");
                String encryptedPassword=Main.password.get(clientSocket);
                cmdBuilder.append("*1\r\n$").append(encryptedPassword.length()).append("\r\n").append(encryptedPassword).append("\r\n");
            }
            else{
                cmdBuilder.append("*1\r\n$6\r\nnopass\r\n");
                cmdBuilder.append("$9\r\npasswords\r\n");
                cmdBuilder.append("*0\r\n");
            }
            return cmdBuilder.toString();
        }
        else if(words.get(3).equals("SETUSER")){
            String encryptedPassword=generateSHA256Hash(words.get(7).substring(1));
            assert encryptedPassword != null;
            Main.password.put(clientSocket,encryptedPassword);
            cmdBuilder.append("+OK\r\n");
            return cmdBuilder.toString();

        }
        return "";
    }
    public static String auth(ArrayList<String>words, Socket clientSocket) {
        String passwordSubmitted=generateSHA256Hash(words.get(5));
        String actualPassword=Main.password.get(clientSocket);
        assert passwordSubmitted != null;
        if(passwordSubmitted.equals(actualPassword)){
            return "+OK\r\n";
        }
        else {
            return "-WRONGPASS invalid username-password pair or user is disabled.\r\n";
        }

    }

}
