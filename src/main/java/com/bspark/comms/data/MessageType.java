package com.bspark.comms.data;

import lombok.Getter;

@Getter
public enum MessageType {
    INTERSECTION_STATUS(0x13, "교차로 상태 데이터", MessageCategory.RESPONSE),
    DETECTOR_INFO(0x23, "검지기 정보 데이터", MessageCategory.RESPONSE),
    PHASE_INFO(0x33, "현시 정보 데이터", MessageCategory.RESPONSE),
    STATUS_REQUEST(0x12, "상태 요청", MessageCategory.REQUEST),
    NETWORK_TEST(0xDA, "네트워크 테스트", MessageCategory.REQUEST),
    STARTUP_CODE(0xA2, "시작 코드", MessageCategory.REQUEST),
    USER_REQUEST(-1, "사용자 요청", MessageCategory.RESPONSE),
    UNKNOWN(-1, "알 수 없음", MessageCategory.UNKNOWN);

    private final int opcode;
    private final String description;
    private final MessageCategory category;

    MessageType(int opcode, String description, MessageCategory category) {
        this.opcode = opcode;
        this.description = description;
        this.category = category;
    }

    /**
     * opcode로 MessageType 찾기
     */
    public static MessageType fromOpcode(byte opcode) {
        int unsignedOpcode = opcode & 0xFF;
        for (MessageType type : values()) {
            if (type.opcode == unsignedOpcode) {
                return type;
            }
        }
        return USER_REQUEST;
    }

    /**
     * 응답 메시지인지 확인
     */
    public boolean isResponse() {
        return category == MessageCategory.RESPONSE;
    }

    /**
     * 요청 메시지인지 확인
     */
    public boolean isRequest() {
        return category == MessageCategory.REQUEST;
    }

    public enum MessageCategory {
        REQUEST, RESPONSE, UNKNOWN
    }
}