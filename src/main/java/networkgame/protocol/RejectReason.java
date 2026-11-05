package networkgame.protocol;

/**
 * 입장을 거절한 이유.
 */
public enum RejectReason {
    /** 4명이 다 찼다. */
    FULL,
    /** 같은 이름이 이미 들어와 있다. */
    NAME_TAKEN
}
