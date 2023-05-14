package com.example.myapplication1;

import androidx.annotation.NonNull;
 import androidx.appcompat.app.AppCompatActivity;
 import android.Manifest;
 import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
 import android.graphics.Bitmap;
 import android.media.MediaScannerConnection;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Environment;
 import android.util.Log;
 import android.view.View;
 import android.widget.Button;
 import android.widget.Toast;
 import com.example.myapplication1.gifencoder.AnimatedGifEncoder;
//CameraInfo, CameraControl 사용
 import androidx.camera.core.Camera;
 import androidx.camera.core.CameraSelector;
 import androidx.camera.core.ImageCapture;
 import androidx.camera.core.ImageCaptureException;
 import androidx.camera.core.ImageProxy;
 import androidx.camera.core.Preview;
 import androidx.camera.lifecycle.ProcessCameraProvider;
 import androidx.camera.view.PreviewView;
 import androidx.core.app.ActivityCompat;
 import androidx.core.content.ContextCompat;
 import androidx.lifecycle.LifecycleOwner;
 import com.google.common.util.concurrent.ListenableFuture;

 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;


public class CameraActivity extends AppCompatActivity {

    private static final String TAG="My T";
    public ExecutorService mCameraExecutor = Executors.newSingleThreadExecutor();
    final List<Bitmap> mBitmapList = new ArrayList<>();
    private int mPictureCount = 0;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //PermissionCheck

        int cameraPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int readPermissionCheck= ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermissionCheck= ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(cameraPermissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            //100 REQUEST_CAMERA_PERMISSION
        }

        //저장소 접근권한
        if (readPermissionCheck!= PackageManager.PERMISSION_GRANTED || writePermissionCheck!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
            //200 STORAGE_PERMISSION_REQUEST_CODE
        }


        //setContentView 이후 실행해야하는 xml요소(view) 불러오기
        //없어도 될 것 같은
        final PreviewView previewView=findViewById(R.id.previewView);
        final Button change=findViewById(R.id.change);


        //CameraProvider 요청
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

        //preview 구현 - 1)cameraProvider 요청
        cameraProviderFuture=ProcessCameraProvider.getInstance(this);
        //사진을 찍었으면 다음 액티비티 불러야함!!!
        //preview 구현 - 2)cameraProvider 초기화 성공여부 확인
        cameraProviderFuture.addListener(() -> {
            try {
                //cameraProvider 생성
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                //미리보기 preview와 cameraProvider binding
                bindPreview(cameraProvider);


            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

        //change 버튼클릭 시 전/후면 전환
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change camera_back/front
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        //Builder 인자에 아무것도 없으니, 필수인자는 없는것
                        //빌더 객체 생성 후 변경불가능상태
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();


            }
        });






        //capture 버튼 클릭 시 촬영








    } //OnCreate()

    // btn_gallery click event
    public void onBtnGalleryClicked(View view)
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public void bindPreview( @NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()//빌더클래스 생성자로 빌더객체 생성
                .build(); //객체생성 후 돌려준다.

        final PreviewView previewView=findViewById(R.id.previewView);//previewView를 바로 넣을 수 없어서.....


        CameraSelector cameraSelector = new CameraSelector.Builder()
                //Builder 인자에 아무것도 없으니, 필수인자는 없는것
                //빌더 객체 생성 후 변경불가능상태
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageCapture imageCapture=new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.unbindAll();
        //반환된 camera 객체의 메서드 2개: CameraControl->ListenableFuture, CameraInfo
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageCapture);

        Button capture = findViewById(R.id.capture);
        capture.setOnClickListener(view -> {
            startGifCapture(imageCapture);
        });




    }
    //bindPreview 함수 구현
    public void startGifCapture(ImageCapture imageCapture){

        mCameraExecutor = Executors.newSingleThreadExecutor();

        mCameraExecutor.execute(new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(),"이제 takepicture 실행",Toast.LENGTH_SHORT).show();
                    imageCapture.takePicture(mCameraExecutor,new ImageCapture.OnImageCapturedCallback() {

                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            super.onCaptureSuccess(image);



                            // image.close(); // 해야하나? 아니라네요
                        } //OnCaptureSuccess 함수

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "사진 촬영 실패", exception);
                        }


                    }); //콜백메서드 통한 takePicture함수 구현


                }//for 반복문
            } //run함수 구현
        }); //excute 함수


    }     // startGifCapture 함수 구현







} //상위 public class

