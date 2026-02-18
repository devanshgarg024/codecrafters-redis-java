import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class slaveConnectionAndAck {

    public static String replconf(ArrayList<String> words, Socket clientSocket) {
        if (words.size() > 3 && words.get(3).equalsIgnoreCase("listening-port")) {
            return "+OK\r\n";
        } else if (words.size() > 3 && words.get(3).equalsIgnoreCase("capa")) {
            return "+OK\r\n";
        } else if (words.size() > 3 && words.get(3).equalsIgnoreCase("getack")) {
            return "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$" +
                    String.valueOf(Main.offset.get()).length() + "\r\n" + Main.offset.get() + "\r\n";
        } else if (words.size() > 3 && words.get(3).equalsIgnoreCase("ack")) {
            processAck(words, clientSocket);
            return "";
        }
        return "";
    }

    private static void processAck(ArrayList<String> words, Socket clientSocket) {
        try {
            int offrec = Integer.parseInt(words.get(5));
            Main.Slaves slaveData = Main.AllSlaveSockets.get(clientSocket);
            slaveData.slaveOffset = offrec;
            synchronized (Main.waitLock){
                Main.waitLock.notifyAll();
            }
        } catch (Exception e) {
            System.out.println("Ack Error: " + e.getMessage());
        }
    }

    public static String wait(ArrayList<String> words) {
        int targetReplicas = Integer.parseInt(words.get(3));
        int timeout = Integer.parseInt(words.get(5));
            ArrayList<String> getAckCmd = new ArrayList<>();
            getAckCmd.add("$8"); getAckCmd.add("REPLCONF");
            getAckCmd.add("$6"); getAckCmd.add("GETACK");
            getAckCmd.add("$1"); getAckCmd.add("*");
            Main.sendToSlaves(getAckCmd,false);

        long startTime = System.currentTimeMillis();
        synchronized (Main.waitLock) {
            long syncedCount = countSyncedSlaves();
            while (syncedCount < targetReplicas) {
                long now = System.currentTimeMillis();
                long elapsed = now - startTime;
                if (elapsed >= timeout)break;
                try {
                    long waitTime = timeout - elapsed;
                    if (waitTime > 0) {
                        Main.waitLock.wait(waitTime);
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                syncedCount = countSyncedSlaves();
            }
            return ":" + syncedCount + "\r\n";
        }
    }

    private static long countSyncedSlaves() {
        int count = 0;
        for (Main.Slaves slave : Main.AllSlaveSockets.values()) {
            if (slave.slaveOffset >= slave.mastersOffset) {
                count++;
            }
        }
        return count;
    }
}