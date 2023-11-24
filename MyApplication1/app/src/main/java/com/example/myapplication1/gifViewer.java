package com.example.myapplication1;

import static org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.photo.Photo.NORMAL_CLONE;
import static org.opencv.photo.Photo.seamlessClone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
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

    private static final int PICK_IMAGE_REQUEST = 1;

    private LinearLayout llImagesContainer;

    private boolean isOpenCvLoaded = false;

    private float dX, dY;

    private Uri tmpUri;

    CascadeClassifier faceCascade;

    MatOfRect targetfaces;

    Mat targetImg;

    Integer numOfFace=0;

    double scaleFactor2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_viewer);
        imageView = findViewById(R.id.m_imageView);
        HorizontalScrollView horizontalScrollView = findViewById(R.id.horizontalScrollView);
        llImagesContainer = findViewById(R.id.llImagesContainer);

        Intent intent= getIntent();
        String action=intent.getAction();


        // opencv setting
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
            // /data/user/0/com.example.myapplication1/app_cascade/haarcascade_frontalface_default.xml
            // temporary file path를 이용해서 CascadeClassifier 생성
            faceCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }



        if ("com.example.ACTION_TYPE_1".equals(action)) {
            // 촬영
            ArrayList<ParcelableFile> photoFile = intent.getParcelableArrayListExtra("photoFile");
            if (photoFile.size()!=0){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(getApplicationContext(), "사진 "+photoFile.size()+" 장 찍힘", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            int j=0;
            for (ParcelableFile photo: photoFile){
                String imageUriString = photo.getAbsolutePath();
                //System.out.println("uri from 캡쳐 ====" + imageUriString);
                Uri imageUri = Uri.parse(imageUriString);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(getApplicationContext(), "싸이즈"+imageUri, Toast.LENGTH_SHORT).show();

                    }
                });
                ImageView image = new ImageView(this);
                // 이미지 설정
                image.setImageURI(imageUri); // 예시 이미지


                // 이미지 리사이징. 비율은 자동조절됨

//                int targetWidth = 1000;
//                int targetHeight = 600;
                // 원본보다 작게 유지
                int targetWidth = 1000; //  800이 딱 붙는것. 즉 좌우로 각각 100씩 여
                int targetHeight = 600;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imageUriString, options);

                // 현재 내 맥북은 960/1280
                // 갤탭은 1932/2576
                int imageWidth = options.outWidth;
                int imageHeight = options.outHeight;
//                System.out.println("imageHeight====" + imageHeight);
//                System.out.println("imageWidth====" + imageWidth);

                // 무조건 1이 나옴
                int scaleFactor = Math.min( (imageWidth / targetWidth), (imageHeight / targetHeight));


//                if (j==0) {
//                    // 맥북
//                    // 여기서 설정하는게 아닌듯?? 왜냐면 imageHeight는 원본 사진임.
//                    //scaleFactor2 = ((double)imageHeight / (double)targetHeight);
//                    System.out.println("scaleFactor ====" + scaleFactor2);
//                }

//                System.out.println("scaleFactor#####" + scaleFactor2);

                options.inJustDecodeBounds = false;
                options.inSampleSize = scaleFactor;

                Bitmap resizedBitmap = BitmapFactory.decodeFile(imageUriString, options);

                // 이미지뷰 크기 조정
                image.setLayoutParams(new LinearLayout.LayoutParams(targetWidth, targetHeight));

                // 리사이징된 이미지를 이미지뷰에 설정
                image.setImageBitmap(resizedBitmap);

                // LinearLayout에 이미지뷰 추가
                llImagesContainer.addView(image);


                //recognizeFace(image);
                image.setId(j);


                // n번 째 이미지 추출 이벤트
