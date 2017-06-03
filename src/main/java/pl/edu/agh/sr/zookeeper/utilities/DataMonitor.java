package pl.edu.agh.sr.zookeeper.utilities;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Arrays;

import static pl.edu.agh.sr.zookeeper.Client.*;

public class DataMonitor implements Watcher, AsyncCallback.StatCallback {
    private ZooKeeper zooKeeper;
    private DataMonitorListener listener;

    private boolean alive;
    private boolean isRunning;

    DataMonitor(ZooKeeper zooKeeper, DataMonitorListener listener) {
        this.zooKeeper = zooKeeper;
        this.listener = listener;

        this.alive = true;
        this.isRunning = false;

        zooKeeper.exists("/" + ZNODE, true, this, null);
        setWatchers("/");
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        if (rc == KeeperException.Code.OK.intValue()) {
            try {
                isRunning = true;
                listener.onZnodeCreated();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (rc == KeeperException.Code.SESSIONEXPIRED.intValue() || rc == KeeperException.Code.NOAUTH.intValue()) {
            alive = false;
            listener.closing(rc);
        }
        else if (rc != KeeperException.Code.NONODE.intValue()){
            zooKeeper.exists(path, true, this, null);
        }
    }

    private void setWatchers(String path) {
        try {
            zooKeeper.getChildren(path, true, null)
                    .forEach(child -> {
                        String childPath = (path + "/" + child).replace("//", "/");
                        zooKeeper.exists(childPath + "/" + ZNODE, true, this, null);
                        setWatchers(childPath);
                    });
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    interface DataMonitorListener {
        void onZnodeCreated();
        void onZnodeDeleted();
        void closing(int rc);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        String path = watchedEvent.getPath();
        if (path != null) {
            if (watchedEvent.getType() == Event.EventType.NodeCreated) {
                zooKeeper.exists(path, true, null, null);
                if (isRunning) {
                    System.out.printf("%s znode has been already created!\n", ZNODE);
                }
                else {
                    isRunning = true;
                    listener.onZnodeCreated();
                }
            }
            else if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
                zooKeeper.exists(path, true, null, null);
                isRunning = false;
                listener.onZnodeDeleted();
                setChildren(0);
            }
            else if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                if (path.endsWith(ZNODE)) {
                    try {
                        int children = zooKeeper.getChildren(path, true, null).size();
                        if (children > getChildren()) {
                            System.out.printf("Current number of %s znode children: %d\n", ZNODE, children);
                        }
                        setChildren(children);
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    setWatchers(path);
                }
            }
        }
    }

    boolean isAlive() {
        return alive;
    }

}
