package com.example.myapplication1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

//CameraInfo, CameraControl 사용
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
//import com.kakao.sdk.common.util.Utility;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import retrofit2.http.HEAD;


public class CameraActivity extends AppCompatActivity {

    //    public hash getKeyHash() {
//        return keyHash;
//    }
//public keyHash k=Utility.get
//    public hash keyHash=Utility.getKeyHash(this);
    private static final String TAG="My T";
    public ExecutorService mCameraExecutor = Executors.newSingleThreadExecutor();
    final List<Bitmap> mBitmapList = new ArrayList<>();
    private int mPictureCount = 0;
    private static final int PICK_IMAGE_REQUEST = 1;

    private int currentLensFacing;
    private Camera camera;
    ArrayList<ParcelableFile> photoFile=new ArrayList<>();   // 여기서 해볼게요. 맨위는 ㅂㄹ
    File cacheDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //cameraPermissionCheck onCreate()에서 해야함
        int cameraPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(cameraPermissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            //100 REQUEST_CAMERA_PERMISSION
        }

        //setContentView 이후 실행해야하는 xml요소(view) 불러오기
        //없어도 될 것 같은
        final PreviewView s=findViewById(R.id.previewView);


        //ArrayList<ParcelableFile> photoFile=new ArrayList<>();   // 여기서 해볼게요. 맨위는 ㅂㄹ
        cacheDir =getCacheDir();

        currentLensFacing=CameraSelector.LENS_FACING_BACK;

    } //OnCreate()

    @Override
    protected void onResume(){
        super.onResume();

        final Button change=findViewById(R.id.change);
        photoFile.clear();  // clear

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
                bindPreview(cameraProvider,currentLensFacing);

                change.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if(currentLensFacing==CameraSelector.LENS_FACING_BACK){
                            currentLensFacing=CameraSelector.LENS_FACING_FRONT;
                        }   //후면->전면
                        else{
                            currentLensFacing=CameraSelector.LENS_FACING_BACK;
                        }   //전면->후면

                        bindPreview(cameraProvider,currentLensFacing);

                    }
                });        //change 버튼클릭 시 전/후면 전환

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

    }

    @Override
    protected void onPause(){
        super.onPause();


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"1onPause",Toast.LENGTH_SHORT).show();}

        });

    }

    @Override
    protected void onStop(){
        super.onStop();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"1onStop",Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onStart(){
        super.onStart();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"1onStart",Toast.LENGTH_SHORT).show();
            }
        });

    }
    // 이미지 저장
    private void saveImageToGallery(Bitmap bitmap, String filename) {
        OutputStream outputStream = null;
        try {
            File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file = new File(directory, filename);
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            Toast.makeText(this, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "이미지 저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                    Toast.makeText(this, "이미지 갤러리 저장이 완료 되었습니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 갤러리 저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();

            }
        }
    }


    // 갤러리에서 사진 선택시 실행되는 함수
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();

            Intent intent = new Intent(this, gifViewer.class);
            intent.putExtra("imageuri", selectedImageUri.toString());
            intent.putExtra("hi", "hi");
            startActivity(intent);
        }
    }
    /*
    @ btn_gallery click event 정의
     */
    public void onBtnGalleryClicked(View view)
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }


    public void bindPreview( @NonNull ProcessCameraProvider cameraProvider, int currentLensFacing) {

        Preview preview = new Preview.Builder()//빌더클래스 생성자로 빌더객체 생성
                .build(); //객체생성 후 돌려준다.

        final PreviewView previewView=findViewById(R.id.previewView);//previewView를 바로 넣을 수 없어서.....

        CameraSelector cameraSelector = new CameraSelector.Builder()
                //Builder 인자에 아무것도 없으니, 필수인자는 없는것
                //빌더 객체 생성 후 변경불가능상태
                .requireLensFacing(currentLensFacing)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageCapture imageCapture=new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.unbindAll(); // 카메라와 연결된 usecase(미리보기, 사진/동영상 캡쳐..) 모두 해제
        //반환된 camera 객체의 메서드 2개: CameraControl->ListenableFuture, CameraInfo
        camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageCapture);

        Button capture = findViewById(R.id.capture);

        capture.setOnClickListener(view -> {


            startGifCapture(imageCapture);
        }); //리스너 익명함수로도 사용가능




    }
    //bindPreview 함수 구현


    public void startGifCapture(ImageCapture imageCapture){

        mCameraExecutor = Executors.newSingleThreadExecutor();

        mCameraExecutor.execute(new Runnable() {
            @Override
            public void run() {


                for (int i = 0; photoFile.size()!=10; i++) {
                    try {
                        Thread.sleep(300);


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    File ff=new File(cacheDir,"image_"+(i+1)+".jpg");
                    ParcelableFile parcelableFile=new ParcelableFile(ff.getAbsolutePath());
                    photoFile.add(parcelableFile);

                    ImageCapture.OutputFileOptions outputFileOptions= new ImageCapture.OutputFileOptions.Builder(ff)
                            .build();

                    imageCapture.takePicture(outputFileOptions, mCameraExecutor, new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {


//                            //오옷 1?
//                            if (photoFile.size()==10) {
//
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        Intent intentPic= new Intent(getApplicationContext(),gifViewer.class);
//                                        intentPic.putParcelableArrayListExtra("photoFile", photoFile);
//                                        Toast.makeText(getApplication(),photoFile.size()+"액 부름",Toast.LENGTH_SHORT).show();
//                                        startActivity(intentPic);
//                                    }
//                                });
//
//                            }  // #1

                            //여기 넣어보자
                        }  //onImageSaved
                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                        }
                    });  //callback메서드 통한 takepicture구현2

                    //여기 넣어볼게요
                    //오옷 2?
//                    if (photoFile.size()!=0) {
//
//
//                                Intent intentPic= new Intent(getApplicationContext(),gifViewer.class);
//
//                                intentPic.putParcelableArrayListExtra("photoFile", photoFile);
//                                startActivity(intentPic);
//
//
//
//                    }  // #2



                }//for 반복문


                //오옷?
//                if (photoFile.size()==10) {
//
//                    Intent intentPic= new Intent(getApplicationContext(),gifViewer.class);
//
//                    intentPic.putParcelableArrayListExtra("photoFile", photoFile);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplication(),photoFile.size()+"액 부름",Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                    startActivity(intentPic);
//
//                }





            } //run함수 구현
        }); //excute 함수

        // # 3 여기에
        if (photoFile.size()==10) {

            Intent intentPic= new Intent(getApplicationContext(), gifViewer.class);

            intentPic.putParcelableArrayListExtra("photoFile", photoFile);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplication(),photoFile.size()+"액 부름",Toast.LENGTH_SHORT).show();
                }
            });
            startActivity(intentPic);

        }


    }     // startGifCapture 함수 구현








} //상위 public class

