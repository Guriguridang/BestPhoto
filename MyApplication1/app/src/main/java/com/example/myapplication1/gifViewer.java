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
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

// import com.bumptech.glide.gifdecoder.GifDecoder;
//import com.bumptech.glide.load.resource.gif.GifDrawable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import pl.droidsonroids.gif.GifDrawable;

// opencv

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


// add

// import com.bumptech.glide.gifdecoder.GifDecoder;
//import com.bumptech.glide.load.resource.gif.GifDrawable;

// opencv


public class gifViewer extends AppCompatActivity {
    private ImageView imageView;
    private LinearLayout llImagesContainer;

    private boolean isOpenCvLoaded = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_viewer);
        imageView = findViewById(R.id.m_imageView);
        HorizontalScrollView horizontalScrollView = findViewById(R.id.horizontalScrollView);
        llImagesContainer = findViewById(R.id.llImagesContainer);

        //=================
        // 촬영 intent를 받은 경우
        Intent intent=getIntent();
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

        // 10개의 이미지를 동적으로 추가
//        for (int i = 0; i < 10; i++) {
//            ImageView image = new ImageView(this);
//            // 이미지 설정
//            image.setImageResource(R.drawable.myimage); // 예시 이미지
//            // 이미지 크기 설정
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//            );
//
//            // 이미지 간격 설정
//            params.setMargins(10, 0, 10, 0);
//            // 이미지뷰에 크기 및 간격 설정 적용
//            image.setLayoutParams(params);
//            // 이미지뷰를 가로 스크롤뷰에 추가
//            llImagesContainer.addView(image);
//        }

        // 갤러리 인텐트 받은 경우
        // Intent에서 전달받은 이미지 URI를 가져옴
        String imageUriString = getIntent().getStringExtra("imageuri");
        System.out.println("uri ====" + imageUriString);

        //Uri imageUri = Uri.parse(imageUriString);
        // 이미지 URI를 ImageView에 설정하여 gif 이미지 표시
        //Glide.with(this).asGif().load(imageUri).into(imageView);




        // 프레임 추출 버튼 클릭 이벤트 처리(시연에 사용하지x)
        Button extractFramesButton = findViewById(R.id.btnExtractFrames);
        extractFramesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(gifViewer.this, "프레임 추출을 시작합니다.", Toast.LENGTH_SHORT).show();
                extractFrames();
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


    // "재촬영" 버튼 클릭 이벤트 처리 (@선영님)
    public void onCaptureButtonClicked(View view) {
        int currentLensFacing=getIntent().getIntExtra("currentLensFacing",0);
        Intent intent = new Intent(this, CameraActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("currentLensFacing",currentLensFacing);
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


            // 보안 취약 시작점
            // imageView2에 표시된 이미지를 웹에 전송한다.
//                Bitmap bitmapForSendToWeb = ((BitmapDrawable) imageView2.getDrawable()).getBitmap();
//                System.out.println("이미지 웹에 전송 시작");
//
//                new Thread(() -> {
//                    // 이미지 전송 함수 실행
//                    imagePostSend(bitmapForSendToWeb);
//                }).start();
//
//                System.out.println("이미지 웹에 전송 끝");
            System.out.println("hi2");
        } catch (IOException e) {
            System.out.println("hi3");
            e.printStackTrace();
        }
    }









    private void extractFrames() {
        Drawable drawable = imageView.getDrawable();
        Toast.makeText(this,drawable.getClass().toString(), Toast.LENGTH_SHORT).show();

        if (drawable instanceof GifDrawable) {
            GifDrawable gifDrawable = (GifDrawable) drawable;
            Toast.makeText(this,"debug here", Toast.LENGTH_SHORT).show();

            // 프레임 추출 후 나열할 LinearLayout
            LinearLayout frameLayout = findViewById(R.id.llButtons);
            frameLayout.removeAllViews();

            // 프레임 추출 및 ImageView로 나열
            int frameCount = gifDrawable.getNumberOfFrames();
            Toast.makeText(this, frameCount, Toast.LENGTH_SHORT).show();

            for (int i = 0; i < frameCount; i++) {
                gifDrawable.seekToFrame(i);
                Bitmap frameBitmap = gifDrawable.getCurrentFrame();
                ImageView frameImageView = new ImageView(this);
                frameImageView.setImageBitmap(frameBitmap);
                frameImageView.setScaleType(ImageView.ScaleType.FIT_XY);
                frameLayout.addView(frameImageView);
            }
        } else if (drawable != null) {
            // drawable is not null, but not GifDrawable
            Toast.makeText(this, "Drawable is not a Gif", Toast.LENGTH_SHORT).show();
        } else {
            // drawable is null
            Toast.makeText(this, "Drawable is null", Toast.LENGTH_SHORT).show();
        }
    }


    // btn_gallery : 프레임 추출할 gif 재선택
    public void onGalleryButtonClicked(View view) {

    }
}