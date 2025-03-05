import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
public class Main {
  public static void main(String[] args) throws Exception {
    Scanner scanner = new Scanner(System.in);
    List<String> builtins = builtins();
    while (true) {
      System.out.print("$ ");
      String input = scanner.nextLine();
      String[] str = input.split(" ");
      String command = str[0];
      String parameter = "";
      if (str.length > 2) {
        for (int i = 1; i < str.length; i++) {
          if (i < str.length - 1) {
            parameter += str[i] + " ";
          } else {
            parameter += str[i];
          }
        }
      } else if (str.length > 1) {
        parameter = str[1];
      }
      switch (command) {
      case "exit":
        if (parameter.equals("0")) {
          System.exit(0);
        } else {
          System.out.println(input + ": command not found");
        }
        break;
      case "echo":
        System.out.println(parameter);
        break;
      case "type":
        if (parameter.equals(builtins.get(0)) ||
            parameter.equals(builtins.get(1)) ||
            parameter.equals(builtins.get(2))) {
          System.out.println(parameter + " is a shell builtin");
        } else {
          String path = getPath(parameter);
          if (path != null) {
            System.out.println(parameter + " is " + path);
          } else {
            System.out.println(parameter + ": not found");
          }
        }
        break;
      default:
        System.out.println(input + ": command not found");
      }
    }
  }
  private static String getPath(String parameter) {
    for (String path : System.getenv("PATH").split(":")) {
      Path fullPath = Path.of(path, parameter);
      if (Files.isRegularFile(fullPath)) {
        return fullPath.toString();
      }
    }
    return null;
  }
  private static List<String> builtins() {
    List<String> builtins = new ArrayList<>();
    builtins.add("exit");
    builtins.add("echo");
    builtins.add("type");
    return builtins;
  }
}