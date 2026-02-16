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
import java.util.stream.Collectors;
import java.security.SecureRandom;

public class Main {
    public record user(Socket clientSocket, LocalTime startTime, LocalTime expTime,boolean willExp ){}
    public static Map<String, List<user>> PopExp=new HashMap<>();
    public static Map<String,String> db =new HashMap<>();
    public static Map<String,ArrayList<LinkedHashMap<String,String>>> streamdb =new HashMap<>();
    public static Map<String,LocalTime> exp =new HashMap<>();
    public static Map<String,List<String>> elementList =new HashMap<>();
    public static String role="master";
    public static String mastersReplID="?";
    public static int offset=-1;

    static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        int port=6379;
        String masterHost=null;
        int masterPort=0;

        if(args.length>=1&&args[0].equals("--port")){
            port=Integer.parseInt(args[1]);
        }
        if(args.length>=3&&args[2].equals("--replicaof")){
            role="slave";
            masterHost=args[3];
//            masterPort=Integer.parseInt(args[4]);

//            System.out.println(masterHost);
//            System.out.println(masterPort);
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        if(!role.equals("master")){
            try(Socket slaveSocket=new Socket("localhost",6379)){
            BufferedReader reader = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()));
            OutputStream output=slaveSocket.getOutputStream();
                output.write("*1\r\n$4\r\nPING\r\n".getBytes());
                output.flush();
                reader.readLine();
                System.out.println(reader);
                output.write(("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n"+String.valueOf(port)+"\r\n").getBytes());
                output.flush();
                reader.readLine();
                System.out.println(reader);

                output.write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes());
                output.flush();
                reader.readLine();
                System.out.println(reader);

                output.write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes());
                output.flush();
                reader.readLine();
                System.out.println(reader);


            }
            catch(IOException e) {
                System.out.println("Could not connect to master: " + e.getMessage());
            }
        }
        else{
            String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            SecureRandom secureRandom = new SecureRandom();
            mastersReplID=secureRandom.ints(40, 0, ALPHA_NUMERIC.length())
                    .mapToObj(ALPHA_NUMERIC::charAt)
                    .map(Object::toString)
                    .collect(Collectors.joining());
        }
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
                if (begin == null) break;
                if(begin.startsWith("*")){
                    int num=Integer.parseInt(begin.substring(1));
                    for(int i=0;i<num;i++){
                        words.add(reader.readLine()) ;
                        words.add(reader.readLine());
                    }
                }
                switch (words.get(1).toUpperCase()) {
                    case "MULTI":
                        ishold = true;
                        output.write("+OK\r\n".getBytes());
                        output.flush();
                        break;

                    case "EXEC":
                        if (!ishold) {
                            output.write("-ERR EXEC without MULTI\r\n".getBytes());
                            output.flush();

                        } else {
                            ishold = false;
                            String response = "*" + queue.size() + "\r\n";
                            output.write(response.getBytes());
                            output.flush();

                            while (!queue.isEmpty()) {
                                executeCommand(queue.remove(), output, clientSocket);
                                output.flush();
                            }
                        }
                        break;

                    case "DISCARD":
                        if (ishold) {
                            queue.clear();
                            output.write("+OK\r\n".getBytes());
                            output.flush();

                            ishold = false;
                        } else {
                            output.write("-ERR DISCARD without MULTI\r\n".getBytes());
                            output.flush();

                        }
                        break;

                    case "INFO":
                        String infoContent = "role:" + role + "\r\n" +
                                "master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb\r\n" +
                                "master_repl_offset:0";
                        System.out.println("dsf");
                        output.write(("$" + infoContent.length() + "\r\n" + infoContent+"\r\n").getBytes());
                        System.out.println("dsf");
                        output.flush();
                        break;
                    default:
                        if(ishold){
                            queue.add(words);
                            output.write("+QUEUED\r\n".getBytes());
                            break;
                        }
                        executeCommand(words, output, clientSocket);
                        output.flush();
                        break;
                }

            }

        } catch (IOException e) {
            System.out.println("Error handling Client: " + e.getMessage());
        }
        // Do heavy I/O here (Database, API calls, etc.)
    }
    public static void executeCommand(Vector<String> words,OutputStream output, Socket clientSocket){
        try{

            switch(words.get(1).toUpperCase()){
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
            case "REPLCONF":
                output.write("+OK\r\n".getBytes());
                break;
            case "PSYNC":
                output.write(("+FULLRESYNC"+mastersReplID+"0\r\n").getBytes());
                break;
            default:
                output.write("-ERR unknown command\r\n".getBytes());
                break;

        }
         } catch (IOException e) {
                System.out.println("Error handling Client: " + e.getMessage());
            }
    }
}
