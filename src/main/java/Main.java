import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
@SuppressWarnings("unused")
public class Main {
    static String paths[] = null;
    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        paths = System.getenv("PATH").split(":");

        // Uncomment this block to pass the first stage
        System.out.print("$ "); 
        
        Scanner scanner = new Scanner(System.in);
        // List<String> inputs = new ArrayList<>();
        while(true){
            String inputs = scanner.nextLine();
            String input[] = inputs.split(" ");
            // inputs.add(scanner.nextLine());
            if(containsRedirect(input)) {
                redirectingSTDOut(inputs);
                System.out.print("$ "); 
                continue;
            }

            switch (input[0].toLowerCase()) {

                case "cd":
                    handleCD(inputs);
                    break;

                case "pwd":
                    String currentPath = System.getProperty("user.dir");
                    System.out.println(currentPath);
                    break;

                case "type":
                    handleType(input);
                    break;
                
                case "exit":
                    return;
                    // break;

                case "echo":
                    System.out.println(inputs.substring(inputs.indexOf(" ")+1));
                    // System.out.println();
                    // for(int i = 1 ; i < input.length ; i++) {
                    //     System.out.print(input[i] +" ");
                    // }
                    // System.out.println();
                    break;
                default:
                    boolean flag = false;
                    for(String path : paths) {
                        Path fullPath = Path.of(path , input[0]);
                        if(Files.isRegularFile(fullPath)) {
                            Process p = Runtime.getRuntime().exec((fullPath+ inputs.substring(input[0].length())).toString().split(" "));
                            p.getInputStream().transferTo(System.out);
                            flag = true;
                            break;
                        }
                    }
                    if(!flag)
                    System.out.println(inputs+": command not found");
                    break;
            }
            
            System.out.print("$ "); 
        }

        // for(String input : inputs) {
        //     System.out.println(input+": command not found");
        // }
        
    }
    public static boolean containsRedirect(String input[]) {
        for(String s : input){
            if(s.endsWith(">")) return true;
        }
        return false;
    }
    public static boolean isRealative(String directories[]) {
        for(String next : directories) {
            if(next.equals(".") || next.equals("..") || next.equals("~")) return true;
        }
        return false;

    }

    public static void handleCD(String input){
        input = input.substring(3);
        String directories[] = input.split("/");
        if(isRealative(directories)) {
            for(String directory : directories) {
                String currentPath = System.getProperty("user.dir");
                if(directory.equals("..")) {
                    int index = currentPath.lastIndexOf("/");
                    currentPath = currentPath.substring(0, index);
                    System.setProperty("user.dir", currentPath);
                }
                else if(directory.equals(".")) continue;

                else if(directory.equals("~")) {
                    String homePath = System.getenv("HOME");
                    System.setProperty("user.dir", homePath);
                }

                else{
                    String newPath = currentPath +"/" + directory;
                    Path path = Path.of(newPath);
                    if(!Files.exists(path)){
                        System.out.println("cd:" + input + ": No such file or directory");
                        return;
                    }
                    System.setProperty("user.dir", path.toString());

                }
            }
            return;
        }
        String currentPath = System.getProperty("user.dir");
        // String newPath =  input.substring(3);
        Path changedPath = Path.of(input);
        if(!Files.exists(changedPath)){
            System.out.println("cd: " + input +": No such file or directory");
            return;
        }
        System.setProperty("user.dir" , input);
    }
    public static boolean doesFileExist(Path path) {
        if(!Files.exists(path)){
            System.out.println("cd: " + path +": No such file or directory");
            return false;
        }
        return true;
    }
    public static List<String> parseCommand(String command) {
        List<String> result = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;

        for (char c : command.toCharArray()) {
            if (c == '\'') {
                inQuotes = !inQuotes; // Toggle inQuotes flag
            } else if (c == ' ' && !inQuotes) {
                if (currentArg.length() > 0) {
                    result.add(currentArg.toString());
                    currentArg.setLength(0);
                }
            } else {
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0) {
            result.add(currentArg.toString());
        }

        return result;

    }
    public static void redirectingSTDOut(String input) {
        String[] commands = input.split("\\s*(1?>)\\s*");
        String commandParts = commands[0].trim();
        String outputFile = commands[1].trim();
        List<String> commandargs = parseCommand(commandParts);
        ProcessBuilder processbuilder = new ProcessBuilder(commandargs);
        if(outputFile != null) {
            processbuilder.redirectOutput(new File(outputFile));
        }
        else {
            processbuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        processbuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        // Start the process
        try{
            Process process = processbuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // System.err.println("Command failed with exit code " + exitCode);
            }
        }catch (Exception e) {
            // System.err.println("Error: " + e.getMessage());
        }
    }
    public static void handleType(String[] inputs) {

        List<String> builtins = new ArrayList<>();
            builtins.add("echo");
            builtins.add("exit");
            builtins.add("type");
            builtins.add("pwd");

        if(builtins.contains(inputs[1])){
            System.out.println(inputs[1] +" is a shell builtin");
            return;
        }
        for(String path : paths) {
            Path fullPath = Path.of(path , inputs[1]);
            if(Files.isRegularFile(fullPath)){
                System.out.println(inputs[1] +" is "+fullPath);
                return;
            }                
        }
        System.out.println(inputs[1] + ": not found");
    }
}