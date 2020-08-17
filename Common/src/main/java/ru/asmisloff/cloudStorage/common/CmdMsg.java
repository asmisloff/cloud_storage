package ru.asmisloff.cloudStorage.common;

/*Командные байты*/
public enum CmdMsg {
    LOGIN((byte)0),
    LOGOUT((byte)1),
    FILE_INFO((byte)2),
    SEND_FILE((byte)4),
    RECEIVE_FILE((byte)5),
    SERVICE_REPORT((byte)6),
    ERROR((byte)7);

    private byte msgCode;

    CmdMsg(byte code) {
        msgCode = code;
    }

    public byte value() {
        return msgCode;
    }
}
