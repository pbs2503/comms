package com.bspark.comms.util;

public class MessageUtils {

    /**
     * 클라이언트 ID 유효성 검증
     */
    public static boolean isValidClientId(String clientId) {
        return clientId != null && !clientId.trim().isEmpty();
    }

    /**
     * 메시지 데이터 유효성 검증
     */
    public static boolean isValidMessage(byte[] data) {
        return data != null && data.length > 0;
    }

    /**
     * opcode를 16진수 문자열로 포맷
     */
    public static String formatOpcode(byte opcode) {
        return String.format("0x%02X", opcode & 0xFF);
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    /**
     * 16진수 문자열을 바이트 배열로 변환
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }

        hex = hex.replaceAll("\\s+", ""); // 공백 제거
        int length = hex.length();
        byte[] data = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }

        return data;
    }

    /**
     * 바이트 배열을 unsigned 값으로 변환 (방어적 복사)
     * 실제로는 Java의 byte는 이미 -128~127 범위이므로
     * 단순히 복사본을 반환합니다.
     */
    public static byte[] convertToUnsigned(byte[] data) {
        if (data == null) {
            return new byte[0];
        }
        return data.clone(); // 방어적 복사
    }

    /**
     * 메시지에서 opcode 추출
     * 메시지 구조: [길이(2바이트)] + [헤더(4바이트)] + [opcode(1바이트)] + [데이터]
     */
    public static byte extractOpcode(byte[] data) {
        if (data == null || data.length < 7) {
            throw new IllegalArgumentException("Data too short to extract opcode");
        }

        // 일반적인 메시지 구조에서 opcode는 7번째 바이트 (인덱스 6)
        // 실제 프로토콜에 맞게 조정해야 할 수 있습니다
        return data[7];
    }

    /**
     * 메시지에서 길이 필드 추출 (빅엔디안)
     */
    public static int extractLength(byte[] data) {
        if (data == null || data.length < 2) {
            return 0;
        }
        return ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    }

    /**
     * 메시지에서 데이터 부분만 추출
     */
    public static byte[] extractPayload(byte[] data) {
        if (data == null || data.length < 7) {
            return new byte[0];
        }

        // opcode 이후의 데이터 추출
        int payloadLength = data.length - 7;
        if (payloadLength <= 0) {
            return new byte[0];
        }

        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, 7, payload, 0, payloadLength);
        return payload;
    }

    /**
     * 메시지 구조 검증
     */
    public static boolean isValidMessageStructure(byte[] data) {
        if (data == null || data.length < 7) {
            return false;
        }

        int declaredLength = extractLength(data);
        return declaredLength == data.length;
    }

    /**
     * 바이트를 unsigned int로 변환
     */
    public static int toUnsignedInt(byte b) {
        return b & 0xFF;
    }

    /**
     * 바이트 배열의 특정 위치에서 2바이트를 읽어 int로 변환 (빅엔디안)
     */
    public static int readInt16BigEndian(byte[] data, int offset) {
        if (data == null || offset < 0 || offset + 1 >= data.length) {
            throw new IllegalArgumentException("Invalid data or offset for reading int16");
        }
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    /**
     * 바이트 배열의 특정 위치에서 4바이트를 읽어 long으로 변환 (빅엔디안)
     */
    public static long readInt32BigEndian(byte[] data, int offset) {
        if (data == null || offset < 0 || offset + 3 >= data.length) {
            throw new IllegalArgumentException("Invalid data or offset for reading int32");
        }
        return ((long)(data[offset] & 0xFF) << 24) |
                ((long)(data[offset + 1] & 0xFF) << 16) |
                ((long)(data[offset + 2] & 0xFF) << 8) |
                (long)(data[offset + 3] & 0xFF);
    }
}