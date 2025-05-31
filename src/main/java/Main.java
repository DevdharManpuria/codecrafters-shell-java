/**
 * A simple shell implementation in Java that supports basic command execution,
 * I/O redirection, command autocompletion, and built-in commands.
 * 
 * Features:
 * - Interactive and non-interactive modes
 * - Command autocompletion with tab
 * - I/O redirection (>, >>, 2>, 2>>)
 * - Built-in commands (echo, exit, type, pwd, cd)
 * - Quote handling (both single and double quotes)
 * - Escape sequence support
 */
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unused")
public class Main {
    // Map for command autocompletion
    private static final Map<String, String> autoCompleteMap = new HashMap<>();
    static {
        autoCompleteMap.put("ec", "echo");
        autoCompleteMap.put("ech", "echo");
        autoCompleteMap.put("echo", "echo");
        autoCompleteMap.put("ex", "exit");
        autoCompleteMap.put("exi", "exit");
        autoCompleteMap.put("exit", "exit");
    }

    /*
     * Main entry point for the shell.
     * Handles both interactive and non-interactive modes.
     */
    public static void main(String[] args) throws Exception {
        boolean interactive = (System.console() != null);
        Scanner sc = null;
        
        if (!interactive)
            sc = new Scanner(System.in);
        else
            setTerminalToCharBuffer();

        Set<String> commands = Set.of("echo", "exit", "type", "pwd", "cd");
        boolean running = true;
        Path currentDir = Path.of(System.getProperty("user.dir"));

        while (running) {
            // Print prompt and get input
            System.out.print("$ ");
            System.out.flush();
            
            String input = !interactive ? sc.nextLine() : getInput();

            // Parse command and handle redirections
            List<String> tokens = new ArrayList<>();
            StringBuilder currentToken = new StringBuilder();
            boolean inQuotes = false;
            char quoteChar = 0;
            
            // Parse input into tokens
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                
                // Handle whitespace
                if (Character.isWhitespace(c) && !inQuotes) {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    continue;
                }
                
                // Handle quotes
                if ((c == '\'' || c == '\"') && (i == 0 || input.charAt(i-1) != '\\')) {
                    if (!inQuotes) {
                        inQuotes = true;
                        quoteChar = c;
                    }
                    else if (c == quoteChar) {
                        inQuotes = false;
                        continue;
                    }
                    currentToken.append(c);
                    continue;
                }
                
                if (c == '\\') {
                    if (i + 1 < input.length()) {
                        char next = input.charAt(i + 1);
                        if (next == '\\' || next == '$' || next == '\"' || next == '\'' || next == '\n') {
                            currentToken.append(next);
                            i++;
                            continue;
                        }
                    }
                    currentToken.append(c);
                    continue;
                }
                currentToken.append(c);
            }
            
            if (currentToken.length() > 0)
                tokens.add(currentToken.toString());

            // Handle redirections
            String stdoutFile = null;
            boolean appendStdout = false;
            String stderrFile = null;
            boolean appendStderr = false;
            List<String> newTokens = new ArrayList<>();
            
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (i + 1 >= tokens.size()) {
                    newTokens.add(token);
                    continue;
                }
                
                String nextToken = tokens.get(i + 1);
                switch (token) {
                    case ">", "1>" -> { stdoutFile = nextToken; i++; }
                    case ">>", "1>>" -> { stdoutFile = nextToken; appendStdout = true; i++; }
                    case "2>" -> { stderrFile = nextToken; i++; }
                    case "2>>" -> { stderrFile = nextToken; appendStderr = true; i++; }
                    default -> newTokens.add(token);
                }
            }
            
            tokens = newTokens;
            String[] parts = tokens.toArray(new String[0]);
            String cmd = parts.length > 0 ? parts[0] : "";
            boolean isBuiltIn = commands.contains(cmd);
            
            // Save original streams
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;

            // Handle redirections for built-in commands
            if (isBuiltIn) {
                if (stdoutFile != null)
                    System.setOut(new PrintStream(new FileOutputStream(stdoutFile, appendStdout)));
                if (stderrFile != null)
                    System.setErr(new PrintStream(new FileOutputStream(stderrFile, appendStderr)));
            }