//                image.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Toast.makeText(getApplicationContext(), "이미지를 선택했습니다.", Toast.LENGTH_SHORT).show();
//
//                    }
//                });


                // 얼굴 인식
                // 갤러리에서 불러온 이미지를 얼굴객체를 인식하기 위해 Mat 형식으로 변환
                // 크기: 갤탭 기준 966, 1288
                Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
                Mat originalMatImg = new Mat();
                Utils.bitmapToMat(bitmap, originalMatImg);

                if (j==0) {
                    // 여기서 scaleFactor2 설정! for 좌표터치 싱크
                    // 1.66
                    scaleFactor2 = ((double)bitmap.getHeight() / (double)targetHeight);
                }


                // 이미지를 분석하기 위해 흑백이미지로 변환
                Mat gray = new Mat();
                Imgproc.cvtColor(originalMatImg, gray, Imgproc.COLOR_RGBA2GRAY);

                MatOfRect faces;
                faces = new MatOfRect();



                // Mat 이미지형식으로부터 그 안에있는 사람들의 얼굴들을 인식
                //faceCascade.detectMultiScale(gray, faces, 1.11, 5);
                faceCascade.detectMultiScale(gray, faces, 1.11, 13);


                // 첫번째 프레임은 디폴트이미지이기 때문에 전역으로 백업
