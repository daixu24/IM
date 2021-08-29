package com.crazymakercircle.imServer.distributed;

import com.crazymakercircle.constants.ServerConstants;
import com.crazymakercircle.entity.ImNode;
import com.crazymakercircle.im.common.bean.msg.ProtoMsg;
import com.crazymakercircle.imServer.protoBuilder.NotificationMsgBuilder;
import com.crazymakercircle.util.JsonUtil;
import com.crazymakercircle.util.ObjectUtil;
import com.crazymakercircle.util.ThreadUtil;
import com.crazymakercircle.zk.CuratorZKclient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**

 **/
@Data
@Slf4j



/*
1,订阅集群中的其他在线netty服务器
2，要其他netty服务器建立长连接，用于转发消息


 */

public class WorkerRouter {
    //Zk客户端
    private CuratorFramework client = null;

    //唯一实例
    private String pathRegistered = null;
    private ImNode node = null;


    private static WorkerRouter singleInstance = null;
    //监听路径
    private static final String path = ServerConstants.MANAGE_PATH;

    //其他节点容器
    private ConcurrentHashMap<Long, PeerSender> workerMap =
            new ConcurrentHashMap<>();


    //调用回调方法，添加进map 并建立连接
   private BiConsumer<ImNode, PeerSender> runAfterAdd = (node, relaySender) -> {
        doAfterAdd(node, relaySender);
    };

    private  Consumer<ImNode> runAfterRemove = (node) -> {
        doAfterRemove(node);
    };


    public synchronized static WorkerRouter getInst() {
        if (null == singleInstance) {
            singleInstance = new WorkerRouter();
        }
        return singleInstance;
    }

    private WorkerRouter() {

    }

    private boolean inited=false;

    /**
     * 初始化节点管理
     */
    public void init() {

        if(inited)
        {
            return;
        }
        inited=true;

        try {
            if (null == client) {
                this.client = CuratorZKclient.instance.getClient();

            }

            //订阅节点的增加和删除事件
            //都是zk封装的
            //负责监听
            PathChildrenCache childrenCache = new PathChildrenCache(client, path, true);
            PathChildrenCacheListener childrenCacheListener = new PathChildrenCacheListener() {

                @Override
                public void childEvent(CuratorFramework client,
                                       PathChildrenCacheEvent event) throws Exception {
                    log.info("开始监听其他的ImWorker子节点:-----");
                    //获取事件的数据
                    ChildData data = event.getData();
                    switch (event.getType()) {
                        //在上一小节中，我们已经知道，一个节点上线时，首先要通过命名服务加入到Netty集
                        //群中。在上面的代码中，WorkerRouter路由器使用Curator的TreeCache缓存订阅了节点的
                        //NODE_ADDED节点添加消息。当一个新的Netty节点加入时，调用processNodeAdded(data)
                        //方法在本地保存一份节点的POJO信息，并且建立一个消息中转的Netty客户连接。
                        //处理节点添加的方法processNodeAdded(data)比较重要，代码如下：
                        case CHILD_ADDED:
                            //节点添加
                            log.info("CHILD_ADDED : " + data.getPath() + "  数据:" + data.getData());
                            processNodeAdded(data);
                            break;
                        case CHILD_REMOVED:
                            log.info("CHILD_REMOVED : " + data.getPath() + "  数据:" + data.getData());
                            processNodeRemoved(data);
                            break;
                        case CHILD_UPDATED:
                            log.info("CHILD_UPDATED : " + data.getPath() + "  数据:" + new String(data.getData()));
                            break;
                        default:
                            log.debug("[PathChildrenCache]节点数据为空, path={}", data == null ? "null" : data.getPath());
                            break;
                    }

                }

            };

            childrenCache.getListenable().addListener(
                    childrenCacheListener, ThreadUtil.getIoIntenseTargetThreadPool());
            System.out.println("Register zk watcher successfully!");
            childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processNodeRemoved(ChildData data) {

        byte[] payload = data.getData();
        ImNode node = ObjectUtil.JsonBytes2Object(payload, ImNode.class);

        long id = ImWorker.getInst().getIdByPath(data.getPath());
        node.setId(id);
        log.info("[TreeCache]节点删除, path={}, data={}",
                data.getPath(), JsonUtil.pojoToJson(node));


        if (runAfterRemove != null) {
            runAfterRemove.accept(node);
        }


    }

    private void doAfterRemove(ImNode node) {
        PeerSender peerSender = workerMap.get(node.getId());

        if (null != peerSender) {
            peerSender.stopConnecting();
            workerMap.remove(node.getId());
        }


    }

    /**
     * 节点增加的处理
     *
     * @param data 新节点
     */
    private void processNodeAdded(ChildData data) {
              byte[] payload = data.getData();
        ImNode node = ObjectUtil.JsonBytes2Object(payload, ImNode.class);


        //获取节点编号  这里面有path路径
        long id = ImWorker.getInst().getIdByPath(data.getPath());
        node.setId(id);

        log.info("[TreeCache]节点更新端口, path={}, data={}",
                data.getPath(), JsonUtil.pojoToJson(node));

        if (node.equals(getLocalNode())) {
            log.info("[TreeCache]本地节点, path={}, data={}",
                    data.getPath(), JsonUtil.pojoToJson(node));
            return;
        }
        //这里是netty服务器的注册 不要弄错了
        //netty服务器， 如果
        //添加进map
        PeerSender relaySender = workerMap.get(node.getId());
        //重复收到注册的事件
        if (null != relaySender && relaySender.getRmNode().equals(node)) {
            //原来map中的node = 刚刚注册的node
            //也有可能ip不一样的不一样的节点 只是下标一样  原来的还没删除  或者替换了  1号节点还没下线就被替换了  完善化，不一定删除一定成功
            //重复注册也就是端口 ip完全一样的注册，
            //

            //*  这个节点id是我们，指定的，有可能发送了多次请求

            log.info("[TreeCache]节点重复增加, path={}, data={}",
                    data.getPath(), JsonUtil.pojoToJson(node));
            return;
        }

        if (runAfterAdd != null) {
            runAfterAdd.accept(node, relaySender);
        }
    }


    private void doAfterAdd(ImNode n, PeerSender relaySender) {
        if (null != relaySender) {
            //关闭老的连接
            relaySender.stopConnecting();
        }
        //创建一个消息转发器
        relaySender = new PeerSender(n);
        //建立转发的连接
        relaySender.doConnect();

        //这里map直接就转发了 sender而不需要进行其他操作
        //netty节点编号 + 连接
        workerMap.put(n.getId(), relaySender);
    }


    public PeerSender route(long nodeId) {
        PeerSender peerSender = workerMap.get(nodeId);
        if (null != peerSender) {
            return peerSender;
        }
        return null;
    }


    public void sendNotification(String json) {
        workerMap.keySet().stream().forEach(
                key ->
                {
                    if (!key.equals(getLocalNode().getId())) {
                        PeerSender peerSender = workerMap.get(key);
                        ProtoMsg.Message pkg = NotificationMsgBuilder.buildNotification(json);
                        peerSender.writeAndFlush(pkg);
                    }
                }
        );

    }


    public ImNode getLocalNode() {
        return ImWorker.getInst().getLocalNodeInfo();
    }

    public void remove(ImNode remoteNode) {
        workerMap.remove(remoteNode.getId());
        log.info("[TreeCache]移除远程节点信息,  node={}", JsonUtil.pojoToJson(remoteNode));
    }
}
