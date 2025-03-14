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
            String[] parts = input.split(" ", 2);
            String cmd = parts[0];
            String arg = parts.length > 1 ? parts[1] : "";
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
                      if (arg.startsWith("/")) {
                        newPath = Path.of(arg);
                      }
                      else if (arg.equals("~")) {
                        newPath = Path.of(System.getProperty("user.home"));
                      } 
                      else if (arg.startsWith("~/")) {
                        newPath = Path.of(System.getProperty("user.home"), arg.substring(2));
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