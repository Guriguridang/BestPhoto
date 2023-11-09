package com.example.myapplication1;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.photo.Photo.MIXED_CLONE;
import static org.opencv.photo.Photo.NORMAL_CLONE;
import static org.opencv.photo.Photo.NORMCONV_FILTER;
import static org.opencv.photo.Photo.seamlessClone;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.photo.Photo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class FaceSwapActivity extends AppCompatActivity {

    ImageView imageView;
    ImageView imageView2;

    MatOfRect faces;
    MatOfRect faces2;

    Mat sourceImg;
    Mat tmpSourceImg;
    Mat targetImg;

    int len1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_swap);

        imageView = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);

        try{
            //이미지 셋팅
            // 이미지 불러오지 못한다고 뜨지만, 실상 결과 보면 잘 불러와짐
            InputStream is = getAssets().open("s1.jpeg");
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            imageView.setImageBitmap(bitmap);

            //is = getAssets().open("target_image2.jpg");
            is = getAssets().open("t1.jpeg");
            bitmap = BitmapFactory.decodeStream(is);

            imageView2.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }



        // opencv 셋팅
        System.loadLibrary("opencv_java4");

        // Load source and target images
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        sourceImg = new Mat();
        Utils.bitmapToMat(bitmap, sourceImg);


        Bitmap bitmap2 = ((BitmapDrawable) imageView2.getDrawable()).getBitmap();
        targetImg = new Mat();
        Utils.bitmapToMat(bitmap2, targetImg);

        //haarcascade_frontalface_default 불러오기 - 얼굴객체 인식을 위한 머신러닝 데이터셋이다.
        CascadeClassifier faceDetector;
        try {
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
            faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());

            // 이미지를 분석하기 위해 흑백이미지로 변환
            Mat gray = new Mat();
            Imgproc.cvtColor(sourceImg, gray, Imgproc.COLOR_RGBA2GRAY);

            Mat gray2 = new Mat();
            Imgproc.cvtColor(targetImg, gray2, Imgproc.COLOR_RGBA2GRAY);

            faces = new MatOfRect();

            faces2 = new MatOfRect();


            // Mat 이미지형식으로부터 그 안에있는 사람들의 얼굴들을 인식
            faceDetector.detectMultiScale(gray, faces, 1.11, 5);

            tmpSourceImg = sourceImg.clone();
            for (Rect rect : faces.toArray()) {
                Imgproc.rectangle(tmpSourceImg, rect.tl(), rect.br(), new Scalar(255, 255, 255), 8);
            }

            faceDetector.detectMultiScale(gray2, faces2, 1.11, 5);

            len1 = faces2.toArray().length;

            // 이미지뷰에 표시
            Bitmap bitmap4 = Bitmap.createBitmap(tmpSourceImg.cols(), tmpSourceImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmpSourceImg, bitmap4);
            imageView.setImageBitmap(bitmap4);

        }
            catch (IOException e) {
                            System.out.println("hi3");
                            e.printStackTrace();
                        }


        // 갤러리 버튼
        Button button2 = findViewById(R.id.button2);

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });


        Button button_swap = findViewById(R.id.btn);
        button_swap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                }
        });

        //이미지뷰를 터치를 했을때 작동하는 메서드
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                float curX = event.getX();  //터치한 곳의 X좌표
                float curY = event.getY();  // Y좌표

                // 현재 이미지뷰의 인식된 얼굴이 터치되었을 경우 스왑함수 호출
                for(int i=0; i<len1; i++) {
                    Rect s = faces.toArray()[i];

                    if( (s.y <= curY) && (curY <= (s.y + s.height)) && (s.x <= curX) && (curX <= (s.x + s.width))) {

                        // 인덱싱작업: 디폴트이미지뷰의 얼굴중 누구의 얼굴인지 판별.
                        int sourceX = s.x;
                        int sourceY = s.y;
                        int subT = 10000;
                        int idx=0;

                        for (int j = 0; j < len1; j++) {
                            Rect targetTmp = faces2.toArray()[j]; // Get the target face rectangle

                            int targetX = targetTmp.x;
                            int targetY = targetTmp.y;
                            int sub = Math.abs(sourceX - targetX) + Math.abs(sourceY - targetY);
                            if (sub < subT) {
                                subT = sub;
                                idx = j;
                            }
                        }

                        // 스왑 핵심함수 호출
                        // 여기서 idx는 디폴트이미지에서의 얼굴 인덱스이다.
                        // 이유: 얼굴인식이 되면 얼굴의 좌표순대로 저장되지 않고 지멋대로 저장되기 때문.
                        swapOne(s, idx);
                    }
                }

                return true;
            }

            public void swapOne(Rect r, Integer i) {

                Rect sourceFaceRect = r;
                Rect targetFaceRect = faces2.toArray()[i];

                // 얼굴 크기 조정
                // Expand the detected region
                int xOffset = -30; // Adjust these values as needed to control the size
                int yOffset = -30;
                int widthOffset = 60;
                int heightOffset = 60;

                // Update the coordinates of the rectangle to make it larger
                sourceFaceRect.x += xOffset;
                sourceFaceRect.y += yOffset;
                sourceFaceRect.width += widthOffset;
                sourceFaceRect.height += heightOffset;

                // Ensure the region is within the image boundaries
                sourceFaceRect.x = Math.max(sourceFaceRect.x, 0);
                sourceFaceRect.y = Math.max(sourceFaceRect.y, 0);
                sourceFaceRect.width = Math.min(sourceFaceRect.width, sourceImg.cols() - sourceFaceRect.x);
                sourceFaceRect.height = Math.min(sourceFaceRect.height, sourceImg.rows() - sourceFaceRect.y);

                // Update the coordinates of the rectangle to make it larger
                targetFaceRect.x += xOffset;
                targetFaceRect.y += yOffset;
                targetFaceRect.width += widthOffset;
                targetFaceRect.height += heightOffset;

                // Ensure the region is within the image boundaries
                targetFaceRect.x = Math.max(targetFaceRect.x, 0);
                targetFaceRect.y = Math.max(targetFaceRect.y, 0);
                targetFaceRect.width = Math.min(targetFaceRect.width, targetImg.cols() - targetFaceRect.x);
                targetFaceRect.height = Math.min(targetFaceRect.height, targetImg.rows() - targetFaceRect.y);


                Mat sourceRoi = new Mat(sourceImg, sourceFaceRect);

                Size targetSize = new Size(targetFaceRect.width, targetFaceRect.height);
                Mat sourceRoiResized = new Mat();
                resize(sourceRoi, sourceRoiResized, targetSize);

                Mat mask = Mat.ones(sourceRoiResized.size(), CvType.CV_8U);
                mask.setTo(new Scalar(256,256,256));

                Point center = new Point(targetFaceRect.x + targetFaceRect.width / 2, targetFaceRect.y + targetFaceRect.height / 2);
                Mat cloned = new Mat();

                // Skipped 46 frames!  The application may be doing too much work on its main thread.
                // 이라는 경고가 뜬다.

                // 파이썬에서는 되지만 안드에서는 안되던 문제 해결의 핵심 코드.
                Imgproc.cvtColor(sourceRoiResized, sourceRoiResized, COLOR_RGBA2RGB, 3);

                seamlessClone(sourceRoiResized, targetImg, mask, center, cloned, NORMAL_CLONE);
                targetImg = cloned;


                Bitmap bitmap4 = Bitmap.createBitmap(targetImg.cols(), targetImg.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(targetImg, bitmap4);
                imageView2.setImageBitmap(bitmap4);
            }
        });
    }



    // 갤러리 버튼
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    imageView.setImageURI(uri);
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("hi", "hi");
                    startActivity(intent);
                }
                break;
        }
    }
}