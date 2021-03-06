package com.crazymakercircle.imServer.distributed;

import com.crazymakercircle.constants.ServerConstants;
import com.crazymakercircle.entity.ImNode;
import com.crazymakercircle.util.JsonUtil;
import com.crazymakercircle.zk.CuratorZKclient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 * IM 节点的ZK协调客户端

 **/
//节点命名服务等
@Data
@Slf4j
//单例模式  一个netty服务器只有一个imworker节点  来记录内容，以及一个router，而zk负责三个节点注册与发现 选举 编号，也就是通过网关， 来去返回地址，负载均衡

public class ImWorker {

    //Zk curator 客户端   负责zookeeper服务器以及上面服务的管理
    private CuratorFramework client = null;

    //保存当前Znode节点的路径，创建后返回
    private String pathRegistered = null;

    private ImNode localNode = null;

    private static ImWorker singleInstance = null;
    private boolean inited=false;

    //取得单例
    public synchronized static ImWorker getInst() {

        if (null == singleInstance) {
            singleInstance = new ImWorker();
            singleInstance.localNode = new ImNode();
        }
        return singleInstance;
    }

    private ImWorker() {

    }

    // 在zookeeper中创建临时节点

    /*
    第一个MANAGE_PATH是一个常量，值为"/im/nodes"，为所有Worker临时工作节点的
父亲节点的路径，在创建Worker节点之前，首先要检查一下，父亲ZNode节点是否存在，
否则的话，先创建父亲节点。"/im/nodes"父亲节点的创建方式是，持久化节点，而不是临
时节点。

第二路径pathPrefix是所有临时节点的前缀，值为"/im/nodes/"，是在工作路径后，加上
一个“/”分割符。也可在工作路径的后面，加上其他的前缀字符，如"/im/nodes/id-"、
“/im/nodes/seq-”等等。

第三路径pathRegistered是临时节点创建成功之后，返回的完整路径。例如：
/im/nodes/0000000000，/im/nodes/0000000001等等。后边的编号是顺序的。
创建节点成功后，截取后边的编号数字，放在POJO对象id属性中供后边使用：
     */


    public synchronized void init() {

        if(inited)
        {
            return;
        }
        inited=true;
        if (null == client) {
            //根据zookeeper的地址来进行创建客户端   这里的地址是某个zookeeper节点的客户端端口
            this.client = CuratorZKclient.instance.getClient();
        }
        if (null == localNode) {
            localNode = new ImNode();
        }

        //创建父节点

        createParentIfNeeded(ServerConstants.MANAGE_PATH);

        // 创建一个 ZNode 节点
        // 节点的 payload 为当前worker 实例

        try {
            //创建一个netty节点上附带的数据
            byte[] payload = JsonUtil.object2JsonBytes(localNode);

            //返回节点编号   创建节点
            pathRegistered = client.create()
                    .creatingParentsIfNeeded()
                    //临时节点  顺序节点
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(ServerConstants.PATH_PREFIX, payload);

            //为node 设置id
            //从返回的路径中获取id
            localNode.setId(getId());
            log.info("本地节点, path={}, id={}",     pathRegistered, localNode.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setLocalNode(String ip, int port) {
        localNode.setHost(ip);
        localNode.setPort(port);
    }

    /**
     * 取得IM 节点编号
     *
     * @return 编号
     */
    public long getId() {

        return getIdByPath(pathRegistered);

    }

    /**
     * 取得IM 节点编号
     *
     * @param path 路径
     * @return 编号
     */
    public long getIdByPath(String path) {
        String sid = null;
        if (null == path) {
            throw new RuntimeException("节点路径有误");

        }
        //去除前缀后的节点id
        int index = path.lastIndexOf(ServerConstants.PATH_PREFIX);
        if (index >= 0) {
            index += ServerConstants.PATH_PREFIX.length();
            sid = index <= path.length() ? path.substring(index) : null;
        }

        if (null == sid) {
            throw new RuntimeException("节点ID获取失败");
        }

        return Long.parseLong(sid);

    }


    /**
     * 增加负载，表示有用户登录成功
     *
     * @return 成功状态
     */
    public boolean incBalance() {
        if (null == localNode) {
            throw new RuntimeException("还没有设置Node 节点");
        }
        // 增加负载：增加负载，并写回zookeeper
        while (true) {
            try {
                //写入
                localNode.incrementBalance();
                byte[] payload = JsonUtil.object2JsonBytes(localNode);
                //写到当前节点中
                client.setData().forPath(pathRegistered, payload);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * 减少负载，表示有用户下线，写回zookeeper
     *
     * @return 成功状态
     */
    public boolean decrBalance() {
        if (null == localNode) {
            throw new RuntimeException("还没有设置Node 节点");
        }
        while (true) {
            try {
                //事实的写入
                localNode.decrementBalance();

                byte[] payload = JsonUtil.object2JsonBytes(localNode);
                client.setData().forPath(pathRegistered, payload);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }

    /**
     * 创建父节点
     *
     * @param managePath 父节点路径
     */
    private void createParentIfNeeded(String managePath) {

        try {
            //判断路径是否存在
            Stat stat = client.checkExists().forPath(managePath);
            if (null == stat) {
                client.create()
                        .creatingParentsIfNeeded()
                        .withProtection()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(managePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 返回本地的节点信息
     *
     * @return 本地的节点信息
     */
    public ImNode getLocalNodeInfo() {
        return localNode;
    }

}