//                if(j==0) {
//                    targetfaces = faces;
//                    targetImg = originalMatImg.clone(); // clone 해야되나?
//                    numOfFace = faces.toArray().length;
//                }

                // 얼굴 가장 많이 인식된 이미지가 디폴트가 되도록 수정
                if(faces.toArray().length > numOfFace) {
                    targetfaces = faces;
                    targetImg = originalMatImg.clone(); // clone 해야되나?
                    numOfFace = faces.toArray().length;
                }


                // 네모 안칠할 백업 이미지
                Mat backupMat = originalMatImg.clone();

                // 인식된 얼굴에 사각형으로 표시
                for (Rect rect : faces.toArray()) {
                    Imgproc.rectangle(backupMat, rect.tl(), rect.br(), new Scalar(255, 255, 255), 8);
                }



                // imageView2에 결과를 보여주기 위한 처리
                Bitmap resultBitmapImg = Bitmap.createBitmap(gray.cols(), gray.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(backupMat, resultBitmapImg);
                image.setImageBitmap(resultBitmapImg);


                // 터치이벤트
                image.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int action = event.getAction();
                        double curX = event.getX();  //터치한 곳의 X좌표
                        double curY = event.getY();  // Y좌표

                        // 이거 위에서 설정 되어야하는데 일로 오면 0으로 적용되는 문제
                        //numOfFace = 1;

                        if(action == event.ACTION_UP) { // 누르고 땠을 때만
                            //현재 이미지뷰의 인식된 얼굴이 터치되었을 경우 스왑함수 호출
                            for(int i=0; i<numOfFace; i++) {
                                Rect s = faces.toArray()[i];

                                // 얼굴의 우측 아랫부분을 눌러야만 스왑함수가 실행되는 오류가 있다..!
                                // 터치반응은 이미지의 좌우 약 100만큼 넘어가도 터치반응이 된다.
                                // Rect 객체는 아무래도 이미지부터 시작하는듯?
                                // y도 40정도 차이가 있는데, 단위의 차이인가..?
//                                System.out.println("scaleFactor ====" + scaleFactor2);
//                                System.out.println("터치좌표");
//                                System.out.println(curY);
//                                System.out.println(curX);
//                                System.out.println("수정후 터치좌표");


                                //curY = (curY*scaleFactor2);
                                //curX = ((curX-100)*scaleFactor2);
                                double nowCurY = (curY*scaleFactor2);
                                double nowCurX = ((curX-100)*scaleFactor2);

//                                System.out.println("수정후 터치좌표");
//                                System.out.println(nowCurY);
//                                System.out.println(nowCurX);
//
//                                System.out.println("얼굴좌표");
//                                System.out.println(s.y + "부터 " + (s.y + s.height));
//                                System.out.println(s.x + "부터 " + (s.x + s.width));

                                if( (s.y <= nowCurY) && (nowCurY <= (s.y + s.height)) && (s.x <= nowCurX) && (nowCurX <= (s.x + s.width))) {


                                    // 인덱싱작업: 디폴트이미지뷰의 얼굴중 누구의 얼굴인지 판별.
                                    int sourceX = s.x;
                                    int sourceY = s.y;
                                    int subT = 10000;
                                    int idx=0;

                                    for (int j = 0; j < numOfFace; j++) {
                                        Rect targetTmp = targetfaces.toArray()[j]; // Get the target face rectangle
                                        int targetX = targetTmp.x;
                                        //int targetY = targetTmp.y;
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
                                    swapOne(originalMatImg, s, idx);
                                }
                            }
                        }
                        return true;
                    }

                    public void swapOne(Mat originalMatImg, Rect r, Integer i) {

//                        System.out.println("############시작");
                        Rect sourceFaceRect = r;
                        Rect targetFaceRect = targetfaces.toArray()[i];

                        // 얼굴 크기 조정
                        // Expand the detected region
//                        int xOffset = -30; // Adjust these values as needed to control the size
//                        int yOffset = -30;
//                        int widthOffset = 60;
//                        int heightOffset = 60;
                        int xOffset = -80; // Adjust these values as needed to control the size
                        int yOffset = -80;
                        int widthOffset = 160;
                        int heightOffset = 160;

                        // Update the coordinates of the rectangle to make it larger
                        sourceFaceRect.x += xOffset;
                        sourceFaceRect.y += yOffset;
                        sourceFaceRect.width += widthOffset;
                        sourceFaceRect.height += heightOffset;

                        // Ensure the region is within the image boundaries
                        sourceFaceRect.x = Math.max(sourceFaceRect.x, 0);
                        sourceFaceRect.y = Math.max(sourceFaceRect.y, 0);

                        //원래
                        // sourceFaceRect.width = Math.min(sourceFaceRect.width, sourceImg.cols() - sourceFaceRect.x);
                        sourceFaceRect.width = Math.min(sourceFaceRect.width, originalMatImg.cols() - sourceFaceRect.x);
                        sourceFaceRect.height = Math.min(sourceFaceRect.height, originalMatImg.rows() - sourceFaceRect.y);

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


                        Mat sourceRoi = new Mat(originalMatImg, sourceFaceRect);

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
                        imageView.setImageBitmap(bitmap4);
//                        System.out.println("############끝");
                    }


                });

                j++;
                if (j==10) {
                    break;
                }
            }
        } else if ("com.example.ACTION_TYPE_2".equals(action)) {
            // 갤러리
            String imageUriString = getIntent().getStringExtra("imageuri");
//            System.out.println("uri ====" + imageUriString);

            if (imageUriString != null) {
                Uri imageUri = Uri.parse(imageUriString);
                Glide.with(this).load(imageUri).into(imageView);
            }
        }

        Bitmap bitmap4 = Bitmap.createBitmap(targetImg.cols(), targetImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(targetImg, bitmap4);
        imageView.setImageBitmap(bitmap4);

        Button btn_next = findViewById(R.id.btnNext);
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageView imgView=findViewById(R.id.m_imageView);
                Intent intent = new Intent(getApplicationContext(), photo.class);
                //startActivity(intent);
                try {
//                    if(imgView.getDrawable() instanceof BitmapDrawable){
//                        Bitmap bitmap=((BitmapDrawable) imgView.getDrawable()).getBitmap();
//                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                        bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
//                        byte[] byteArray = stream.toByteArray();
//
//                        intent.putExtra("img", byteArray);
//                        startActivity(intent);
//                    }

                    Bitmap bitmap = Bitmap.createBitmap(targetImg.cols(), targetImg.rows(), Bitmap.Config.ARGB_8888);

                    Utils.matToBitmap(targetImg, bitmap);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,95,stream);
                    byte[] byteArray = stream.toByteArray();

                    intent.putExtra("img", byteArray);
                    startActivity(intent);

                }
                catch (Exception e){
                    Toast.makeText(gifViewer.this,"byte=0",Toast.LENGTH_SHORT).show();
                    Toast.makeText(gifViewer.this,e.toString(),Toast.LENGTH_SHORT).show();
                    //System.out.println(e.toString());
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
//                System.out.println("인식된 얼굴 객체 좌표 :");
//                System.out.println(rect);
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


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();

            Intent intent = new Intent(this, gifViewer.class);
            intent.putExtra("imageuri", selectedImageUri.toString());
            intent.setAction("com.example.ACTION_TYPE_2");
            startActivity(intent);
        }
    }
}

//    public void onGalleryButtonClicked(View view)
//    {
//        Intent intent = new Intent();
//        intent.setType("image/*");
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
//    }
//}
