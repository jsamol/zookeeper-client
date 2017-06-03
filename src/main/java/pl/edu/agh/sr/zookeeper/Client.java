package pl.edu.agh.sr.zookeeper;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import pl.edu.agh.sr.zookeeper.utilities.Executor;
import pl.edu.agh.sr.zookeeper.utilities.TreePrinter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {
    public static final int TIMEOUT = 1000;
    public static final String ZNODE = "znode_testowy";

    private Executor executor;

    private static int children = 0;

    private Client(Executor executor) {
        this.executor = executor;
    }

    private void start() {
        new Thread(executor).start();
        TreePrinter treePrinter = executor.getTreePrinter();

        BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in)
        );

        while(true) {
            try {
                printHelp();
                String command = input.readLine();
                if ("quit".equals(command)) {
                    System.exit(0);
                }
                else if ("print_tree".equals(command)) {
                    treePrinter.print();
                }
                else if ("help".equals(command)) {
                    printHelp();
                }
                else {
                    System.out.println("Invalid command! Type 'help' to display all available commands.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("More arguments expected!");
            System.exit(1);
        }
        Logger.getRootLogger().setLevel(Level.OFF);
        String hostPort = args[0];
        String exec[] = new String[args.length - 1];
        System.arraycopy(args, 1, exec, 0, exec.length);
        Executor executor = null;
        try {
            executor = new Executor(hostPort, exec);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        Client client = new Client(executor);
        client.start();
    }

    private void printHelp() {
        System.out.printf(
                "Available commands:\n" +
                "print_tree\t-> prints the %s znode tree \n" +
                "quit\t\t-> quit application\n\n", ZNODE
        );
        System.out.print("> ");
        System.out.flush();
    }

    public static int getChildren() {
        return children;
    }

    public static void setChildren(int children) {
        Client.children = children;
    }
}
