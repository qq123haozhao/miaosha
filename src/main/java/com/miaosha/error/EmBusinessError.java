package com.miaosha.error;

public enum EmBusinessError implements CommonError {
    //通用类型错误 10001
    PARAMETER_VALIDATION_ERROR(10001, "参数不合法"),
    UNKNOW_ERROR(10002, "未知错误"),

    // 20000 开头为用户信息相关错误定义
    USER_NOT_EXIST(20001, "用户不存在"),
    LOGIN_ERROR(20002, "用户名或密码错误"),
    USER_NOT_LOGIN(20003, "用户未登陆"),

    //30000 开头为商品信息相关错误定义
    ITEM_NOT_EXIST(30001, "商品不存在"),

    //40000 开头为订单信息相关错误定义
    STOCK_NOT_ENOUGH(40001, "商品库存不足")
    ;

    private EmBusinessError(int errCode, String errMsg){
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    private int errCode;
    private String errMsg;

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
