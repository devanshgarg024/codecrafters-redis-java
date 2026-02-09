import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    //  Uncomment the code below to pass the first stage
        int port = 6379;
      try (var executor = Executors.newVirtualThreadPerTaskExecutor();
           ServerSocket serverSocket = new ServerSocket(port)) {

          serverSocket.setReuseAddress(true);
          System.out.println("Redis server started on port " + port);
          // Wait for connection from client.
         while(true) {
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
        InputStream input = clientSocket.getInputStream();
        OutputStream output = clientSocket.getOutputStream();
        byte[] buffer = new byte[1024];
//
//             // Read the command from the client
        while(true){
            int bytesRead = input.read(buffer);
            String message = new String(buffer, 0, bytesRead);
//             System.out.println(message);
            if(message.toLowerCase().contains("ping")){
                output.write("+PONG\r\n".getBytes());
            }
            else if(message.toLowerCase().contains("close")){
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
