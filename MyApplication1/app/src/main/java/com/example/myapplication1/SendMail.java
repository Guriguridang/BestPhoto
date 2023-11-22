package com.example.myapplication1;

import android.content.Context;
import android.graphics.Bitmap;

public class SendMail {
    String user = "suny6932@gmail.com";
    String password = "ojpt ymtf eigt ytfc";
    Bitmap bitmap;

    public SendMail(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    GMailSender gMailSender = new GMailSender(user, password);
    String emailCode = gMailSender.getEmailCode();

    public boolean sendSecurityCode(Context context, String sendTo) {
        try {
            gMailSender.sendMailWithImage("Guriguridang 이미지 전송", sendTo, bitmap);
            return true;

        } catch (Exception e) {
            return false;
        }

    }
}
