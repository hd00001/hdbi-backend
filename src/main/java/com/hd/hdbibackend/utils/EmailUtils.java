package com.hd.hdbibackend.utils;

import cn.hutool.extra.mail.MailUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @注释
 */
public class EmailUtils {

    public static void sendCaptcha(String email, String captcha){
        //通过邮箱发送
        LocalDate currentDate = LocalDate.now();
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        // 格式化日期
        String formattedDate = currentDate.format(formatter);
        MailUtil.send(email, "哥布林智能BI用户注册", "亲爱的用户：\n" +
                "\n" +
                " \n" +
                "您好！感谢您使用哥布林智能分析服务，您的账号正在进行注册邮箱验证，本次请求的验证码为：\n" +
                "\n" +
                captcha+"(为了保障您账号的安全性，请在10分钟内完成验证。)\n" +
                "\n" +
                " \n" +
                "哥布林智能BI\n" +
                "\n" +
                formattedDate, false);
    }
}



