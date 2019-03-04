import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Httpfs {

    private final String HTTP_ERROR_404 = "HTTP/1.1 404 not found\r\n";
    private final String HTTP_SUCCESS_200 = "HTTP/1.1 200 OK\r\n";
    private boolean debug = false;
    private int port = 999; // todo change to 8080 for submission
    private String path = "./data/";

    public static void main(String[] args) throws IOException {
        new Httpfs().run(args);
    }

    private void parseArgs(String[] args) {
        if (args == null || args.length == 0)
            return;

        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        if (args[0].equals("-v")) {
            debug = true;
        }

        if (argsList.contains("-p")) {
            port = Integer.parseInt(args[argsList.indexOf("-p") + 1]);
        }

        if (argsList.contains("-d")) {
            String pathToDir = args[argsList.indexOf("-d") + 1];
            path = path + pathToDir + "/";
        }

        if (debug) {
            System.out.println("Server running on port " + port);
            System.out.println("Path to directory: " + path);

        }
    }

    private void run(String[] args) throws IOException {
        parseArgs(args);

        ServerSocket serverSocket = new ServerSocket(port);
        Socket socketConnection;

        System.out.println("Server is running!");

        while (true) {
            StringBuilder payload = new StringBuilder();
            String request = "";

            socketConnection = serverSocket.accept();
            InputStreamReader in = new InputStreamReader(socketConnection.getInputStream());
            BufferedReader reader = new BufferedReader(in);
            PrintWriter out = new PrintWriter(socketConnection.getOutputStream(), true);
            String line = reader.readLine();
            String urlPath = null;

            while (line != null && !line.isEmpty()) {
                if (line.contains("GET") || line.contains("POST")) {
//                    System.out.println(line);
                    if (line.contains("GET")) {
                        request = "GET";
                    } else if (line.contains("POST")) {
                        request = "POST";
                    }
                    urlPath = line.substring(line.indexOf("/") + 1, line.indexOf("HTTP/"));
//                    System.out.println(urlPath);
                }
                line = reader.readLine();

            }

            if (request.equals("GET")) {
                if (urlPath != null && !urlPath.equals(" ")) {
                    handleGetPath(out, urlPath);
                    socketConnection.close();
                } else if (urlPath != null && urlPath.equals(" ")) {
                    handleDefaultGet(out);
                    socketConnection.close();
                }
            }

            if (request.equals("POST")) {
                System.out.println("Payload data is: " + payload.toString());

                // Parse the payload for body

                // Overwrite or create new file with body

                // Close socket (Make sure to return status code)
                socketConnection.close();
            }
        }
    }

    private List<String> listFilesForFolder(final File folder, List<String> fileList) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry, fileList);
            } else {
                System.out.println(fileEntry.getName());
                fileList.add(fileEntry.getName());
            }
        }

        return fileList;
    }

    private void handleDefaultGet(PrintWriter out) {
        File folder = new File(path);
        List<String> fileList = new ArrayList<>();
        fileList = listFilesForFolder(folder, fileList);

        out.print(HTTP_SUCCESS_200);

        // Output all files found
        for (String file : fileList) {
//            System.out.println(file);
            out.print("\r\n" + file);
        }

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
        if (containsFile(urlPath)) {
            out.print(HTTP_SUCCESS_200);
            printFileContent(out, urlPath);
            out.print("\r\n");
            out.println();
        } else {
            // File does not exist
            out.print(HTTP_ERROR_404);
            out.print("\r\nFile not found");
            out.print("\r\n");
            out.println();
        }
    }


    private void printFileContent(PrintWriter out, String filePath) {
        try {
            filePath = filePath.trim();
            Scanner sc = new Scanner(new FileInputStream(path + filePath + ".txt"));
            String line = sc.nextLine();

            // Print first line
            out.print("\r\n" + line);

            if (debug) {
                System.out.println(line);
            }

            // Print rest of lines
            while (sc.hasNextLine()) {
                line = sc.nextLine();

                if (debug) {
                    System.out.println(line);
                }
                out.print("\r\n" + line);
            }
        } catch (FileNotFoundException e) {
            if (debug) {
                System.out.println("File '" + filePath + "' was not found.");
            }
        }
    }

    /**
     * Prints content of file
     *
     * @param filePath - Path of url
     * @return true if file exists
     */
    private boolean containsFile(String filePath) {
        try {
            filePath = filePath.trim();
            Scanner sc = new Scanner(new FileInputStream(path + filePath + ".txt"));

            if (sc.hasNextLine()) {
                return true;
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
