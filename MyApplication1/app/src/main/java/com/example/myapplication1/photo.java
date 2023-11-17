package com.example.myapplication1;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class photo extends AppCompatActivity {
    private ImageView imageView;
    private float prevX = -1;
    private float prevY = -1;
    private boolean isDragging = false;

    private Matrix imageMatrix = new Matrix();
    private byte[] byteArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        imageView = findViewById(R.id.image);
        Button btn_save=findViewById(R.id.btn_savegallery);

        Intent intent = getIntent();
        if (intent != null) {
            byteArray = intent.getByteArrayExtra("image");

            if (byteArray != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                ImageView imageView = findViewById(R.id.image);
                imageView.setImageBitmap(bitmap);


            }
            btn_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    saveBitmapToFile(getBitmapFromImageView(imageView));

                }
            });
        }
        Button btn_edit = findViewById(R.id.btn_editphoto);
        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recognizeFace(imageView);
            }
        });
        imageView.setOnTouchListener(new View.OnTouchListener() {
            private Bitmap originalBitmap;
            private int[] pixels;
            private int startPixel; // To store the pixel value of the initial touch position

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Get the current bitmap from the ImageView
                        Drawable drawable = imageView.getDrawable();
                        if (drawable instanceof BitmapDrawable) {
                            originalBitmap = ((BitmapDrawable) drawable).getBitmap();
                            pixels = new int[originalBitmap.getWidth() * originalBitmap.getHeight()];
                            originalBitmap.getPixels(pixels, 0, originalBitmap.getWidth(), 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight());

                            // Capture the pixel color of the initial touch position
                            int startX = (int) event.getX();
                            int startY = (int) event.getY();
                            startPixel = pixels[startY * originalBitmap.getWidth() + startX];
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (originalBitmap != null) {
                            // Apply the startPixel color to all touched pixels
                            int x = (int) event.getX();
                            int y = (int) event.getY();
                            pixels[y * originalBitmap.getWidth() + x] = startPixel;
                            Bitmap newBitmap = Bitmap.createBitmap(pixels, originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                            imageView.setImageBitmap(newBitmap);
                            Toast.makeText(photo.this, "d", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        originalBitmap = null;
                        pixels = null;
                        break;
                }
                return true;
            }
        });




    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // If the Drawable is not a BitmapDrawable, create a new Bitmap
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    private void recognizeFace(ImageView imageView) {
        try {
            System.loadLibrary("opencv_java4");
            Context context = getApplicationContext();
            InputStream is3 = context.getAssets().open("haarcascade_frontalface_default.xml");

            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is3.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            os.close();
            CascadeClassifier faceCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());

            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            Mat originalMatImg = new Mat();
            Utils.bitmapToMat(bitmap, originalMatImg);

            Mat gray2 = new Mat();
            Imgproc.cvtColor(originalMatImg, gray2, Imgproc.COLOR_RGBA2GRAY);

            MatOfRect faces;
            faces = new MatOfRect();

            faceCascade.detectMultiScale(gray2, faces, 1.3, 5);

            for (Rect rect : faces.toArray()) {
                System.out.println("인식된 얼굴 객체 좌표 :");
                System.out.println(rect);
                Imgproc.rectangle(originalMatImg, rect.tl(), rect.br(), new Scalar(255, 0, 0), 8);
            }
            Bitmap resultBitmapImg = Bitmap.createBitmap(gray2.cols(), gray2.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(originalMatImg, resultBitmapImg);
            imageView.setImageBitmap(resultBitmapImg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveBitmapToFile(Bitmap bitmap) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
        String timestamp = sdf.format(new Date());

        String filename = "image_" + timestamp + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // 이미지 형식 지정

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver contentResolver = getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            // 비트맵을 JPEG 파일로 압축하여 저장
            OutputStream outputStream = contentResolver.openOutputStream(item);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                contentResolver.update(item, values, null, null);
            }
            Toast.makeText(this, "갤러리에 파일을 저장하였습니다.", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "파일 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getBitmapFromImageView(ImageView imageView) {
        // 이미지뷰에서 Drawable을 가져옵니다.
        Drawable drawable = imageView.getDrawable();

        // Drawable을 비트맵으로 변환합니다.
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            // Drawable이 비트맵이 아닌 경우, 새로운 비트맵을 생성하여 이미지뷰의 내용을 그려줍니다.
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }
    }

}