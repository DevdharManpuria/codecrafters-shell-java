import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
@SuppressWarnings("unused")
public class Main {
    public static void main(String[] args) throws Exception {
        Set<String> commands = Set.of("echo", "exit", "type", "pwd", "cd");
        Scanner sc = new Scanner(System.in);
        boolean running = true;
        Path currentDir = Path.of(System.getProperty("user.dir"));
        while (running) {
            System.out.print("$ ");
            String input = sc.nextLine();
            if (input.contains("\t")) {
                int tabIndex = input.indexOf("\t");
                String prefix = input.substring(0, tabIndex);
                String candidate = null;
                if ("echo".startsWith(prefix))
                    candidate = "echo ";
                else if ("exit".startsWith(prefix))
                    candidate = "exit ";
                if (candidate != null) {
                    System.out.print("\r$ " + candidate);
                    input = candidate;
                    continue;
                }
                input = input.replace("\t", "");
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
                    if (appendStdout) {
                        System.setOut(new PrintStream(new FileOutputStream(redirectStdoutFile, true)));
                    } else {
                        System.setOut(new PrintStream(new FileOutputStream(redirectStdoutFile)));
                    }
                }
                if (redirectStderrFile != null) {
                    if (appendStderr) {
                        System.setErr(new PrintStream(new FileOutputStream(redirectStderrFile, true)));
                    } else {
                        System.setErr(new PrintStream(new FileOutputStream(redirectStderrFile)));
                    }
                }
            }
            switch (cmd) {
                case "exit":
                    if (parts.length > 1) {
                        String arg = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        if (arg.equals("0"))
                            running = false;
                    }
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
        sc.close();
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
}
