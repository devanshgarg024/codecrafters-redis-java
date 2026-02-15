import com.sun.source.doctree.ValueTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalTime;

import static java.lang.Math.max;
import static java.lang.Math.min;


public class Main {
    public record user(Socket clientSocket, LocalTime startTime, LocalTime expTime,boolean willExp ){}
    public static Map<String, List<user>> PopExp=new HashMap<>();
    public static Map<String,String> db =new HashMap<>();
    public static Map<String,ArrayList<LinkedHashMap<String,String>>> streamdb =new HashMap<>();
    public static Map<String,LocalTime> exp =new HashMap<>();
    public static Map<String,List<String>> elementList =new HashMap<>();
    public static String role="master";
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        int port=6379;
            if(args.length>=1&&args[0].equals("--port")){
                port=Integer.parseInt(args[1]);
            }
            if(args.length>=3&&args[2].equals("--replicaof"))role="slave";

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
            Queue<Vector<String>> queue = new LinkedList<>();
            boolean ishold=false;
            while(true){
            Vector<String> words = new Vector<>();
            String begin=reader.readLine();
            if(begin.startsWith("*")){
                int num=Integer.parseInt(begin.substring(1));
                for(int i=0;i<num;i++){
                    words.add(reader.readLine());
                    words.add(reader.readLine());
                }
            }
            if(words.get(1).equals("MULTI")){
                ishold=true;
                output.write("+OK\r\n".getBytes());
                continue;
            }
            if(words.get(1).equals("EXEC")){
                String response="";
                if(!ishold){
                    response+="-ERR EXEC without MULTI\r\n";
                    output.write(response.getBytes());
                }
                else{
                    ishold=false;
                    response+=("*"+queue.size()+"\r\n");
                    output.write(response.getBytes());
                    while(!queue.isEmpty()){
                        executeCommand(queue.remove(),output,clientSocket,ishold);
                    }
                }
                continue;
            }
            if(words.get(1).equals("DISCARD")){
                if(ishold){
                    queue.clear();
                    output.write("+OK\r\n".getBytes());
                    ishold=false;
                }
                else{
                    output.write("-ERR DISCARD without MULTI\r\n".getBytes());
                }
                continue;
            }
            if(words.get(1).equals("INFO")){
                output.write(("$"+role.length()+"\r\nrole:"+role+"\r\n").getBytes());
            }

            if(ishold){
                queue.add(words);
                output.write("+QUEUED\r\n".getBytes());
                continue;
            }
                executeCommand(words, output, clientSocket, ishold);
//            for(int i=0;i<words.size();i++){
//                System.out.println(words.get(i));
//            }


            }

        } catch (IOException e) {
            System.out.println("Error handling Client: " + e.getMessage());
        }
        // Do heavy I/O here (Database, API calls, etc.)
    }
    public static void executeCommand(Vector<String> words,OutputStream output, Socket clientSocket, boolean ishold ){
        try{

            switch(words.get(1)){
            case "PING":
                output.write("+PONG\r\n".getBytes());
                break;
            case "ECHO":
                output.write((words.get(2)+"\r\n"+words.get(3)+"\r\n").getBytes());
                break;
            case "SET":
                output.write(getSetCommand.set(words).getBytes());
                break;
            case "GET":
                output.write(getSetCommand.get(words).getBytes());
                break;
            case "RPUSH":
                output.write(pushRangeCommand.rpush(words).getBytes());
                break;
            case "LPUSH":
                output.write(pushRangeCommand.lpush(words).getBytes());
                break;
            case "LRANGE":
                output.write(pushRangeCommand.lrange(words).getBytes());
                break;
            case "LLEN":
                output.write(pushRangeCommand.llen(words).getBytes());
                break;
            case "LPOP":
                output.write(popCommand.lpop(words).getBytes());
                break;
            case "BLPOP":
                output.write(popCommand.blpop(words,clientSocket).getBytes());
                break;
            case "TYPE":
                output.write(typeCommand.type(words).getBytes());
                break;
            case "XADD":
                output.write(streamCommand.xadd(words).getBytes());
                break;
            case "XRANGE":
                output.write(streamCommand.xrange(words).getBytes());
                break;
            case "XREAD":
                output.write(streamCommand.xread(words).getBytes());
                break;
            case "INCR":
                output.write(OperationCommand.incr(words).getBytes());
                break;
        }
         } catch (IOException e) {
                System.out.println("Error handling Client: " + e.getMessage());
            }
    }
}
