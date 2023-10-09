package com.example.myapplication1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

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
    private SoundPool soundPool;
    private int soundId;
    private long sec;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplication(),photoFile.size()+"Create",Toast.LENGTH_SHORT).show();
            }
        });

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
        final Button change=findViewById(R.id.change);

        //ArrayList<ParcelableFile> photoFile=new ArrayList<>();   // 여기서 해볼게요. 맨위는 ㅂㄹ
        cacheDir =getCacheDir();


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

        //SoundPool 초기화

        soundPool=new SoundPool.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                )
                .setMaxStreams(1)
                .build();
        soundId=soundPool.load(this,R.raw.sound3,1);


    } //OnCreate()

    @Override
    protected void onStart(){


        super.onStart();
        for(ParcelableFile file:photoFile){
            File photo=new File(file.getPath());

            if(photo.exists()){
                boolean deleted=photo.delete();
                if(deleted){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"삭제성공",Toast.LENGTH_SHORT).show();}

                    });
                }
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"Start",Toast.LENGTH_SHORT).show();
            }
        });




    } // onStart()

    @Override
    protected void onResume(){
        super.onResume();

        photoFile.clear();  // clear

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"Resume",Toast.LENGTH_SHORT).show();}

        });


    }

    @Override
    protected void onPause(){
        super.onPause();


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"onPause",Toast.LENGTH_SHORT).show();}

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
        final LinearLayout dialog_layout=findViewById(R.id.dialog_layout);
        final Button timer=findViewById(R.id.timer);
        final ToggleButton tb1=findViewById(R.id.toggleButton1);
        final ToggleButton tb2=findViewById(R.id.toggleButton2);
        final ToggleButton tb3=findViewById(R.id.toggleButton3);

        dialog_layout.setVisibility(View.INVISIBLE);
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
        timer.setOnClickListener(view ->{
            if(dialog_layout.getVisibility()==View.INVISIBLE){
                dialog_layout.setVisibility(View.VISIBLE);
            }
            else {
                dialog_layout.setVisibility(View.INVISIBLE);
            }

        }); // timer 클릭리스너

        tb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    tb2.setChecked(false);
                    tb3.setChecked(false);
                    sec=0;
                }
            }
        });  // tb1 이벤트처리

        tb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    tb1.setChecked(false);
                    tb3.setChecked(false);
                    sec=3000;
                }
            }
        });  // tb2 이벤트처리

        tb3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    tb1.setChecked(false);
                    tb2.setChecked(false);
                    sec=7000;
                }
            }
        });  // tb3 이벤트처리

        capture.setOnClickListener(view -> {
            startTimer(sec,imageCapture);

        }); //리스너 익명함수로도 사용가능



    }
    //bindPreview 함수 구현


    public void startGifCapture(ImageCapture imageCapture) {

        mCameraExecutor = Executors.newSingleThreadExecutor();

        final int[] photoCount = {0}; // 사진을 찍은 횟수를 추적하는 변수

        mCameraExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    File ff = new File(cacheDir, "image_" + (i + 1) + ".jpg");
                    ParcelableFile parcelableFile = new ParcelableFile(ff.getAbsolutePath());
                    photoFile.add(parcelableFile);

                    ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(ff)
                            .build();

                    imageCapture.takePicture(outputFileOptions, mCameraExecutor, new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            photoCount[0]++; // 사진을 찍은 횟수를 증가시킴

                            if (photoCount[0] == 10) {
                                // 사진을 10장 찍었을 때만 gifViewer 액티비티를 엽니다
                                Intent intentPic = new Intent(getApplicationContext(), gifViewer.class);
                                intentPic.putParcelableArrayListExtra("photoFile", photoFile);
                                startActivity(intentPic);
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                        }
                    });
                }
            }
        });
    }

    public void startTimer(final long sec,ImageCapture imageCapture){
        //타이머 초기화
        if(countDownTimer!=null) countDownTimer.cancel();
        countDownTimer=new CountDownTimer(sec,1000){

            @Override
            public void onTick(long millisUntilFinished){
                //1초마다 소리재생
                soundPool.play(soundId,1.0f, 1.0f, 1, 0, 1.0f);
            }

            @Override
            public void onFinish(){
                startGifCapture(imageCapture);
            }
        };
        countDownTimer.start();
    } // startTimer 메서드








} //상위 public class

