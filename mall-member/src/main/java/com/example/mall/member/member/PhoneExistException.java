package com.example.mall.member.member;

public class PhoneExistException extends RuntimeException {
    public PhoneExistException() {
        super("该手机号码已注册");
    }

}
