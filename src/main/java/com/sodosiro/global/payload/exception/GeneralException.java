package com.sodosiro.global.payload.exception;


import com.sodosiro.global.payload.code.BaseCode;
import com.sodosiro.global.payload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private BaseCode code;

    public ReasonDTO getErrorReason() {
        return this.code.getReason();
    }

    public ReasonDTO getErrorReasonHttpStatus(){
        return this.code.getReasonHttpStatus();
    }

    public GeneralException(BaseCode code, Throwable e) {
        super(code.getReason().getMessage(), e);
        this.code = code;
    }

}