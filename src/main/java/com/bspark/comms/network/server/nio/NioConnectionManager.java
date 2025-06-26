package com.bspark.comms.network.server.nio;

import com.bspark.comms.dao.IpAddressWhitelistDAO;
import com.bspark.comms.data.MessageType;
import com.bspark.comms.events.ClientConnectedEvent;
import com.bspark.comms.events.ClientDisconnectedEvent;
import com.bspark.comms.events.DataReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NioConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(NioConnectionManager.class);

    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, SelectionKey> clientMap = new ConcurrentHashMap<>();
    private final Map<SelectionKey, String> keyMap = new ConcurrentHashMap<>();
    private final Map<SelectionKey, ByteBuffer> bufferMap = new ConcurrentHashMap<>();
    private final AtomicInteger connectionSequence = new AtomicInteger(0);
    @Autowired
    private final IpAddressWhitelistDAO whiteListDao;

    private Set<String> whiteList = ConcurrentHashMap.newKeySet();
    private final int BUFFER_SIZE = 8192;

    public NioConnectionManager(ApplicationEventPublisher eventPublisher, IpAddressWhitelistDAO whiteListDao) {
        this.eventPublisher = eventPublisher;
        this.whiteListDao = whiteListDao;
    }

    public void setWhiteList(Set<String> whiteList) {
        if (whiteList != null) {
            this.whiteList = ConcurrentHashMap.newKeySet();
            this.whiteList.addAll(whiteList);
        }
    }

    public void acceptConnection(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        // 클라이언트 연결 수락
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) {
            return;
        }

        String clientIp = ((InetSocketAddress)clientChannel.getRemoteAddress()).getAddress().getHostAddress();

        // 화이트리스트 확인


        if (!whiteList.isEmpty() && !whiteList.contains(clientIp) && !whiteListDao.isIpAllowed(clientIp)) {
            logger.warn("화이트리스트에 없는 IP에서 연결 시도: {}", clientIp);
            clientChannel.close();
            return;
        }

        // 논블로킹 모드 설정
        clientChannel.configureBlocking(false);

        // 클라이언트 ID 생성
        String clientId = generateClientId(clientChannel);

        // 읽기 이벤트 등록
        SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ);

        // 클라이언트 정보 저장
        clientMap.put(clientId, key);
        keyMap.put(key, clientId);
        bufferMap.put(key, ByteBuffer.allocate(BUFFER_SIZE));

        logger.info("클라이언트 연결 수락: {} ({})", clientId, clientIp);

        // 연결 이벤트 발행
        eventPublisher.publishEvent(new ClientConnectedEvent(this, clientId, clientIp));
    }

    public void readData(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        String clientId = keyMap.get(key);
        ByteBuffer buffer = bufferMap.get(key);

        if (clientId == null || buffer == null) {
            closeConnection(key);
            return;
        }

        try {
            buffer.clear();
            int bytesRead = channel.read(buffer);

            if (bytesRead == -1) {
                // 연결 종료
                closeConnection(key);
                return;
            }

            // 데이터 수신 이벤트 발행 부분 수정
            if (bytesRead > 0) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                logger.debug("데이터 수신: {} ({} 바이트)", clientId, data.length);

                // 메시지 유형 결정 (7번 인덱스의 바이트를 opcode로 가정)
                MessageType messageType = MessageType.UNKNOWN;
                if (data.length > 0) {
                    // 첫 바이트를 opcode로 사용하여 메시지 타입 결정
                    messageType = MessageType.fromOpcode(data[7]);
                }

                // 데이터 수신 이벤트 발행
                eventPublisher.publishEvent(new DataReceivedEvent(
                        this, clientId, messageType, data));
            }
        } catch (IOException e) {
            logger.error("데이터 읽기 오류 {}: {}", clientId, e.getMessage());
            closeConnection(key);
        }
    }

    public boolean sendData(String clientId, byte[] data) {
        SelectionKey key = clientMap.get(clientId);
        if (key == null || !key.isValid()) {
            logger.warn("존재하지 않는 클라이언트에게 데이터 전송 시도: {}", clientId);
            return false;
        }

        SocketChannel channel = (SocketChannel) key.channel();
        if (!channel.isConnected()) {
            logger.warn("연결되지 않은 채널에 데이터 전송 시도: {}", clientId);
            closeConnection(key);
            return false;
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            logger.debug("데이터 전송 성공: {} ({} 바이트)", clientId, data.length);
            return true;
        } catch (IOException e) {
            logger.error("데이터 전송 오류 {}: {}", clientId, e.getMessage());
            closeConnection(key);
            return false;
        }
    }

    public void closeConnection(SelectionKey key) {
        if (!key.isValid()) {
            return;
        }

        String clientId = keyMap.remove(key);
        if (clientId != null) {
            clientMap.remove(clientId);

            // 연결 종료 이벤트 발행
            eventPublisher.publishEvent(new ClientDisconnectedEvent(this, clientId));
        }

        bufferMap.remove(key);

        try {
            key.channel().close();
        } catch (IOException e) {
            logger.error("채널 종료 중 오류: {}", e.getMessage());
        }

        key.cancel();

        if (clientId != null) {
            logger.info("클라이언트 연결 종료: {}", clientId);
        }
    }

    public void closeAllConnections() {
        for (SelectionKey key : keyMap.keySet()) {
            closeConnection(key);
        }

        clientMap.clear();
        keyMap.clear();
        bufferMap.clear();
    }

    /**
     * 특정 클라이언트 연결 종료
     */
    public boolean disconnectClient(String clientId) {
        SelectionKey key = clientMap.get(clientId);
        if (key == null || !key.isValid()) {
            logger.warn("연결 종료 요청된 클라이언트가 존재하지 않음: {}", clientId);
            return false;
        }

        closeConnection(key);
        return true;
    }

    /**
     * 클라이언트 ID 생성 (IP 주소만 사용, 중복 시 기존 연결 종료)
     */
    private String generateClientId(SocketChannel channel) {
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress) channel.getRemoteAddress();
            String ip = remoteAddress.getAddress().getHostAddress();

            // 같은 IP의 기존 연결이 있으면 기존 연결 종료
            disconnectExistingConnection(ip);

            return ip;
        } catch (IOException e) {
            return "unknown";
        }
    }

    /**
     * 같은 IP의 기존 연결 종료
     */
    private void disconnectExistingConnection(String ip) {
        SelectionKey existingKey = clientMap.get(ip);
        if (existingKey != null && existingKey.isValid()) {
            logger.info("같은 IP의 기존 연결 종료: {}", ip);
            closeConnection(existingKey);
        }
    }

    public Map<String, String> getConnectedClients() {
        Map<String, String> clients = new HashMap<>();
        for (Map.Entry<String, SelectionKey> entry : clientMap.entrySet()) {
            try {
                SocketChannel channel = (SocketChannel) entry.getValue().channel();
                InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                clients.put(entry.getKey(), address.getAddress().getHostAddress());
            } catch (Exception e) {
                clients.put(entry.getKey(), "unknown");
            }
        }
        return clients;
    }

    public int getActiveConnectionCount() {
        return clientMap.size();
    }
}