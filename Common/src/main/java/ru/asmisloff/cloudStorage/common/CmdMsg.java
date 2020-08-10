package ru.asmisloff.cloudStorage.common;

/*Командные байты*/
public enum CmdMsg {
    LOGIN((byte)0),
    LOGOUT((byte)1),
    INFO((byte)2),
    SEND_FILE((byte)4),
    RECEIVE_FILE((byte)5),
    UNDEFINED((byte)6),
    UPLOADED_SUCCESSFULLY((byte)7);

    private byte msgCode;

    private CmdMsg(byte code) {
        msgCode = code;
    }

    public byte value() {
        return msgCode;
    }
}