            // Execute command
            String cmdUnquoted = unquoteString(cmd);
            switch (cmd) {
                case "exit" -> running = false;
                
                case "echo" -> {
                    if (parts.length > 1) {
                        String[] echoArgs = Arrays.copyOfRange(parts, 1, parts.length);
                        for (int i = 0; i < echoArgs.length; i++)
                            echoArgs[i] = unquoteString(echoArgs[i]);
                        System.out.println(String.join(" ", echoArgs));
                    }
                }
                
                case "type" -> {
                    if (parts.length > 1) {
                        String term = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        if (commands.contains(term))
                            System.out.printf("%s is a shell builtin%n", term);
                        else {
                            String path = getPath(term);
                            if (path == null)
                                System.out.printf("%s: not found%n", term);
                            else
                                System.out.printf("%s is %s%n", term, path);
                        }
                    }
                }
                
                case "pwd" -> System.out.println(System.getProperty("user.dir"));
                
                case "cd" -> {
                    if (parts.length > 1) {
                        String arg = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        Path newPath;
                        String homeDir = System.getenv("HOME");
                        if (homeDir == null)
                            homeDir = System.getProperty("user.home");
                        
                        if (arg.startsWith("/"))
                            newPath = Path.of(arg);
                        else if (arg.equals("~"))
                            newPath = Path.of(homeDir);
                        else if (arg.startsWith("~/"))
                            newPath = Path.of(homeDir, arg.substring(2));
                        else
                            newPath = currentDir.resolve(arg).normalize();
                        
                        if (Files.isDirectory(newPath)) {
                            currentDir = newPath.toAbsolutePath();
                            System.setProperty("user.dir", currentDir.toString());
                        }
                        else
                            System.out.printf("cd: %s: No such file or directory%n", arg);
                    }
                    else
                        System.out.println("cd: missing argument");
                }
                
                default -> {
                    String path = getPath(cmdUnquoted);
                    if (path == null)
                        System.out.printf("%s: command not found%n", cmd);
                    else {
                        parts[0] = cmdUnquoted;
                        for (int j = 0; j < parts.length; j++)
                            parts[j] = unquoteString(parts[j]);
                        
                        ProcessBuilder pb = new ProcessBuilder(parts);
                        if (stdoutFile != null)
                            pb.redirectOutput(appendStdout 
                                ? ProcessBuilder.Redirect.appendTo(new File(stdoutFile))
                                : ProcessBuilder.Redirect.to(new File(stdoutFile)));
                        if (stderrFile != null)
                            pb.redirectError(appendStderr
                                ? ProcessBuilder.Redirect.appendTo(new File(stderrFile))
                                : ProcessBuilder.Redirect.to(new File(stderrFile)));
                        
                        Process p = pb.start();
                        p.waitFor();
                        
                        if (stdoutFile == null)
                            p.getInputStream().transferTo(System.out);
                        if (stderrFile == null)
                            p.getErrorStream().transferTo(System.err);
                    }
                }
            }

