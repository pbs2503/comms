package com.bspark.comms.network.server.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NioTcpServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(NioTcpServer.class);

    private final NioConnectionManager connectionManager;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private Thread serverThread;

    @Value("${server.tcp.buffer-size:8192}")
    private int bufferSize;

    public NioTcpServer(NioConnectionManager connectionManager,
                        ApplicationEventPublisher eventPublisher) {
        this.connectionManager = connectionManager;
        this.eventPublisher = eventPublisher;
    }

    public void start(int port, Set<String> whiteList) {
        if (running.get()) {
            logger.warn("NIO TCP 서버가 이미 실행 중입니다");
            return;
        }

        try {
            // Selector 초기화
            selector = Selector.open();

            // 서버 소켓 채널 초기화
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));

            // 연결 수락 이벤트 등록
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            // 화이트리스트 설정
            connectionManager.setWhiteList(whiteList);

            // 서버 스레드 시작
            running.set(true);
            serverThread = new Thread(this, "nio-tcp-server");
            serverThread.setDaemon(true);
            serverThread.start();

            logger.info("NIO TCP 서버가 포트 {}에서 시작되었습니다", port);
        } catch (IOException e) {
            logger.error("NIO TCP 서버 시작 실패: {}", e.getMessage(), e);
            closeResources();
        }
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                // 이벤트 기다리기 (블로킹)
                int readyChannels = selector.select(100);

                if (!running.get()) {
                    break;
                }

                if (readyChannels == 0) {
                    continue;
                }

                // 준비된 이벤트 처리
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    // 연결 수락 이벤트
                    if (key.isAcceptable()) {
                        connectionManager.acceptConnection(serverChannel, selector);
                    }

                    // 읽기 이벤트
                    if (key.isReadable()) {
                        connectionManager.readData(key);
                    }
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                logger.error("NIO TCP 서버 실행 중 오류: {}", e.getMessage(), e);
            }
        } finally {
            closeResources();
        }
    }

    public void stop() {
        if (!running.getAndSet(false)) {
            logger.warn("NIO TCP 서버가 실행 중이 아닙니다");
            return;
        }

        logger.info("NIO TCP 서버 중지 중...");

        // 서버 스레드 깨우기 위해 selector 종료
        if (selector != null) {
            selector.wakeup();
        }

        // 스레드 종료 대기
        if (serverThread != null) {
            try {
                serverThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("서버 스레드 종료 대기 중 인터럽트 발생");
            }
        }

        // 모든 연결 종료
        connectionManager.closeAllConnections();

        logger.info("NIO TCP 서버가 중지되었습니다");
    }

    private void closeResources() {
        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            logger.error("리소스 정리 중 오류: {}", e.getMessage(), e);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getServerPort() {
        return serverChannel != null && serverChannel.socket().isBound()
                ? serverChannel.socket().getLocalPort() : -1;
    }

    @PreDestroy
    public void shutdown() {
        stop();
    }
}