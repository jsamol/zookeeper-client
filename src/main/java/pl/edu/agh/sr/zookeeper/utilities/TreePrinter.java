package pl.edu.agh.sr.zookeeper.utilities;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import static pl.edu.agh.sr.zookeeper.Client.*;

public class TreePrinter {
    private ZooKeeper zooKeeper;
    private String znodePath;

    TreePrinter(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;

        this.znodePath = null;
    }

    public void print() {
        getPath("/");
        if (znodePath != null) {
            print(znodePath, 0);
        }
        else {
            System.out.println(ZNODE + " znode does not exist!");
        }
    }

    private void print(final String path, int level) {
        System.out.println(path);
        try {
            zooKeeper.getChildren(path, false)
                    .forEach(child -> print(path + "/" + child, level + 1));
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    private void getPath(String path) {
        try {
            zooKeeper.getChildren(path, false)
                    .forEach(child -> {
                        if (ZNODE.equals(child)) {
                            if ("/".equals(path)) {
                                znodePath = path + child;
                            }
                            else {
                                znodePath = path + "/" + child;
                            }
                        }
                        else {
                            if ("/".equals(path)) {
                                getPath(path + child);
                            }
                            else {
                                getPath(path + "/" + child);
                            }
                        }
                    });
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
