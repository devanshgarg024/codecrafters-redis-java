import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;

public class slaveConnectionAndAck {
    public static String replconf(Vector<String> words){
    String response="";
        if(words.get(3).toLowerCase().equals("listening-port")){
            return "+OK\r\n";
        }
        else if(words.get(3).toLowerCase().equals("capa")){
            return "+OK\r\n";
        }
        else if(words.get(3).toLowerCase().equals("getack")){
            response=("*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$"+String.valueOf(Main.offset).length()+"\r\n"+Main.offset+"\r\n");
        }
        return response;
    }
    public static String wait(Vector<String > words){
        String response="";
        int NumOfSyncSlave=Integer.parseInt(words.get(3));
        int timeout=Integer.parseInt(words.get(5));
//        synchronized (Main.AllSlaveSockets) {
//            Iterator<Socket> iterator = Main.AllSlaveSockets.iterator();
//            while (iterator.hasNext()) {
//                Socket slave = iterator.next();
//                try {
//
//                    slave.getOutputStream().write(("*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$1\r\n*\r\n").getBytes());
//                    slave.getOutputStream().flush();
//                } catch (IOException e) {
//                    System.out.println("Slave disconnected: " + slave.getRemoteSocketAddress());
//                    iterator.remove();
//                    try {
//                        slave.close();
//                    } catch (IOException ignored) {
//                    }
//                }
//            }
//        }
        response+=":0\r\n";
        return response;
    }
}
