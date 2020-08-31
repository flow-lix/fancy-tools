/**
 * @Copyright (c) 2019, Denali System Co., Ltd. All Rights Reserved.
 * Website: www.denalisystem.com | Email: marketing@denalisystem.com
 */
package org.fancy.remoting.rpc;

import lombok.extern.slf4j.Slf4j;
import org.fancy.remoting.AbstractNettyConfigRemoting;
import org.fancy.remoting.ConnectionEventListener;
import org.fancy.remoting.DefaultClientConnectionManger;
import org.fancy.remoting.DefaultConnectionMonitor;
import org.fancy.remoting.Reconnector;
import org.fancy.remoting.channel.ConnectionFactory;
import org.fancy.remoting.channel.ConnectionSelectStrategy;
import org.fancy.remoting.channel.DefaultConnectionFactory;
import org.fancy.remoting.channel.RandomSelectStrategy;
import org.fancy.remoting.codec.RpcCodecFactory;
import org.fancy.remoting.exception.LifeCycleException;
import org.fancy.remoting.handler.ConnectionEventHandler;
import org.fancy.remoting.handler.HeartbeatHandler;
import org.fancy.remoting.handler.RpcHandler;
import org.fancy.remoting.protocol.UserProcessor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class RpcClient extends AbstractNettyConfigRemoting {

    private final RpcTaskScanner TASK_SCANNER;
    private final ConcurrentMap<String, UserProcessor<?>> USER_PROCESSORS;
    private final ConnectionEventHandler connectionEventHandler;
    private final ConnectionEventListener connectionEventListener;

    private DefaultClientConnectionManger connectionManger;
    private DefaultConnectionMonitor connectionMoniter;
    private Reconnector reconnector;

    private RpcClientRemoting clientRemoting;

    public RpcClient(RpcTaskScanner TASK_SCANNER) {
        this.TASK_SCANNER = TASK_SCANNER;
        this.USER_PROCESSORS = new ConcurrentHashMap<>();
        this.connectionEventHandler = new ConnectionEventHandler();
        this.connectionEventListener = new ConnectionEventListener();
    }

    @Override
    public void startup() throws LifeCycleException {
        super.startup();

        for (UserProcessor<?> processor : USER_PROCESSORS.values()) {
            if (!processor.isStarted()) {
                processor.startup();
            }
        }

        ConnectionSelectStrategy selectStrategy = new RandomSelectStrategy();
        ConnectionFactory connectionFactory = new DefaultConnectionFactory(this, new RpcCodecFactory(), new HeartbeatHandler(), new RpcHandler(USER_PROCESSORS));
        this.connectionManger = new DefaultClientConnectionManger(selectStrategy, connectionFactory,
                connectionEventHandler, connectionEventListener, getGlobalSwitch());
        this.connectionManger.startup();

        this.clientRemoting = new RpcClientRemoting(new RpcCommandFactory(), this.connectionManger);

        this.TASK_SCANNER.add(connectionManger);
        this.TASK_SCANNER.startup();

        // toDo 1. Monitor; 2. Reconnect
    }

    @Override
    public void shutdown() throws LifeCycleException {

    }

    @Override
    public boolean isStarted() {
        return false;
    }


}
