package pl.edu.agh.sr.zookeeper.utilities;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.*;

import static pl.edu.agh.sr.zookeeper.Client.*;

public class Executor implements Watcher, Runnable, DataMonitor.DataMonitorListener {
    private ZooKeeper zooKeeper;
    private DataMonitor dataMonitor;
    private TreePrinter treePrinter;
    private String[] exec;

    private Process process;
    private StreamWriter streamWriter;

    public Executor(String hostPort, String[] exec) throws IOException {
        this.zooKeeper = new ZooKeeper(hostPort, TIMEOUT, this);
        this.treePrinter = new TreePrinter(zooKeeper);

        this.exec = exec;

        this.process = null;
    }

    private static class StreamWriter extends Thread {
        private BufferedReader input;

        private StreamWriter(InputStream inputStream) {
            this.input = new BufferedReader(
                    new InputStreamReader(inputStream)
            );
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String line = input.readLine();
                    if (line != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        dataMonitor = new DataMonitor(zooKeeper, this);
        synchronized (this) {
            while(dataMonitor.isAlive()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        dataMonitor.process(watchedEvent);
    }

    @Override
    public void onZnodeCreated() {
        if (process == null) {
            System.out.println("Starting process...");
            try {
                process = Runtime.getRuntime().exec(exec);
                streamWriter = new StreamWriter(process.getInputStream());
                streamWriter.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onZnodeDeleted() {
        if (process != null) {
            System.out.println("Stopping process...");
            process.destroy();
            process = null;
        }
    }

    @Override
    public void closing(int rc) {
        synchronized (this) {
            notifyAll();
        }
    }

    public TreePrinter getTreePrinter() {
        return treePrinter;
    }

}