            // Restore original streams
            System.out.flush();
            System.err.flush();
            System.setOut(oldOut);
            System.setErr(oldErr);
        }

        if (!interactive)
            sc.close();
    }

    /*
     * Gets input from the user in interactive mode with support for tab completion.
     */
    private static String getInput() throws IOException {
        StringBuilder input = new StringBuilder();
        int tabCount = 0;
        
        while (true) {
            if (System.in.available() != 0) {
                int key = System.in.read();
                char charKey = (char) key;
                
                if (charKey == 0x09) {  // Tab key
                    tabCount++;
                    String current = input.toString();
                    List<String> matches = getMatches(current.trim());
                    
                    if (matches.isEmpty()) {
                        System.out.print("\007");  // Bell
                        System.out.flush();
                    }
                    else if (matches.size() == 1) {
                        String match = matches.get(0);
                        for (int i = 0; i < input.length(); i++)
                            System.out.print("\b \b");
                        input = new StringBuilder(match + " ");
                        System.out.print(input.toString());
                        tabCount = 0;
                    }
                    else {
                        String lcp = longestCommonPrefix(matches);
                        if (lcp.length() > current.trim().length()) {
                            for (int i = 0; i < input.length(); i++)
                                System.out.print("\b \b");
                            input = new StringBuilder(lcp);
                            System.out.print(input.toString());
                            tabCount = 0;
                        }
                        else {
                            if (tabCount == 1) {
                                System.out.print("\007");
                                System.out.flush();
                            }
                            else {
                                System.out.println();
                                for (int i = 0; i < matches.size(); i++) {
                                    System.out.print(matches.get(i));
                                    if (i < matches.size() - 1)
                                        System.out.print("  ");
                                }
                                System.out.println();
                                System.out.print("$ " + current);
                                System.out.flush();
                                tabCount = 0;
                            }
                        }
                    }
                    continue;
                }
                
                tabCount = 0;
                if (charKey == 0x0A) {  // Enter key
                    System.out.println();
                    break;
                }
                else {
                    input.append(charKey);
                    System.out.print(charKey);
                    System.out.flush();
                }
            }
        }
        return input.toString();
    }

    /*
     * Gets all possible command matches for autocompletion.
     */
    private static List<String> getMatches(String prefix) {
        List<String> results = new ArrayList<>();
        
        // Check built-in commands
        for (String key : autoCompleteMap.keySet())
            if (key.startsWith(prefix)) {
                String comp = autoCompleteMap.get(key);
                if (!results.contains(comp))
                    results.add(comp);
            }
        
        // Check executables in PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null)
            for (String path : pathEnv.split(":")) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null)
                        for (File file : files)
                            if (file.isFile() && file.canExecute() 
                                && file.getName().startsWith(prefix) 
                                && !results.contains(file.getName()))
                                results.add(file.getName());
                }
            }
        
        Collections.sort(results);
        return results;
    }

    /*
     * Finds the longest common prefix among a list of strings.
     */
    private static String longestCommonPrefix(List<String> strs) {
        if (strs == null || strs.isEmpty())
            return "";
        
        String prefix = strs.get(0);
        for (int i = 1; i < strs.size(); i++)
            while (strs.get(i).indexOf(prefix) != 0) {
                prefix = prefix.substring(0, prefix.length() - 1);
                if (prefix.isEmpty())
                    return "";
            }
        return prefix;
    }

    /*
     * Removes quotes from a string if present.
     */
    private static String unquoteString(String str) {
        if ((str.startsWith("\"") && str.endsWith("\"")) ||
            (str.startsWith("'") && str.endsWith("'")))
            return str.substring(1, str.length() - 1);
        return str;
    }

    /*
     * Finds the full path of an executable command.
     */
    private static String getPath(String command) {
        Path cmdPath = Path.of(command);
        if ((cmdPath.isAbsolute() || command.contains("/"))
                && Files.exists(cmdPath)
                && Files.isRegularFile(cmdPath)
                && Files.isExecutable(cmdPath))
            return cmdPath.toString();
        
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null)
            for (String path : pathEnv.split(":")) {
                Path fullPath = Path.of(path, command);
                if (Files.exists(fullPath) && Files.isRegularFile(fullPath))
                    return fullPath.toString();
            }
        
        Path cwdPath = Path.of(System.getProperty("user.dir")).resolve(command);
        if (Files.exists(cwdPath) && Files.isExecutable(cwdPath))
            return cwdPath.toAbsolutePath().toString();
        
        return null;
    }

    /*
     * Sets up the terminal for character-by-character input in interactive mode.
     */
    private static void setTerminalToCharBuffer() throws IOException, InterruptedException {
        stty("-g");
        stty("-icanon min 1");
        stty("-echo");
    }

    /*
     * Executes a stty command to modify terminal settings.
     */
    private static String stty(final String args) throws IOException, InterruptedException {
        String cmd = "stty " + args + " < /dev/tty";
        return exec(new String[] {"sh", "-c", cmd});
    }

    /*
     * Executes a system command and returns its output.
     */
    private static String exec(final String[] cmd) throws IOException, InterruptedException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Process p = Runtime.getRuntime().exec(cmd);
        
        int c;
        InputStream in = p.getInputStream();
        while ((c = in.read()) != -1)
            bout.write(c);
        
        in = p.getErrorStream();
        while ((c = in.read()) != -1)
            bout.write(c);
        
        p.waitFor();
        return new String(bout.toByteArray());
    }
}
