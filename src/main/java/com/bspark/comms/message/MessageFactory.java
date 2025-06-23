
package com.bspark.comms.message;

import com.bspark.comms.core.protocol.message.MessageBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @deprecated MessageFactory는 MessageBuilder로 대체되었습니다.
 * 하위 호환성을 위해 유지됩니다.
 */
@Component
@RequiredArgsConstructor
@Deprecated(since = "2.0", forRemoval = true)
public class MessageFactory {

    private final MessageBuilder messageBuilder;

    /**
     * @deprecated MessageBuilder.buildMessage(opcode)를 사용하세요.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public byte[] createMessage(byte opcode) {
        return messageBuilder.buildMessage(opcode);
    }

    /**
     * @deprecated MessageBuilder.buildMessage(opcode, data)를 사용하세요.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public byte[] createMessage(byte opcode, byte[] data) {
        return messageBuilder.buildMessage(opcode, data);
    }
}