package com.example.myapplication1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

// add
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;


public class gifViewer extends AppCompatActivity {
    private ImageView imageView;
    private LinearLayout llImagesContainer;

    private boolean isOpenCvLoaded = false;

    private float dX, dY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_viewer);
        imageView = findViewById(R.id.m_imageView);
        HorizontalScrollView horizontalScrollView = findViewById(R.id.horizontalScrollView);
        llImagesContainer = findViewById(R.id.llImagesContainer);

        Intent intent= getIntent();
        String action=intent.getAction();

        if ("com.example.ACTION_TYPE_1".equals(action)) {
            // 촬영
            //=================
            // 촬영 intent를 받은 경우
            ArrayList<ParcelableFile> photoFile = intent.getParcelableArrayListExtra("photoFile");
            if (photoFile.size()!=0){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "사진 "+photoFile.size()+" 장 찍힘", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            int j=0;
            for (ParcelableFile photo: photoFile){
                String imageUriString = photo.getAbsolutePath();
                System.out.println("uri from 캡쳐 ====" + imageUriString);
                Uri imageUri = Uri.parse(imageUriString);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "싸이즈"+imageUri, Toast.LENGTH_SHORT).show();

                    }
                });
                ImageView image = new ImageView(this);
                // 이미지 설정
                image.setImageURI(imageUri); // 예시 이미지
                // 이미지 크기 설정
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                // 이미지 간격 설정
                params.setMargins(0, 0, 0, 0);
                // 이미지뷰에 크기 및 간격 설정 적용
                image.setLayoutParams(params);
                // 이미지뷰를 가로 스크롤뷰에 추가
                llImagesContainer.addView(image);

                //recognizeFace(image);
                image.setId(j);
                j++;
                if (j==10) {
                    break;
                }

                // n번 째 이미지 추출 이벤트
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getApplicationContext(), "이미지를 선택했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else if ("com.example.ACTION_TYPE_2".equals(action)) {
            // 갤러리
            String imageUriString = getIntent().getStringExtra("imageuri");
            System.out.println("uri ====" + imageUriString);

            if (imageUriString != null) {
                Uri imageUri = Uri.parse(imageUriString);
                Glide.with(this).load(imageUri).into(imageView);
            }
        }

        // 이미지 보정
        Button extractFramesButton = findViewById(R.id.btnExtractFrames);
        extractFramesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(gifViewer.this, "보정하기 선택", Toast.LENGTH_SHORT).show();
                extractEyes(imageView);
            }
        });

        Button btn_face = findViewById(R.id.btn_face);
        btn_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(gifViewer.this, "얼굴 인식 시작", Toast.LENGTH_SHORT).show();
                for(int i=0; i<9; i++) {
                    System.out.println("id ====" +llImagesContainer.findViewById(i).getId() );
                    ImageView view = llImagesContainer.findViewById(i);
                    recognizeFace(view);
                }
            }
        });
    }



    private void extractEyes(ImageView imageView) {
        try {
            System.loadLibrary("opencv_java4");
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // "재촬영" 버튼 클릭 이벤트 처리 (@선영님)
    public void onCaptureButtonClicked(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
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

    // btn_gallery : 프레임 추출할 gif 재선택
    public void onGalleryButtonClicked(View view) {

    }
}