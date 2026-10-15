package com.example.campus.dto;

import lombok.Data;

/**
 * 移动端提交实名认证资料的请求体。
 * <p>
 * 图片必须先走 {@code POST /files/upload} 拿到 {@code /files/yyyy/MM/dd/x.png}，
 * 再把返回的相对路径填进 studentCertUrl —— 这个接口只收路径，不收文件。
 */
@Data
public class VerifySubmitRequest {

    /** 真实姓名，2~10 个汉字 */
    private String realName;

    /**
     * 学号。
     * <p>
     * 注意：users.student_id 同时是小程序端的登录账号
     * （AuthController.login 就是按它查用户的），所以这里**只做一致性校验、不会改它** ——
     * 改了用户自己就登不进来了，而且等于允许把账号过户给别人。
     */
    private String studentId;

    /** 学生证照片地址，来自 /files/upload */
    private String studentCertUrl;
}
