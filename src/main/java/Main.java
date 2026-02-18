import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.security.SecureRandom;
import java.util.Base64;

public class Main {
    public static class Slaves {
        public int mastersOffset;
        public int slaveOffset;

        public Slaves(int mastersOffset, int slaveOffset) {
            this.mastersOffset = mastersOffset;
            this.slaveOffset = slaveOffset;
        }
    }

    public static final Object waitLock = new Object();
    public static volatile CountDownLatch ackLatch = null;
    public record user(Socket clientSocket, LocalTime startTime, LocalTime expTime,boolean willExp ){}
    public static ConcurrentHashMap<String, List<user>> PopExp=new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,String> db =new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,ArrayList<LinkedHashMap<String,String>>> streamdb =new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,LocalTime> exp =new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,List<String>> elementList =new ConcurrentHashMap<>();
    public static String role="master";
    public static String mastersReplID="?";
    public static AtomicInteger offset=new AtomicInteger(-1);
    public static String RDBfile="UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
    public static int port=6379;
    public static ConcurrentHashMap<Socket,Slaves> AllSlaveSockets=new ConcurrentHashMap<>();
    public static String dir=null;
    public static String dbfilename=null;
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
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
        if(args.length>=2&&args[0].equals("--dir")){
            dir=args[1];
        }
        if(args.length>=4&&args[2].equals("--dir")){
            dbfilename=args[3];
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        if(!role.equals("master")){
            try{
                Socket masterSocket=new Socket("localhost",6379);
                executor.submit(() -> {
                    handleMasterRequests(masterSocket);
                });

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

    private static void handleMasterRequests(Socket masterSocket){
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(masterSocket.getInputStream(), java.nio.charset.StandardCharsets.ISO_8859_1));
        OutputStream output=masterSocket.getOutputStream();
        output.write("*1\r\n$4\r\nPING\r\n".getBytes());
        output.flush();
        reader.readLine();

        output.write(("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$"+String .valueOf(port).length()+"\r\n"+port+"\r\n").getBytes());
        output.flush();
        reader.readLine();

        output.write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes());
        output.flush();
        reader.readLine();

        output.write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes());

        output.flush();
        reader.readLine();
        offset.set(0);


            String rdbSizeLine = reader.readLine(); // $88
            if (rdbSizeLine != null && rdbSizeLine.startsWith("$")) {
                int length = Integer.parseInt(rdbSizeLine.substring(1));
                char[] buffer = new char[length];
                int totalRead = 0;
                while (totalRead < length) {
                    int read = reader.read(buffer, totalRead, length - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
            }


        while(true){
            ArrayList<String> words = new ArrayList<>();
            String begin=reader.readLine();
            if (begin == null) break;
            if(begin.startsWith("*")){
                int num=Integer.parseInt(begin.substring(1));
                for(int i=0;i<num;i++){
                    words.add(reader.readLine()) ;
                    words.add(reader.readLine());
                }
            }
            else continue;

            executeCommand(words,output,masterSocket,false);
            int calcoffset = 0;
            StringBuilder cmdBuilder = new StringBuilder();
            cmdBuilder.append("*").append(words.size()/2).append("\r\n");
            for(String w : words) {
                cmdBuilder.append(w).append("\r\n");
            }
            calcoffset = cmdBuilder.toString().getBytes().length;
            offset.addAndGet(calcoffset);
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
            Queue<ArrayList<String>> queue = new LinkedList<>();
            boolean ishold=false;
            while(true){
                ArrayList<String> words = new ArrayList<>();
                String begin=reader.readLine();
                if (begin == null) break;
                if(begin.startsWith("*")){
                    int num=Integer.parseInt(begin.substring(1));
                    for(int i=0;i<num;i++){
                        words.add(reader.readLine()) ;
                        words.add(reader.readLine());
                    }
                } else continue;
                if(words.isEmpty())continue;
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
                                executeCommand(queue.remove(), output, clientSocket,true);
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
                                "master_replid:"+mastersReplID+"\r\n" +
                                "master_repl_offset:0";
                        output.write(("$" + infoContent.length() + "\r\n" + infoContent+"\r\n").getBytes());
                        output.flush();
                        break;
                    default:
                        if(ishold){
                            queue.add(words);
                            output.write("+QUEUED\r\n".getBytes());
                            break;
                        }
                        executeCommand(words, output, clientSocket,true);
                        output.flush();
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("Error handling Client: " + e.getMessage());
        }
        // Do heavy I/O here (Database, API calls, etc.)
    }
    public static void executeCommand(ArrayList<String> words,OutputStream output, Socket clientSocket,boolean shouldReturn){
        try{
            String response;

            switch(words.get(1).toUpperCase()){
            case "PING":
                if(shouldReturn)output.write("+PONG\r\n".getBytes());
                break;
            case "ECHO":
                response=(words.get(2)+"\r\n"+words.get(3)+"\r\n");
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "SET":
                sendToSlaves(words,true);
                response=getSetCommand.set(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "GET":
                response=getSetCommand.get(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "RPUSH":
                sendToSlaves(words,true);
                response=pushRangeCommand.rpush(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "LPUSH":
                sendToSlaves(words,true);
                response=pushRangeCommand.lpush(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "LRANGE":
                response=pushRangeCommand.lrange(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "LLEN":
                response=pushRangeCommand.llen(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "LPOP":
                sendToSlaves(words,true);
                response=popCommand.lpop(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "BLPOP":
                sendToSlaves(words,true);
                response=popCommand.blpop(words,clientSocket);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "TYPE":
                response=typeCommand.type(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "XADD":
                sendToSlaves(words,true);
                response=streamCommand.xadd(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "XRANGE":
                response=streamCommand.xrange(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "XREAD":
                response=streamCommand.xread(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "INCR":
                sendToSlaves(words,true);
                response=OperationCommand.incr(words);
                if(shouldReturn)output.write(response.getBytes());
                break;
            case "REPLCONF":
                response=slaveConnectionAndAck.replconf(words,clientSocket);
                if(!response.isEmpty())output.write(response.getBytes());
                break;
            case "PSYNC":
                Base64.Decoder decoder = Base64.getDecoder();
                byte[] decodedBytes = decoder.decode(RDBfile);
                output.write(("+FULLRESYNC "+mastersReplID+" 0\r\n").getBytes());
                output.flush();
                output.write(("$"+decodedBytes.length+"\r\n").getBytes());
                output.write(decodedBytes);
                Slaves slave=new Slaves(0,0);
                AllSlaveSockets.put(clientSocket,slave);
                break;
            case "WAIT":
                response=slaveConnectionAndAck.wait(words);
                output.write(response.getBytes());
                break;
                case "CONFIG":
                    if(words.get(5).equals("dir")){
                        output.write(("*2\r\n$3\r\ndir\r\n$"+dir.length()+"\r\n"+dir+"\r\n").getBytes());
                    }
                    else{
                        output.write(("*2\r\n$3\r\ndbfilename\r\n$"+dbfilename.length()+"\r\n"+dbfilename+"\r\n").getBytes());
                    }
                    break;
            default:
                output.write("-ERR unknown command\r\n".getBytes());
                break;

        }
         } catch (IOException e) {
                System.out.println("Error handling Client: " + e.getMessage());
            }
    }
    public static void sendToSlaves(ArrayList<String> words, boolean isReplicationTraffic) {
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("*").append(words.size() / 2).append("\r\n");
        for (String word : words) {
            cmdBuilder.append(word).append("\r\n");
        }
        String cmd = cmdBuilder.toString();
        byte[] cmdBytes = cmd.getBytes(); // Get correct byte length

        if (role.equals("master")) {
                Iterator<Map.Entry<Socket, Slaves>> iterator = AllSlaveSockets.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Socket, Slaves> entry = iterator.next();
                    try {
                        entry.getKey().getOutputStream().write(cmdBytes);
                        entry.getKey().getOutputStream().flush();
                        if(isReplicationTraffic)entry.getValue().mastersOffset += cmdBytes.length;
                    } catch (IOException e) {
                        iterator.remove();
                    }
                }
        }
    }
}
