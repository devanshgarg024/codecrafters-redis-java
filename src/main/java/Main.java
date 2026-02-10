import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream; // This was missing
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalTime;

public class Main {
    public static Map<String,String> db =new HashMap<>();
    public static Map<String,LocalTime> exp =new HashMap<>();
    public static Map<String,List<String>> elementList =new HashMap<>();
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        int port = 6379;

        // FIX: Create the executor OUTSIDE the try-with-resources block
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Redis server started on port " + port);

            // Wait for connection from client.
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> {
                    handleRequest(clientSocket);
                });
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (clientSocket) {
            System.out.println("Processing on: " + Thread.currentThread());
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream output = clientSocket.getOutputStream();
            while(true){
            Vector<String> words = new Vector<>();
            String begin=reader.readLine();
//            System.out.println(begin);
            if(begin.startsWith("*")){
                int num=Integer.parseInt(begin.substring(1));
                for(int i=0;i<num;i++){
                    words.add(reader.readLine());
                    words.add(reader.readLine());
                }
            }


//            for(int i=0;i<words.size();i++){
//                System.out.println(words.get(i));
//            }
            switch(words.get(1)){
                case "PING":
                    output.write("+PONG\r\n".getBytes());
                    break;
                case "ECHO":
                    output.write((words.get(2)+"\r\n"+words.get(3)+"\r\n").getBytes());
                    break;
                case "SET":
                    if(words.size()==6){
                        db.put(words.get(3),words.get(5));
                        output.write("+OK\r\n".getBytes());
                        exp.remove(words.get(3));
                    }
                    else if(words.size()==10){
                        LocalTime expTime;
//                        System.out.println(words.get(7).toLowerCase());
                        if(words.get(7).toLowerCase().equals("ex")){
                            expTime = LocalTime.now().plusSeconds(Integer.parseInt(words.get(9)));
                        }
                        else if(words.get(7).toLowerCase().equals("px")){
                            long nanoSecToAdd=Integer.parseInt(words.get(9));
                            nanoSecToAdd=nanoSecToAdd*1000000;
                            expTime = LocalTime.now().plusNanos(nanoSecToAdd);
                        }
                        else{
                            return ;
                        }
                        db.put(words.get(3),words.get(5));
                        exp.put(words.get(3),expTime);
                        output.write("+OK\r\n".getBytes());
                    }
                    break;
                case "GET":
                    LocalTime now = LocalTime.now();
                    if(db.containsKey(words.get(3))&&(!exp.containsKey(words.get(3))||exp.get(words.get(3)).isAfter(now))){
                        String val=db.get(words.get(3));
                        System.out.println(val);
                        output.write(("$"+String.valueOf(val.length())+"\r\n"+val+"\r\n").getBytes());
                    }
                    else{
                        output.write("$-1\r\n".getBytes());
                    }
                    break;
                case "RPUSH":
                    String key=words.get(3);
                    if(elementList.containsKey(key)){
                        elementList.get(key).add(words.get(5));
                    }
                    else{
                        List<String> l=new ArrayList<>();
                        l.add(words.get(5));
                        elementList.put(key,l);
                    }
                    int sizOfList=elementList.get(key).size();
                    output.write((":"+String.valueOf(sizOfList)+"\r\n").getBytes());
                    break;

            }
            }

        } catch (IOException e) {
            System.out.println("Error handling Client: " + e.getMessage());
        }
        // Do heavy I/O here (Database, API calls, etc.)
    }
}
