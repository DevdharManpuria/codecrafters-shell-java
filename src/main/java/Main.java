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
            List<String> tokens = new ArrayList<>();
            String commandString = "";
            int i = 0;
            StringBuilder sb = new StringBuilder();
            while (i < input.length()) {
                while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
                    i++;
                }
                if (i >= input.length()) break;
                if (commandString.isEmpty()) {
                    while (i < input.length() && !Character.isWhitespace(input.charAt(i))) {
                        sb.append(input.charAt(i));
                        i++;
                    }
                    commandString = sb.toString();
                    tokens.add(commandString);
                    sb.setLength(0);
                    continue;
                }
                if (input.charAt(i) == '\'') {
                    i++; 
                    while (i < input.length() && input.charAt(i) != '\'') {
                        sb.append(input.charAt(i));
                        i++;
                    }
                    i++;
                    tokens.add(sb.toString());
                    sb.setLength(0);
                    continue;
                }
                while (i < input.length() && !Character.isWhitespace(input.charAt(i))) {
                    sb.append(input.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
                sb.setLength(0);
            }
            String[] parts = tokens.toArray(new String[0]);
            String cmd = parts[0];
            String arg = tokens.size() > 1 ? String.join(" ", tokens.subList(1, tokens.size())) : "";
            switch (cmd) {
                case "exit":
                    if (arg.equals("0")) {
                        running = false;
                    }
                    break;
                case "echo":
                    System.out.println(arg);
                    break;
                case "type":
                    if (commands.contains(arg)) {
                        System.out.printf("%s is a shell builtin%n", arg);
                    } else {
                        String path = getPath(arg);
                        if (path == null) {
                            System.out.printf("%s: not found%n", arg);
                        } else {
                            System.out.printf("%s is %s%n", arg, path);
                        }
                    }
                    break;
                case "pwd":
                    System.out.println(System.getProperty("user.dir"));
                    break;
                case "cd":
                    if (!arg.isEmpty()) { 
                      Path newPath;
                      String homeDir = System.getenv("HOME");
                      if (homeDir == null) {
                        homeDir = System.getProperty("user.home");
                      }
                      if (arg.startsWith("/")) {
                        newPath = Path.of(arg);
                      }
                      else if (arg.equals("~")) {
                        newPath = Path.of(homeDir);
                      } 
                      else if (arg.startsWith("~/")) {
                        newPath = Path.of(homeDir, arg.substring(2));
                      }
                      else {
                        newPath = currentDir.resolve(arg).normalize();
                      }
                      if (Files.isDirectory(newPath)) {
                        currentDir = newPath.toAbsolutePath();
                        System.setProperty("user.dir", currentDir.toString());
                      } 
                      else {
                      System.out.printf("cd: %s: No such file or directory%n", arg);
                      }
                    } 
                    else {
                      System.out.println("cd: missing argument");
                    }
                    break;
                default:
                    String path = getPath(cmd);
                    if (path == null) {
                        System.out.printf("%s: command not found%n", cmd);
                    } else {
                        for (int j = 0; j < parts.length; j++) {
                            parts[j] = parts[j].replaceAll("^'(.*)'$", "$1");
                        }
                        Process p = new ProcessBuilder(parts).start();
                        p.getInputStream().transferTo(System.out);
                    }
                    break;
            }
        }
        sc.close();
    }
    private static String getPath(String command) {
      Path cmdPath = Path.of(command);
      if ((cmdPath.isAbsolute() || command.contains("/")) && Files.isRegularFile(cmdPath) && Files.isExecutable(cmdPath)) {
          return cmdPath.toString();
      }
      for (String path : System.getenv("PATH").split(":")) {
          Path fullPath = Path.of(path, command);
          if (Files.isRegularFile(fullPath) && Files.isExecutable(fullPath)) {
              return fullPath.toString();
          }
      }
      return null;
  }
}