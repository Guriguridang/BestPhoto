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
import java.util.Arrays;

public class photo extends AppCompatActivity {
    private ImageView imageView;

    private ImageView imageView2;
    public byte[] byteArray;
    private float prevX = -1;
    private float prevY = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        imageView = findViewById(R.id.image);
        imageView2= findViewById(R.id.image2);

        Intent intent = getIntent();
        if (intent != null) {
            byte[] byteArray = intent.getByteArrayExtra("image");

            if (byteArray != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                ImageView imageView = findViewById(R.id.image);
                imageView.setImageBitmap(bitmap);
            }
        }
        // 보정하기
        Button btn_edit = findViewById(R.id.btn_editphoto);
        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recognizeFace(imageView);
            }
        });
        // 저장하기
        Button btn_save = findViewById(R.id.btn_savegallery);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFile("photo.jpg");
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
                            // 터치 시작점의 좌표
                            int startX = (int) event.getX(0);
                            int startY = (int) event.getY(0);

                            // 터치된 영역의 색상
                            int touchColor = originalBitmap.getPixel(startX, startY);

                            // Apply the touchColor to all touched pixels in the specified radius
                            int radius = 5; // 원하는 반경 설정
                            for (int dx = -radius; dx <= radius; dx++) {
                                for (int dy = -radius; dy <= radius; dy++) {
                                    int x = startX + dx;
                                    int y = startY + dy;

                                    // 픽셀이 이미지 내부에 있는지 확인
                                    if (x >= 0 && x < originalBitmap.getWidth() && y >= 0 && y < originalBitmap.getHeight()) {
                                        pixels[y * originalBitmap.getWidth() + x] = touchColor;
                                    }
                                }
                            }

                            // Create a new Bitmap and set the modified pixels
                            Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                            newBitmap.setPixels(pixels, 0, originalBitmap.getWidth(), 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight());

                            // Update the UI on the main thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (newBitmap != null && !newBitmap.sameAs(originalBitmap)) {
                                        imageView.setImageBitmap(newBitmap);
                                        imageView.invalidate();
                                    }
                                }
                            });
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

    private void saveFile(String filename) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put( MediaStore.Images.Media.IS_PENDING, 1); }

        ContentResolver contentResolver = getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(item, "w", null);
            FileOutputStream fos = new FileOutputStream(pdf.getFileDescriptor());
            fos.write(byteArray);
            fos.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING,0);
                contentResolver.update(item, values, null, null);
            }
            Toast.makeText(this,"갤러리에 파일을 저장하였습니다.",Toast.LENGTH_SHORT).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this,"File Err",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,"I/O Err",Toast.LENGTH_SHORT).show();
        }
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
}