package org.stool.myserver.core;

import io.netty.channel.EventLoopGroup;
import org.stool.myserver.core.EntryPoint;

import java.util.concurrent.ExecutorService;

public interface EntryPointInternal extends EntryPoint {


    EventLoopGroup getIOWorkerEventLoopGroup();

    EventLoopGroup getAcceptorEventLoopGroup();

    ExecutorService getWorkerPool();


    Context getContext();
}
