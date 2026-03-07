import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class handleMasterRequest {

    public static void handleMasterRequests(Socket masterSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(masterSocket.getInputStream(), java.nio.charset.StandardCharsets.ISO_8859_1));
            OutputStream output = masterSocket.getOutputStream();

            output.write("*1\r\n$4\r\nPING\r\n".getBytes());
            output.flush();
            reader.readLine();

            output.write(("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$" + String.valueOf(Main.port).length() + "\r\n" + Main.port + "\r\n").getBytes());
            output.flush();
            reader.readLine();

            output.write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes());
            output.flush();
            reader.readLine();

            output.write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes());
            output.flush();

            String a = reader.readLine();
            int temp = Integer.parseInt(a.substring(53));
            Main.offset.set(temp);

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

                Main.executeCommand(words, output, masterSocket, false);
                int calcoffset = 0;
                StringBuilder cmdBuilder = new StringBuilder();
                cmdBuilder.append("*").append(words.size() / 2).append("\r\n");
                for (String w : words) {
                    cmdBuilder.append(w).append("\r\n");
                }
                calcoffset = cmdBuilder.toString().getBytes().length;
                Main.offset.addAndGet(calcoffset);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
