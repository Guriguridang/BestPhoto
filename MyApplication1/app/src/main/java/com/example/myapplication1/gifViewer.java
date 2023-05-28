package com.example.myapplication1;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

// add
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
// import com.bumptech.glide.gifdecoder.GifDecoder;
//import com.bumptech.glide.load.resource.gif.GifDrawable;
import pl.droidsonroids.gif.GifDrawable;

public class gifViewer extends AppCompatActivity {
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_viewer);
        imageView = findViewById(R.id.m_imageView);

        // Intent에서 전달받은 이미지 URI를 가져옴
        String imageUriString = getIntent().getStringExtra("imageUri");
        Uri imageUri = Uri.parse(imageUriString);

        // 이미지 URI를 ImageView에 설정하여 gif 이미지 표시
        Glide.with(this).asGif().load(imageUri).into(imageView);

        // 프레임 추출 버튼 클릭 이벤트 처리
        Button extractFramesButton = findViewById(R.id.btnExtractFrames);
        extractFramesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(gifViewer.this, "프레임 추출을 시작합니다.", Toast.LENGTH_SHORT).show();
                extractFrames();
            }
        });
    }

    // "재촬영" 버튼 클릭 이벤트 처리
    public void onCaptureButtonClicked(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
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
