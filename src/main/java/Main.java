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

public class Main {
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
      try(clientSocket){
        System.out.println("Processing on: " + Thread.currentThread());
          BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream output = clientSocket.getOutputStream();
        byte[] buffer = new byte[1024];
//
//             // Read the command from the client
        while(true){
            String command = reader.readLine();
//            String command = new String(buffer, 0, bytesRead);
            System.out.println(command);
            if(command.toLowerCase().contains("ping")){
                output.write("+PONG\r\n".getBytes());
            }
            else if(command.toLowerCase().contains("close")){
                break;
            }
        }

      }
      catch(IOException e){
          System.out.println("Error handling Client: "+ e.getMessage());
        }
        // Do heavy I/O here (Database, API calls, etc.)
    }
}
