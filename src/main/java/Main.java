import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("unused")
public class Main {
    private static final Map<String, String> autoCompleteMap = new HashMap<>();
    static {
        autoCompleteMap.put("ec", "echo");
        autoCompleteMap.put("ech", "echo");
        autoCompleteMap.put("echo", "echo");
        autoCompleteMap.put("ex", "exit");
        autoCompleteMap.put("exi", "exit");
        autoCompleteMap.put("exit", "exit");
    }
    
    public static void main(String[] args) throws Exception {
        boolean interactive = (System.console() != null);
        Scanner sc = null;
        if (!interactive) {
            sc = new Scanner(System.in);
        } else {
            setTerminalToCharBuffer();
        }
        Set<String> commands = Set.of("echo", "exit", "type", "pwd", "cd");
        boolean running = true;
        Path currentDir = Path.of(System.getProperty("user.dir"));
        while (running) {
            System.out.print("$ ");
            System.out.flush();
            String input;
            if (!interactive) {
                input = sc.nextLine();
            } else {
                input = getInput();
            }
            List<String> tokens = new ArrayList<>();
            String commandString = "";
            int i = 0;
            StringBuilder sb = new StringBuilder();
            boolean lastQuoted = false;
            while (i < input.length()) {
                while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
                    i++;
                    lastQuoted = false;
                }
                if (i >= input.length())
                    break;
                if (commandString.isEmpty()) {
                    if (input.charAt(i) == '\'' || input.charAt(i) == '\"') {
                        char quote = input.charAt(i);
                        i++;
                        while (i < input.length() && input.charAt(i) != quote) {
                            if (quote == '\"' && input.charAt(i) == '\\' && i + 1 < input.length()) {
                                char next = input.charAt(i + 1);
                                if (next == '\\' || next == '$' || next == '\"' || input.charAt(i + 1) == '\n') {
                                    sb.append(next);
                                    i += 2;
                                    continue;
                                } else {
                                    sb.append('\\');
                                    i++;
                                    continue;
                                }
                            } else {
                                sb.append(input.charAt(i));
                                i++;
                            }
                        }
                        i++;
                        commandString = sb.toString();
                        tokens.add(commandString);
                        sb.setLength(0);
                        lastQuoted = true;
                        continue;
                    } else {
                        while (i < input.length() && !Character.isWhitespace(input.charAt(i))) {
                            if (input.charAt(i) == '\\') {
                                i++;
                                if (i < input.length()) {
                                    sb.append(input.charAt(i));
                                    i++;
                                } else {
                                    sb.append('\\');
                                }
                            } else {
                                sb.append(input.charAt(i));
                                i++;
                            }
                        }
                        commandString = sb.toString();
                        tokens.add(commandString);
                        sb.setLength(0);
                        lastQuoted = false;
                        continue;
                    }
                }
                if (input.charAt(i) == '\'') {
                    i++;
                    while (i < input.length() && input.charAt(i) != '\'') {
                        sb.append(input.charAt(i));
                        i++;
                    }
                    i++;
                    if (lastQuoted && !tokens.isEmpty()) {
                        int lastIndex = tokens.size() - 1;
                        tokens.set(lastIndex, tokens.get(lastIndex) + sb.toString());
                    } else {
                        tokens.add(sb.toString());
                    }
                    sb.setLength(0);
                    lastQuoted = true;
                    continue;
                } else if (input.charAt(i) == '\"') {
                    i++;
                    while (i < input.length() && input.charAt(i) != '\"') {
                        if (input.charAt(i) == '\\' && i + 1 < input.length()) {
                            char next = input.charAt(i + 1);
                            if (next == '\\' || next == '$' || next == '\"' || input.charAt(i + 1) == '\n') {
                                sb.append(next);
                                i += 2;
                                continue;
                            } else {
                                sb.append('\\');
                                i++;
                                continue;
                            }
                        } else {
                            sb.append(input.charAt(i));
                            i++;
                        }
                    }
                    i++;
                    if (lastQuoted && !tokens.isEmpty()) {
                        int lastIndex = tokens.size() - 1;
                        tokens.set(lastIndex, tokens.get(lastIndex) + sb.toString());
                    } else {
                        tokens.add(sb.toString());
                    }
                    sb.setLength(0);
                    lastQuoted = true;
                    continue;
                }
                while (i < input.length() && !Character.isWhitespace(input.charAt(i))) {
                    if (input.charAt(i) == '\\') {
                        i++;
                        if (i < input.length()) {
                            sb.append(input.charAt(i));
                            i++;
                        } else {
                            sb.append('\\');
                        }
                    } else {
                        sb.append(input.charAt(i));
                        i++;
                    }
                }
                if (lastQuoted && !tokens.isEmpty()) {
                    int lastIndex = tokens.size() - 1;
                    tokens.set(lastIndex, tokens.get(lastIndex) + sb.toString());
                } else {
                    tokens.add(sb.toString());
                }
                sb.setLength(0);
                lastQuoted = true;
            }
            String redirectStdoutFile = null;
            boolean appendStdout = false;
            String redirectStderrFile = null;
            boolean appendStderr = false;
            List<String> newTokens = new ArrayList<>();
            for (int j = 0; j < tokens.size(); j++) {
                String token = tokens.get(j);
                if (token.equals(">") || token.equals("1>")) {
                    if (j + 1 < tokens.size()) {
                        redirectStdoutFile = tokens.get(j + 1);
                        j++;
                    }
                } else if (token.equals(">>") || token.equals("1>>")) {
                    if (j + 1 < tokens.size()) {
                        redirectStdoutFile = tokens.get(j + 1);
                        appendStdout = true;
                        j++;
                    }
                } else if (token.equals("2>>")) {
                    if (j + 1 < tokens.size()) {
                        redirectStderrFile = tokens.get(j + 1);
                        appendStderr = true;
                        j++;
                    }
                } else if (token.equals("2>")) {
                    if (j + 1 < tokens.size()) {
                        redirectStderrFile = tokens.get(j + 1);
                        j++;
                    }
                } else {
                    newTokens.add(token);
                }
            }
            tokens = newTokens;
            String[] parts = tokens.toArray(new String[0]);
            String cmd = parts[0];
            boolean isBuiltIn = commands.contains(cmd);
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            if (isBuiltIn) {
                if (redirectStdoutFile != null) {
                    if (appendStdout)
                        System.setOut(new PrintStream(new FileOutputStream(redirectStdoutFile, true)));
                    else
                        System.setOut(new PrintStream(new FileOutputStream(redirectStdoutFile)));
                }
                if (redirectStderrFile != null) {
                    if (appendStderr)
                        System.setErr(new PrintStream(new FileOutputStream(redirectStderrFile, true)));
                    else
                        System.setErr(new PrintStream(new FileOutputStream(redirectStderrFile)));
                }
            }
            switch (cmd) {
                case "exit":
                    running = false;
                    break;
                case "echo":
                    if (parts.length > 1) {
                        String arg = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        System.out.println(arg);
                    }
                    break;
                case "type":
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
                    break;
                case "pwd":
                    System.out.println(System.getProperty("user.dir"));
                    break;
                case "cd":
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
                        } else
                            System.out.printf("cd: %s: No such file or directory%n", arg);
                    } else
                        System.out.println("cd: missing argument");
                    break;
                default:
                    String cmdUnquoted = cmd;
                    if ((cmd.startsWith("\"") && cmd.endsWith("\"")) ||
                        (cmd.startsWith("'") && cmd.endsWith("'")))
                        cmdUnquoted = cmd.substring(1, cmd.length() - 1);
                    String path = getPath(cmdUnquoted);
                    if (path == null)
                        System.out.printf("%s: command not found%n", cmd);
                    else {
                        parts[0] = cmdUnquoted;
                        for (int j = 0; j < parts.length; j++) {
                            if ((parts[j].startsWith("\"") && parts[j].endsWith("\"")) ||
                                (parts[j].startsWith("'") && parts[j].endsWith("'")))
                                parts[j] = parts[j].substring(1, parts[j].length() - 1);
                        }
                        ProcessBuilder pb = new ProcessBuilder(parts);
                        if (redirectStdoutFile != null) {
                            if (appendStdout)
                                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(redirectStdoutFile)));
                            else
                                pb.redirectOutput(new File(redirectStdoutFile));
                        }
                        if (redirectStderrFile != null) {
                            if (appendStderr)
                                pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(redirectStderrFile)));
                            else
                                pb.redirectError(new File(redirectStderrFile));
                        }
                        Process p = pb.start();
                        p.waitFor();
                        if (redirectStdoutFile == null)
                            p.getInputStream().transferTo(System.out);
                        if (redirectStderrFile == null)
                            p.getErrorStream().transferTo(System.err);
                    }
                    break;
            }
            System.out.flush();
            System.err.flush();
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
        if (!interactive) {
            sc.close();
        }
    }
    
    private static String getInput() throws IOException {
        StringBuilder input = new StringBuilder();
        int tabCount = 0;
        while (true) {
            if (System.in.available() != 0) {
                int key = System.in.read();
                char charKey = (char) key;
                if (charKey == 0x09) {
                    tabCount++;
                    String current = input.toString();
                    List<String> matches = getMatches(current.trim());
                    if (matches.isEmpty()) {
                        System.out.print("\007");
                        System.out.flush();
                    } else if (matches.size() == 1) {
                        String match = matches.get(0);
                        for (int i = 0; i < input.length(); i++) {
                            System.out.print("\b \b");
                        }
                        input = new StringBuilder(match + " ");
                        System.out.print(input.toString());
                        tabCount = 0;
                    } else {
                        String lcp = longestCommonPrefix(matches);
                        if (lcp.length() > current.trim().length()) {
                            for (int i = 0; i < input.length(); i++) {
                                System.out.print("\b \b");
                            }
                            input = new StringBuilder(lcp);
                            System.out.print(input.toString());
                            tabCount = 0;
                        } else {
                            if (tabCount == 1) {
                                System.out.print("\007");
                                System.out.flush();
                            } else {
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
                if (charKey == 0x0A) {
                    System.out.println();
                    break;
                } else {
                    input.append(charKey);
                    System.out.print(charKey);
                    System.out.flush();
                }
            }
        }
        return input.toString();
    }
    
    private static List<String> getMatches(String prefix) {
        List<String> results = new ArrayList<>();
        for (String key : autoCompleteMap.keySet()) {
            if (key.startsWith(prefix)) {
                String comp = autoCompleteMap.get(key);
                if (!results.contains(comp))
                    results.add(comp);
            }
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] paths = pathEnv.split(":");
            for (String p : paths) {
                File dir = new File(p);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.canExecute() && file.getName().startsWith(prefix) && !results.contains(file.getName()))
                                results.add(file.getName());
                        }
                    }
                }
            }
        }
        Collections.sort(results);
        return results;
    }
    
    private static String longestCommonPrefix(List<String> strs) {
        if (strs == null || strs.isEmpty()) return "";
        String prefix = strs.get(0);
        for (int i = 1; i < strs.size(); i++) {
            while (strs.get(i).indexOf(prefix) != 0) {
                prefix = prefix.substring(0, prefix.length() - 1);
                if (prefix.isEmpty()) return "";
            }
        }
        return prefix;
    }
    
    public static String autocomplete(String input) {
        List<String> matches = getMatches(input);
        if (matches.size() == 1)
            return matches.get(0) + " ";
        String lcp = longestCommonPrefix(matches);
        return lcp;
    }
    
    private static String getPath(String command) {
        Path cmdPath = Path.of(command);
        if ((cmdPath.isAbsolute() || command.contains("/"))
                && Files.exists(cmdPath)
                && Files.isRegularFile(cmdPath)
                && Files.isExecutable(cmdPath))
            return cmdPath.toString();
        for (String path : System.getenv("PATH").split(":")) {
            Path fullPath = Path.of(path, command);
            if (Files.exists(fullPath) && Files.isRegularFile(fullPath))
                return fullPath.toString();
        }
        Path cwdPath = Path.of(System.getProperty("user.dir")).resolve(command);
        if (Files.exists(cwdPath) && Files.isExecutable(cwdPath))
            return cwdPath.toAbsolutePath().toString();
        return null;
    }
    
    private static void setTerminalToCharBuffer() throws IOException, InterruptedException {
        stty("-g");
        stty("-icanon min 1");
        stty("-echo");
    }
    
    private static String stty(final String args) throws IOException, InterruptedException {
        String cmd = "stty " + args + " < /dev/tty";
        return exec(new String[] {"sh", "-c", cmd});
    }
    
    private static String exec(final String[] cmd) throws IOException, InterruptedException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Process p = Runtime.getRuntime().exec(cmd);
        int c;
        InputStream in = p.getInputStream();
        while ((c = in.read()) != -1) {
            bout.write(c);
        }
        in = p.getErrorStream();
        while ((c = in.read()) != -1) {
            bout.write(c);
        }
        p.waitFor();
        return new String(bout.toByteArray());
    }
}
