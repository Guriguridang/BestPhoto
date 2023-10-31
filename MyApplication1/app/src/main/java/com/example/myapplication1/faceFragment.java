package com.example.myapplication1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class faceFragment extends AppCompatActivity {

    static {
        OpenCVLoader.initDebug();
    }

    private static final int PICK_IMAGE_REQUEST = 1;

    private Button btnSelectImage;
    private ImageView imageView;

    private CascadeClassifier eyeClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_faceroi);

        btnSelectImage = findViewById(R.id.btnSelectImage);
        imageView = findViewById(R.id.imageView);

        initializeOpenCV();

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSelectImageClick(view);
            }
        });
    }


    private void initializeOpenCV() {
        try {
            // Load the eye cascade classifier file from assets
            InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
            File cascadeDir = getDir("cascade", MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            eyeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            cascadeDir.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onSelectImageClick(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            processAndDisplayImage(selectedImageUri);
        }
    }

    private void processAndDisplayImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Convert Bitmap to Mat
            Mat imageMat = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4);
            org.opencv.android.Utils.bitmapToMat(bitmap, imageMat);

            // Convert to grayscale
            Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGBA2GRAY);

            // Detect eyes
            MatOfRect eyes = new MatOfRect();
            eyeClassifier.detectMultiScale(imageMat, eyes, 1.3, 5, 0, new Size(), new Size());

            // Draw rectangles around detected eyes
            for (org.opencv.core.Rect rect : eyes.toArray()) {
                Imgproc.rectangle(imageMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(255, 0, 0, 255), 2);
            }

            // Convert Mat back to Bitmap
            Bitmap processedBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(imageMat, processedBitmap);

            // Display the processed image
            imageView.setImageBitmap(processedBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
