package com.example.myapplication1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//CameraInfo, CameraControl 사용
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.myapplication1.databinding.ActivityCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;
//import com.kakao.sdk.common.util.Utility;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    public ExecutorService mCameraExecutor = Executors.newSingleThreadExecutor();
    private static final int PICK_IMAGE_REQUEST = 1;
    private int currentLensFacing=1;
    private Camera camera;
    ArrayList<ParcelableFile> photoFile=new ArrayList<>();
    ArrayList<ParcelableFile> flipFile=new ArrayList<>();
    File cacheDir;
    private SoundPool soundPool;
    private int soundId,soundId1;
    private long sec;
    private CountDownTimer countDownTimer;
    ActivityCameraBinding viewBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewBinding= ActivityCameraBinding.inflate(getLayoutInflater());
        Button galleryButton = findViewById(R.id.btn_gallery);
        galleryButton.setOnClickListener(this::onBtnGalleryClicked);


        //cameraPermissionCheck onCreate()에서 해야함
        int cameraPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(cameraPermissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            //100 REQUEST_CAMERA_PERMISSION
        }

        final PreviewView s=findViewById(R.id.previewView);
        final Button change=findViewById(R.id.change);
        final Button timer=findViewById(R.id.timer);

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
        soundId1=soundPool.load(this,R.raw.picture,1);


    } //OnCreate()


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
    public void onBtnGalleryClicked(View view)
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onStart(){


        super.onStart();
        for(ParcelableFile file:photoFile){
            File photo=new File(file.getAbsolutePath());

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
            }
        });

    } // onStart()

    @Override
    protected void onResume(){
        super.onResume();

        photoFile.clear();  // clear
        flipFile.clear();

    }

    @Override
    protected void onPause(){
        super.onPause();

    }

    @Override
    protected void onStop(){
        super.onStop();
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


    public void bindPreview( @NonNull ProcessCameraProvider cameraProvider, int currentLensFacing) {

        Preview preview = new Preview.Builder()//빌더클래스 생성자로 빌더객체 생성
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build(); //객체생성 후 돌려준다.

        final PreviewView previewView=findViewById(R.id.previewView);//previewView를 바로 넣을 수 없어서.....
        final LinearLayout dialog_layout=findViewById(R.id.dialog_layout);
        final Button timer=findViewById(R.id.timer);
        final ToggleButton tb1=findViewById(R.id.toggleButton1);
        final ToggleButton tb2=findViewById(R.id.toggleButton2);
        final ToggleButton tb3=findViewById(R.id.toggleButton3);
        final TextView textView=findViewById(R.id.textView2);
        final SeekBar seekBar=findViewById(R.id.seekBar);



        dialog_layout.setVisibility(View.INVISIBLE);
        CameraSelector cameraSelector = new CameraSelector.Builder()
                //Builder 인자에 아무것도 없으니, 필수인자는 없는것
                //빌더 객체 생성 후 변경불가능상태
                .requireLensFacing(currentLensFacing)
                .build();

        // Provider
        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        //Provider
        ImageCapture imageCapture=new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();



        cameraProvider.unbindAll(); // 카메라와 연결된 usecase(미리보기, 사진/동영상 캡쳐..) 모두 해제
        //반환된 camera 객체의 메서드 2개: CameraControl->ListenableFuture, CameraInfo
        camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageCapture);
        CameraInfo cameraInfo = camera.getCameraInfo();

        camera.getCameraControl().setZoomRatio(1.0f);
        seekBar.setMax(20);  //원래 30
        seekBar.setMin(10);
        seekBar.setProgress(10);
        textView.setText(String.format(Locale.KOREA,"%.1f",(cameraInfo.getZoomState().getValue().getZoomRatio())));



        //seekbar 리스너 등록
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){

                camera.getCameraControl().setZoomRatio(progress/10.0f);
                textView.setText(String.format(Locale.KOREA,"%.1f", progress/10.0f));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar){}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){}
        });



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
                    File flippedFile = new File(cacheDir, "flipped_image_" + (i + 1) + ".jpg");

                    ParcelableFile parcelableFile = new ParcelableFile(ff.getAbsolutePath());
                    photoFile.add(parcelableFile);

                    ParcelableFile flipparcebleeFile = new ParcelableFile(flippedFile.getAbsolutePath());
                    flipFile.add(flipparcebleeFile);

                    ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(ff)
                            .build();

                    imageCapture.takePicture(outputFileOptions, mCameraExecutor, new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            if(currentLensFacing==CameraSelector.LENS_FACING_FRONT){
                                Bitmap org=BitmapFactory.decodeFile(ff.getAbsolutePath());
                                Bitmap flip=flipBitmap(org);

                                try(FileOutputStream outputStream=new FileOutputStream(flippedFile)) {
                                    flip.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                                } catch (IOException e){
                                    e.printStackTrace();
                                }
                            }

                            photoCount[0]++; // 사진을 찍은 횟수를 증가시킴

                            if (photoCount[0] == 10) {
                                // 사진을 10장 찍었을 때만 gifViewer 액티비티를 엽니다

                                Intent intentPic = new Intent(getApplicationContext(), gifViewer.class);
                                if(currentLensFacing==CameraSelector.LENS_FACING_FRONT){
                                    intentPic.putParcelableArrayListExtra("photoFile", flipFile);
                                }
                                else {
                                    intentPic.putParcelableArrayListExtra("photoFile", photoFile);
                                }
                                intentPic.setAction("com.example.ACTION_TYPE_1");
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
    private Bitmap flipBitmap(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1); // Horizontal flip
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
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

                soundPool.play(soundId1,4.0f,4.0f,1,0,1.0f);
                startGifCapture(imageCapture);

            }
        };
        countDownTimer.start();
    } // startTimer 메서드


    public static class photoedit {
    }

    private void saveBitmapToFile(Bitmap bitmap, File file){
        try(FileOutputStream fos=new FileOutputStream(file)){
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
            fos.flush();
        } catch(IOException e){
            e.printStackTrace();
        }

    }
} //상위 public class