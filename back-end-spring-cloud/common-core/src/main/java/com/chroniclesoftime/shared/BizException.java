package com.chroniclesoftime.shared;

public class BizException extends RuntimeException {
    private final int code;
    public BizException(int code, String message) { super(message); this.code = code; }
    public BizException(ResultCode rc) { super(rc.message()); this.code = rc.code(); }
    public BizException(ResultCode rc, String message) { super(message); this.code = rc.code(); }
    public int getCode() { return code; }
}
