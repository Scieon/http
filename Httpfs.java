import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Httpfs {

    private final String HTTP_ERROR_404 = "HTTP/1.1 404 not found\r\n";
    private final String HTTP_SUCCESS_200 = "HTTP/1.1 200 OK\r\n";
    private final String HTTP_FORBIDDEN_203 = "HTTP/1.1 403 Forbidden\r\n";
    private boolean debug = false;
    private boolean secure = true;
    private boolean endRequest = false;
    private int port = 8080;
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
            String request = "";
            String content ="";

            socketConnection = serverSocket.accept();
            InputStreamReader in = new InputStreamReader(socketConnection.getInputStream());
            BufferedReader reader = new BufferedReader(in);
            PrintWriter out = new PrintWriter(socketConnection.getOutputStream(), true);
            StringBuilder requestBody = new StringBuilder();
            String line = reader.readLine();
            String urlPath = null;

            // Parsing Headers
            while (line != null && !line.isEmpty()) {
                if (line.contains("GET") || line.contains("POST")) {
                    if (line.contains("GET")) {
                        request = "GET";
                    } else if (line.contains("POST")) {
                        request = "POST";
                    }
                    urlPath = line.substring(line.indexOf("/") + 1, line.indexOf("HTTP/"));
                    secure = isSecure(line, urlPath);
                }
                System.out.println(line);
                line = reader.readLine();
            }

            // Parsing Request Body
            while (reader.ready()) {
            requestBody.append((char) reader.read());
            }
            content = requestBody.toString();

            if(!secure) {
                 handleSecurity(out); 
                 socketConnection.close();
             } else {
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
                    handlePostPath(out, urlPath, content);
                    socketConnection.close();
                }
            }
        }
    }

    private boolean isSecure(String line, String urlPath) {
        if (line.contains("..") || urlPath.contains("/")) {
            return false;
        }
        return true;
    }

    private void handleSecurity(PrintWriter out) {
        out.print(HTTP_FORBIDDEN_203);
        out.print("\r\nYou do not have security access.");
        out.print("\r\n");
        out.println();
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

    /**
     * Handles standard GET 
     *
     * @param out - Output Stream 
     * @return a list of files within the specified directory
     */
    private void handleDefaultGet(PrintWriter out) {
        File folder = new File(path);
        List<String> fileList = new ArrayList<>();
        fileList = listFilesForFolder(folder, fileList);

        out.print(HTTP_SUCCESS_200);

            // Output all files found
        for (String file : fileList) {
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

    /**
     * Handles POST request
     *
     * @param out     output stream
     * @param urlPath file path resource
     */
    private void handlePostPath(PrintWriter out, String urlPath, String content){
        // String[] data = {"does", "it", "override"};

        if (writeFileContent(content, urlPath)) {
            out.print(HTTP_SUCCESS_200);
            out.print("\r\n");
            out.println();
        } else {
            out.print(HTTP_ERROR_404);
            out.print("\r\n");
            out.println();
        }
    }

    /**
     * Writes content to a file
     *
     * @param data - Message body content of request 
     * @param filePath - Path of url
     * @return true if file exists
     */
    private boolean writeFileContent(String data, String filePath) {
        try {
            filePath = filePath.trim();
            FileWriter fw = new FileWriter(path + filePath + ".txt");
            PrintWriter pw = new PrintWriter(fw);

            pw.print(data);
            pw.close();

        } catch (IOException e) {
            if (debug) {
                System.out.println("Error opening " + filePath + ".txt");
            }
            return false;
        }
        return true;
    }
}
