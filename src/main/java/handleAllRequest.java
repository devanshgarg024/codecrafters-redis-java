import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class handleAllRequest {

    static void handleRequest(Socket clientSocket) {
        try (clientSocket) {
            if(Main.password.containsKey("default")){
                Main.isAuthenticated.put(clientSocket,false);
            }
            else{
                Main.isAuthenticated.put(clientSocket,true);
            }
            System.out.println("Processing on: " + Thread.currentThread());
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream output = clientSocket.getOutputStream();
            Queue<ArrayList<String>> queue = new LinkedList<>();
            boolean ishold = false;

            while (true) {
                ArrayList<String> words = new ArrayList<>();
                String begin = reader.readLine();
                if (begin == null) break;

                if (begin.startsWith("*")) {
                    int num = Integer.parseInt(begin.substring(1));
                    for (int i = 0; i < num; i++) {
                        words.add(reader.readLine());
                        words.add(reader.readLine());
                    }
                } else {
                    continue;
                }

                if (words.isEmpty()) continue;

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
                                Main.executeCommand(queue.remove(), output, clientSocket, true);
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
                        String infoContent = "role:" + Main.role + "\r\n" +
                                "master_replid:" + Main.mastersReplID + "\r\n" +
                                "master_repl_offset:"+Main.offset;
                        output.write(("$" + infoContent.length() + "\r\n" + infoContent + "\r\n").getBytes());
                        output.flush();
                        break;

                    default:
                        if (ishold) {
                            queue.add(words);
                            output.write("+QUEUED\r\n".getBytes());
                            break;
                        }
                        Main.executeCommand(words, output, clientSocket, true);
                        output.flush();
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("Error handling Client: " + e.getMessage());
        }
        // Do heavy I/O here (Database, API calls, etc.)
    }
}
