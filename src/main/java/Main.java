import java.util.*;
public class Main {
    public static void main(String[] args) throws Exception {
      System.out.print("$ ");
      Scanner scanner = new Scanner(System.in);
      String input = scanner.nextLine();
      do {
        System.out.println(input + ": command not found");
        System.out.print("$ ");
        input = scanner.nextLine();
        if(input.equals("exit 0")){
            System.out.println("exit 0");
            break;
        }
      } while (true);
    }
  }