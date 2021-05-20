package com.twa.flights.api.clusters.service;

import com.twa.flights.api.clusters.configuration.zookeeper.ZooKeeperCuratorConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.barriers.DistributedBarrier;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class ZooKeeperService {
    private static final Logger log = LoggerFactory.getLogger(ZooKeeperService.class);
    private static final int MAX_WAITING_TIME = 10000;

    private final ZooKeeperCuratorConfiguration curatorConfiguration;
    private final CuratorFramework zkClient;

    public ZooKeeperService(ZooKeeperCuratorConfiguration curatorConfiguration) {
        this.curatorConfiguration = curatorConfiguration;
        this.zkClient = curatorConfiguration.getClient();
    }

    private DistributedBarrier getBarrier(String barrierPath) {
        return new DistributedBarrier(zkClient, barrierPath) {
            @Override
            public synchronized void setBarrier() throws Exception {
                try {
                    zkClient.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL)
                            .forPath(barrierPath);
                } catch (KeeperException.NodeExistsException nodeExistsException) {
                    log.error("I'm {}: Node exists exception for barrierPath {}", getHostName(), barrierPath);
                    throw nodeExistsException;
                }
            }
        };
    }

    public boolean barrierExists(String barrierPath) {
        Stat stat;
        try {
            stat = zkClient.checkExists().forPath(barrierPath);
        } catch (Exception e) {
            log.error("I'm {}: There was an error checking if barrier {} exists", getHostName(), barrierPath, e);
            stat = null;
        }
        return Objects.nonNull(stat);
    }

    public boolean createBarrier(String barrierPath) {
        try {
            getBarrier(barrierPath).setBarrier();
        } catch (Exception e) {
            // Exception only ensures that the barrier already exists
            log.info("Exception while creating Barrier. Confirms a pre-existing barrier: {}", e.getMessage());
            return true;
        }
        return false;
    }

    public void deleteBarrier(String path) {
        if (barrierExists(path)) {
            try {
                zkClient.delete().quietly().forPath(path);
                log.info("Barrier {} was deleted", path);
            } catch (Exception e) {
                log.error("I'm {}: There was an error deleting the barrier {}", getHostName(), path, e);
            }
        }
    }

    public void waitOnBarrier(String path) {
        try {
            log.info("Waiting on barrier {}", path);
            getBarrier(path).waitOnBarrier(MAX_WAITING_TIME, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("I'm {}: There was an error waiting in barrier {}", getHostName(), path, e);
        }
    }

    private String getHostName() {
        String hostName = "";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("There was an error to obtain the hostname");
        }
        return hostName;
    }
}
