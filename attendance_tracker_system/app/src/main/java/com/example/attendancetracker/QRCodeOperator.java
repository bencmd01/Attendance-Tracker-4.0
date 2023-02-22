package com.example.attendancetracker;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Class containing methods to operate on student QR codes. Currently, the only
 * functionality it has is generating a student QR code based on the student email.
 */
public class QRCodeOperator {

    /**
     * Encodes the given student's email as a QR code (2D bitmap) and returns
     * the generated QR code.
     */
    public static Bitmap generateQRCode(String studentEmail) {
        Bitmap bitmap = null;
        int width = 256;
        int height = 256;

        Log.d("DEBUG", "QR Code; Student Email: " + studentEmail);

        QRCodeWriter writer = new QRCodeWriter();
        try {
            // Encode student email as a BitMatrix
            BitMatrix bitMatrix = writer.encode(String.format("%s", studentEmail), BarcodeFormat.QR_CODE, width, height);

            // Create a bitmap (QR code) based on the BitMatrix.
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
