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

public class Main {
        public static Map<String,String> db =new HashMap<>();
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
                    db.put(words.get(3),words.get(4));
                    break;
                case "GET":
                    if(db.containsKey(words.get(3))){
                        String val=db.get(words.get(3));
                        output.write(("$"+String.valueOf(val.length())+"\r\n"+val+"\r\n").getBytes());
                    }
                    else{
                        output.write("$-1\r\n".getBytes());
                    }
                    break;


            }

//                System.out.println(command);
//                if (command.toLowerCase().contains("ping")) {
//                    output.write("+PONG\r\n".getBytes());
//                } else if (command.toLowerCase().contains("close")) {
//                    break;
//                }
//                else if(command.toLowerCase().contains("echo")){
//
//                }
            }

        } catch (IOException e) {
            System.out.println("Error handling Client: " + e.getMessage());
        }
        // Do heavy I/O here (Database, API calls, etc.)
    }
}
