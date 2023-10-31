package com.example.myapplication1;

import com.example.myapplication1.ParcelableFile;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

// add
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import pl.droidsonroids.gif.GifDrawable;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


public class gifViewer extends AppCompatActivity {
    private ImageView imageView;
    private LinearLayout llImagesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_viewer);
        imageView = findViewById(R.id.m_imageView);
        HorizontalScrollView horizontalScrollView = findViewById(R.id.horizontalScrollView);
        llImagesContainer = findViewById(R.id.llImagesContainer);


        // 갤러리 인텐트 받은 경우
        String imageUriString = getIntent().getStringExtra("imageuri");
        System.out.println("uri ====" + imageUriString);

        Uri imageUri = Uri.parse(imageUriString);
        // 이미지 URI를 ImageView에 설정하여 gif 이미지 표시
        //        Glide.with(this).asGif().load(imageUri).into(imageView);
        Glide.with(this).load(imageUri).into(imageView);

        // 이미지 보정
        Button extractFramesButton = findViewById(R.id.btnExtractFrames);
        extractFramesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(gifViewer.this, "이미지를 보정합니다", Toast.LENGTH_SHORT).show();
                extractEyes(imageView);
            }
        });

    }
    private void extractEyes(ImageView imageView) {
        try {
            System.loadLibrary("opencv_java4");
            // haarcascade_frontalface_default.xml 파일 로드
            Context context = getApplicationContext();
            InputStream faceCascadeInputStream = context.getAssets().open("haarcascade_frontalface_default.xml");

            // InputStream을 앱의 캐시 디렉터리의 temporary file로 복사
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File faceCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(faceCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = faceCascadeInputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            os.close();

            // faceCascade 객체 생성
            CascadeClassifier faceCascade = new CascadeClassifier(faceCascadeFile.getAbsolutePath());

            // 갤러리에서 불러온 이미지를 얼굴 객체를 인식하기 위해 Mat 형식으로 변환
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            Mat originalMatImg = new Mat();
            Utils.bitmapToMat(bitmap, originalMatImg);

            // 이미지를 분석하기 위해 흑백 이미지로 변환
            Mat gray = new Mat();
            Imgproc.cvtColor(originalMatImg, gray, Imgproc.COLOR_RGBA2GRAY);

            // 얼굴을 검출하여 faces에 저장
            MatOfRect faces = new MatOfRect();
            faceCascade.detectMultiScale(gray, faces, 1.3, 5);

            // 검출된 얼굴에 대해 눈을 검출하고 사각형으로 표시
            for (Rect faceRect : faces.toArray()) {
                // 얼굴 영역에 하얀색 사각형 그리기
                Imgproc.rectangle(originalMatImg, faceRect.tl(), faceRect.br(), new Scalar(255, 255, 255), 2);
                Mat faceROI = new Mat(gray, faceRect);
                detectEyes(faceROI, originalMatImg);
            }
            // 결과를 ImageView에 표시
            Bitmap resultBitmapImg = Bitmap.createBitmap(originalMatImg.cols(), originalMatImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(originalMatImg, resultBitmapImg);
            imageView.setImageBitmap(resultBitmapImg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void detectEyes(Mat faceROI, Mat originalMatImg) throws IOException {
        // haarcascade_eye.xml 파일 로드
        Context context = getApplicationContext();
        InputStream eyeCascadeInputStream = context.getResources().openRawResource(R.raw.haarcascade_eye);

        // InputStream을 앱의 캐시 디렉터리의 temporary file로 복사
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        File eyeCascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
        FileOutputStream os = new FileOutputStream(eyeCascadeFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = eyeCascadeInputStream.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }

        os.close();

        // eyeCascade 객체 생성
        CascadeClassifier eyeCascade = new CascadeClassifier(eyeCascadeFile.getAbsolutePath());

        MatOfRect eyes = new MatOfRect();
        eyeCascade.detectMultiScale(faceROI, eyes, 1.1, 2, 0, new Size(30, 30));

        // 검출된 눈에 대해 빨간색 사각형 표시
        for (Rect eyeRect : eyes.toArray())
        {
            // 상대 좌표로 변환
            float relativeEyeRectLeft = (float) eyeRect.tl().x / faceROI.cols();
            float relativeEyeRectTop = (float) eyeRect.tl().y / faceROI.rows();
            float relativeEyeRectRight = (float) eyeRect.br().x / faceROI.cols();
            float relativeEyeRectBottom = (float) eyeRect.br().y / faceROI.rows();

            // 상대 좌표로 변환된 눈의 좌표에 빨간색 사각형 그리기
            Point relativeEyeRectTl = new Point(relativeEyeRectLeft * originalMatImg.cols(), relativeEyeRectTop * originalMatImg.rows());
            Point relativeEyeRectBr = new Point(relativeEyeRectRight * originalMatImg.cols(), relativeEyeRectBottom * originalMatImg.rows());
            Imgproc.rectangle(originalMatImg, relativeEyeRectTl, relativeEyeRectBr, new Scalar(255, 0, 0), 2);
        }


    }



    private void recognizeFace(ImageView imageView) {
        try {
            System.out.println("before");
            System.loadLibrary("opencv_java4");
            System.out.println("hi");
            //haarcascade_frontalface_default 불러오기 - 얼굴객체 인식을 위한 머신러닝 데이터셋이다.
            Context context = getApplicationContext();
            InputStream is3 = context.getAssets().open("haarcascade_frontalface_default.xml");

            // InputStream을 앱의 캐시디렉토리의 temporary file로 복사
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is3.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            os.close();
            // /data/user/0/com.example.myapplication1/app_cascade/haarcascade_frontalface_default.xml
            // temporary file path를 이용해서 CascadeClassifier 생성
            CascadeClassifier faceCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());


            // 갤러리에서 불러온 이미지를 얼굴객체를 인식하기 위해 Mat 형식으로 변환
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            Mat originalMatImg = new Mat();
            Utils.bitmapToMat(bitmap, originalMatImg);

            // 이미지를 분석하기 위해 흑백이미지로 변환
            Mat gray2 = new Mat();
            Imgproc.cvtColor(originalMatImg, gray2, Imgproc.COLOR_RGBA2GRAY);

            MatOfRect faces;
            faces = new MatOfRect();

            // Mat 이미지형식으로부터 그 안에있는 사람들의 얼굴들을 인식
            faceCascade.detectMultiScale(gray2, faces, 1.3, 5);

            // 인식된 얼굴에 사각형으로 표시
            for (Rect rect : faces.toArray()) {
                System.out.println("인식된 얼굴 객체 좌표 :");
                System.out.println(rect);
                Imgproc.rectangle(originalMatImg, rect.tl(), rect.br(), new Scalar(255, 0, 0), 8);
            }

            // imageView2에 결과를 보여주기 위한 처리
            Bitmap resultBitmapImg = Bitmap.createBitmap(gray2.cols(), gray2.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(originalMatImg, resultBitmapImg);
            imageView.setImageBitmap(resultBitmapImg);
            System.out.println("hi2");
        } catch (IOException e) {
            System.out.println("hi3");
            e.printStackTrace();
        }
    }







    // btn_gallery : 갤러리 재선택
    public void onGalleryButtonClicked(View view) {

    }
}