import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Httpfs {

    private final String HTTP_ERROR_404 = "HTTP/1.1 404 not found\r\n";
    private final String HTTP_SUCCESS_200 = "HTTP/1.1 200 OK\r\n";
    private boolean debug = false;
    private int port = 999; // todo change to 8080 for submission
    private String path = ".\\data\\";

    public static void main(String[] args) throws IOException {
        new Httpfs().run(args);

    }

    private void run(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socketConnection;

        System.out.println("Server is running!");

        debug = true; // todo set debug through args
        // todo Also set PORT!
        // todo add path if specified

        while (true) {
            socketConnection = serverSocket.accept();
            InputStreamReader in = new InputStreamReader(socketConnection.getInputStream());
            BufferedReader reader = new BufferedReader(in);
            PrintWriter out = new PrintWriter(socketConnection.getOutputStream(), true);
            String line = reader.readLine();
            String urlPath = null;

            while (line != null && !line.isEmpty()) {
                if (line.contains("GET") || line.contains("POST")) {
                    urlPath = line.substring(line.indexOf("/") + 1, line.indexOf("HTTP/"));
                }
                line = reader.readLine();
            }

            if (urlPath != null && !urlPath.equals(" ")) {
                handleGetPath(out, urlPath);
                socketConnection.close();
            } else if (urlPath != null && urlPath.equals(" ")) {
                handleDefaultGet(out);
                socketConnection.close();
            }
        }
    }

    // todo (All this does right now is print out files, do we need to return them??)
    private void handleDefaultGet(PrintWriter out) {
        // print list of files
        File f = new File(path); // current directory

        File[] files = f.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (debug) {
                        System.out.println("Inside Directory: " + file);
                    }
                } else {
                    String pathName = file.toString();
                    String filename = pathName.substring(path.length());
                    if (debug) {
                        System.out.println("Found file: " + filename);
                    }
                }
            }
        }

        out.print(HTTP_SUCCESS_200);
        out.print("\r\n");
        out.println();
    }

    /**
     * Handles GET route with resource
     *
     * @param out     output stream
     * @param urlPath file path resource
     */
    private void handleGetPath(PrintWriter out, String urlPath) {
        if (printFileContent(urlPath)) {
            out.print(HTTP_SUCCESS_200);
            out.print("\r\n");
            out.println();
        } else {
            // File does not exist
            out.print(HTTP_ERROR_404);
            out.print("\r\n");
            out.println();
        }
    }

    /**
     * Prints content of file
     *
     * @param filePath - Path of url
     * @return true if file exists
     */
    private boolean printFileContent(String filePath) {
        try {
            filePath = filePath.trim();
            Scanner sc = new Scanner(new FileInputStream(path + filePath + ".txt"));
            String line = sc.nextLine();

            while (sc.hasNextLine()) {
                System.out.println(line);
                line = sc.nextLine();
            }
        } catch (FileNotFoundException e) {
            if (debug) {
                System.out.println("File '" + filePath + "' was not found.");
            }
            return false;
        }
        return true;
    }

}
