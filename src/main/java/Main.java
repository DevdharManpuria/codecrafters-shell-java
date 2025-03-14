import java.nio.file.*;
import java.util.*;
public class Main {
    public static void main(String[] args) throws Exception {
        Set<String> commands = Set.of("echo", "exit", "type", "pwd");
        Scanner sc = new Scanner(System.in);
        boolean running = true;
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
        for (String path : System.getenv("PATH").split(":")) {
            Path fullPath = Path.of(path, command);
            if (Files.isRegularFile(fullPath) && Files.isExecutable(fullPath)) {
                return fullPath.toString();
            }
        }
        return null;
    }
}