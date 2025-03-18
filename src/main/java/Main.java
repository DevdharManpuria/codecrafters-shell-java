import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                tokens.add(matcher.group(2));
            } else if (matcher.group(3) != null) {
                tokens.add(matcher.group(3));
            }
        }
        return tokens;
    }

    public static class ShellContext {
        public AtomicBoolean exitCondition = new AtomicBoolean(false);
        public CmdProcessor cmdProc = new CmdProcessor();
        public List<String> path = new ArrayList<>();
        private String cwd;
        public ShellContext() {
            cwd = System.getProperty("user.dir");
        }
        public void exit() {
            exitCondition.set(true);
        }
        public Command findCmd(String name) {
            return cmdProc.find(name);
        }
        public void loadPath(String pathstr) {
            if (pathstr == null) return;
            String[] dirs = pathstr.split(":");
            for (String d : dirs) {
                path.add(d);
            }
        }
        public void clearPath() {
            path.clear();
        }
        public String searchPath(String cmd) {
            for (String dir : path) {
                File f = new File(dir, cmd);
                if (f.exists() && f.canExecute()) {
                    return f.getAbsolutePath();
                }
            }
            return "";
        }
        public void setCwd(String newCwd) {
            if (newCwd == null || newCwd.isEmpty()) {
                cwd = System.getProperty("user.dir");
            } else {
                File dir = new File(newCwd);
                if (dir.exists() && dir.isDirectory()) {
                    cwd = dir.getAbsolutePath();
                } else {
                    System.out.println("cd: " + newCwd + ": No such file or directory");
                }
            }
        }
        public String getCwd() {
            return cwd;
        }
    }

    public interface Command {
        String getName();
        int execute(ShellContext ctx, String[] args);
    }

    public static class CmdProcessor {
        private List<Command> commands = new ArrayList<>();
        public int add(Command cmd) {
            commands.add(cmd);
            return commands.size() - 1;
        }
        public Command find(String name) {
            for (Command cmd : commands) {
                if (cmd.getName().equals(name))
                    return cmd;
            }
            return null;
        }
        public int process(ShellContext ctx, String[] args) {
            if (args.length == 0) return 0;
            Command cmd = find(args[0]);
            if (cmd != null) {
                return cmd.execute(ctx, args);
            }
            try {
                ProcessBuilder pb = new ProcessBuilder(args);
                pb.directory(new File(ctx.getCwd()));
                pb.inheritIO();
                Process p = pb.start();
                return p.waitFor();
            } catch (IOException | InterruptedException e) {
                System.out.println(args[0] + ": command not found");
                return -1;
            }
        }
    }

    public static class exit implements Command {
        public String getName() {
            return "exit";
        }
        public int execute(ShellContext ctx, String[] args) {
            int ret = 0;
            System.out.println("exit");
            if (args.length > 2) {
                System.out.println("shell: exit: too many arguments");
                return 1;
            }
            if (args.length == 2) {
                try {
                    ret = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.out.println("shell: exit: " + args[1] + ": numeric argument required");
                    ret = 2;
                }
            }
            ctx.exit();
            return ret;
        }
    }

    public static class echo implements Command {
        public String getName() {
            return "echo";
        }
        public int execute(ShellContext ctx, String[] args) {
            for (int i = 1; i < args.length; i++) {
                System.out.print(args[i]);
                if (i < args.length - 1)
                    System.out.print(" ");
            }
            System.out.println();
            return 0;
        }
    }

    public static class type implements Command {
        public String getName() {
            return "type";
        }
        public int execute(ShellContext ctx, String[] args) {
            if (args.length == 1)
                return 0;
            for (int i = 1; i < args.length; i++) {
                Command cmd = ctx.findCmd(args[i]);
                if (cmd != null) {
                    System.out.println(args[i] + " is a shell builtin");
                } else {
                    String cmdPath = ctx.searchPath(args[i]);
                    if (!cmdPath.isEmpty()) {
                        System.out.println(args[i] + " is " + cmdPath);
                    } else {
                        System.out.println(args[i] + " not found");
                    }
                }
            }
            return 1;
        }
    }

    public static class pwd implements Command {
        public String getName() {
            return "pwd";
        }
        public int execute(ShellContext ctx, String[] args) {
            System.out.println(ctx.getCwd());
            return 0;
        }
    }

    public static class cd implements Command {
        public String getName() {
            return "cd";
        }
        public int execute(ShellContext ctx, String[] args) {
            if (args.length == 1) {
                String home = System.getenv("HOME");
                if (home == null)
                    home = System.getProperty("user.home");
                ctx.setCwd(home);
            } else {
                String newCwd = args[1];
                if (newCwd.startsWith("~")) {
                    String home = System.getenv("HOME");
                    if (home == null)
                        home = System.getProperty("user.home");
                    newCwd = home + newCwd.substring(1);
                }
                ctx.setCwd(newCwd);
            }
            return 0;
        }
    }

    public static void main(String[] args) throws Exception {
        ShellContext ctx = new ShellContext();
        ctx.cmdProc.add(new exit());
        ctx.cmdProc.add(new echo());
        ctx.cmdProc.add(new type());
        ctx.cmdProc.add(new pwd());
        ctx.cmdProc.add(new cd());
        String pathStr = System.getenv("PATH");
        ctx.loadPath(pathStr);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (!ctx.exitCondition.get()) {
            System.out.print("$ ");
            String line = reader.readLine();
            if (line == null || line.trim().isEmpty())
                continue;
            List<String> tokens = tokenize(line);
            String[] argv = tokens.toArray(new String[0]);
            ctx.cmdProc.process(ctx, argv);
        }
    }
}
