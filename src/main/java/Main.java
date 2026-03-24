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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.security.SecureRandom;
import java.util.Base64;

public class Main {

    public static final Object waitLock = new Object();
    public record user(Socket clientSocket, LocalTime startTime, LocalTime expTime, boolean willExp) {}

    public static ConcurrentHashMap<String, List<user>> PopExp = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> db = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ArrayList<LinkedHashMap<String, String>>> streamdb = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, LocalTime> exp = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, List<String>> elementList = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Socket, Set<String>> subs = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Set<Socket>> channels = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, RedisSortedSet> zset = new ConcurrentHashMap<>();

    public static String role = "master";
    public static String mastersReplID = "?";
    public static AtomicLong offset = new AtomicLong(0);
    public static String RDBfile = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
    public static int port = 6379;
    public static ConcurrentHashMap<Socket, Long> AllSlaveSockets = new ConcurrentHashMap<>();
    public static String dir = null;
    public static String dbfilename = null;

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        String masterHost = null;
        int masterPort = 0;

        if (args.length >= 1 && args[0].equals("--port")) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length >= 3 && args[2].equals("--replicaof")) {
            role = "slave";
            String[] temp = args[3].split(" ");
            masterHost = temp[0];
            masterPort = Integer.parseInt(temp[1]);
        }
        if (args.length >= 2 && args[0].equals("--dir")) {
            dir = args[1];
        }
        if (args.length >= 4 && args[2].equals("--dir")) {
            dbfilename = args[3];
        }

        ExecutorService executor = Executors.newCachedThreadPool();

        if (!role.equals("master")) {
            try {
                Socket masterSocket = new Socket(masterHost, masterPort);
                executor.submit(() -> {
                    handleMasterRequest.handleMasterRequests(masterSocket);
                });

            } catch (IOException e) {
                System.out.println("Could not connect to master: " + e.getMessage());
            }
        } else {
            String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            SecureRandom secureRandom = new SecureRandom();
            mastersReplID = secureRandom.ints(40, 0, ALPHA_NUMERIC.length())
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
                    handleAllRequest.handleRequest(clientSocket);
                });
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }




    public static void executeCommand(ArrayList<String> words, OutputStream output, Socket clientSocket, boolean shouldReturn) {
        try {
            String response;
            if(subs.containsKey(clientSocket)&&!subs.get(clientSocket).isEmpty()){
                switch (words.get(1).toUpperCase()) {
                    case "PING":
                        if (shouldReturn) output.write("*2\r\n$4\r\npong\r\n$0\r\n\r\n".getBytes());
                        break;
                    case "SUBSCRIBE":
                        response=subAndPub.subscribe(words, clientSocket);
                        if(shouldReturn)output.write(response.getBytes());
                        break;
                    case "UNSUBSCRIBE":
                        response=subAndPub.unsubscribe(words, clientSocket);
                        if(shouldReturn)output.write(response.getBytes());
                        break;
//                    case "PSUBSCRIBE":
//                        response=subAndPub.subscribe(words, clientSocket);
//                        if(shouldReturn)output.write(response.getBytes());
//                        break;
                    default:
                        response="-ERR Can't execute '"+words.get(1)+ "' in subscribed mode\r\n";
                        if (shouldReturn)output.write(response.getBytes());
                }
            }
            else{
            switch (words.get(1).toUpperCase()) {
                case "PING":
                    if (shouldReturn) output.write("+PONG\r\n".getBytes());
                    break;
                case "ECHO":
                    response = (words.get(2) + "\r\n" + words.get(3) + "\r\n");
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "SET":
                    sendToSlaves(words, true);
                    response = getSetCommand.set(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "GET":
                    response = getSetCommand.get(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "RPUSH":
                    sendToSlaves(words, true);
                    response = pushRangeCommand.rpush(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "LPUSH":
                    sendToSlaves(words, true);
                    response = pushRangeCommand.lpush(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "LRANGE":
                    response = pushRangeCommand.lrange(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "LLEN":
                    response = pushRangeCommand.llen(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "LPOP":
                    sendToSlaves(words, true);
                    response = popCommand.lpop(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "BLPOP":
                    sendToSlaves(words, true);
                    response = popCommand.blpop(words, clientSocket);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "TYPE":
                    response = typeCommand.type(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "XADD":
                    sendToSlaves(words, true);
                    response = streamCommand.xadd(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "XRANGE":
                    response = streamCommand.xrange(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "XREAD":
                    response = streamCommand.xread(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "INCR":
                    sendToSlaves(words, true);
                    response = OperationCommand.incr(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "SUBSCRIBE":
                    response=subAndPub.subscribe(words, clientSocket);
                    if(shouldReturn)output.write(response.getBytes());
                    break;
                case "PUBLISH":
                    response=subAndPub.publish(words);
                    if(shouldReturn)output.write(response.getBytes());
                    break;
                case "ZADD":
                    sendToSlaves(words, true);
                    response = sortedSet.zadd(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "ZRANK":
                    response = sortedSet.zrank(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "ZRANGE":
                    response = sortedSet.zrange(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "ZCARD":
                    response = sortedSet.zcard(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "ZSCORE":
                    response = sortedSet.zscore(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "ZREM":
                    sendToSlaves(words, true);
                    response = sortedSet.zrem(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "GEOADD":
                    sendToSlaves(words, true);
                    response = geoSet.geoadd(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "GEOPOS":
                    response = geoSet.geopos(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "GEODIST":
                    response = geoSet.geodist(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;
                case "GEOSEARCH":
                    response = geoSet.geosearch(words);
                    if (shouldReturn) output.write(response.getBytes());
                    break;

//                case "KEYS":
//                    String format=words.get(3);
//                    format=format.substring(0,format.length()-1);
//                    ArrayList<String> validkeys=new ArrayList<>();
//
//                    String finalFormat = format;
//                    db.forEach((key, value) -> {
//                        if(key.startsWith(finalFormat)){
//                            validkeys.add(key);
//                        }
//                    });
//                    StringBuilder cmdBuilder = new StringBuilder();
//                    cmdBuilder.append('*');
//                    cmdBuilder.append(validkeys.size());
//                    cmdBuilder.append("\r\n");
//                    for(String temp:validkeys){
//                        cmdBuilder.append('$');
//                        cmdBuilder.append(temp.length());
//                        cmdBuilder.append("\r\n");
//                        cmdBuilder.append(temp);
//                        cmdBuilder.append("\r\n");
//                    }
//                    response= cmdBuilder.toString();
//                    if (shouldReturn) output.write(response.getBytes());

                case "REPLCONF":
                    response = slaveConnectionAndAck.replconf(words, clientSocket);
                    if (!response.isEmpty()) output.write(response.getBytes());
                    break;
                case "PSYNC":
                    Base64.Decoder decoder = Base64.getDecoder();
                    byte[] decodedBytes = decoder.decode(RDBfile);
                    output.write(("+FULLRESYNC " + mastersReplID + " " + offset.get() + "\r\n").getBytes());
                    output.flush();
                    output.write(("$" + decodedBytes.length + "\r\n").getBytes());
                    output.write(decodedBytes);
                    output.flush();
                    AllSlaveSockets.put(clientSocket, 0L);
                    break;
                case "WAIT":
                    response = slaveConnectionAndAck.wait(words);
                    output.write(response.getBytes());
                    break;
                case "CONFIG":
                    if (words.get(5).equals("dir")) {
                        output.write(("*2\r\n$3\r\ndir\r\n$" + dir.length() + "\r\n" + dir + "\r\n").getBytes());
                    } else {
                        output.write(("*2\r\n$3\r\ndbfilename\r\n$" + dbfilename.length() + "\r\n" + dbfilename + "\r\n").getBytes());
                    }
                    break;
                default:
                    output.write("-ERR unknown command\r\n".getBytes());
                    break;
            }
            }
        } catch (IOException e) {
            System.out.println("Error handling Client: " + e.getMessage());
        }
    }

    public static void sendToSlaves(ArrayList<String> words, boolean offChange) {
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append("*").append(words.size() / 2).append("\r\n");
        for (String word : words) {
            cmdBuilder.append(word).append("\r\n");
        }
        String cmd = cmdBuilder.toString();
        byte[] cmdBytes = cmd.getBytes(); // Get correct byte length

        if (role.equals("master")) {
            if (offChange) offset.addAndGet(cmdBytes.length);
            Iterator<Map.Entry<Socket, Long>> iterator = AllSlaveSockets.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Socket, Long> entry = iterator.next();
                try {
                    entry.getKey().getOutputStream().write(cmdBytes);
                    entry.getKey().getOutputStream().flush();
                } catch (IOException e) {
                    iterator.remove();
                }
            }
        }
    }
}