package com.example.myapplication1;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class photo extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        Intent intent = getIntent();
        if (intent != null) {
            byte[] byteArray = intent.getByteArrayExtra("image");

            if (byteArray != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                ImageView imageView = findViewById(R.id.image);
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}