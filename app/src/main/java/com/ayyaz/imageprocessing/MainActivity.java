package com.ayyaz.imageprocessing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ayyaz.imageprocessing.databinding.ActivityMainBinding;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    static {
        if (OpenCVLoader.initDebug()) {

            Log.d(TAG, "OpenCV connected");

        } else {

            Log.d(TAG, "OpenCV not connected");

        }
    }

    private ActivityMainBinding binding;
    private Bitmap imageBitmap, objectBitmap;
    private int x = 0, y = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imageView.setOnTouchListener(this::onImageViewTouch);

        binding.buttonImportImage.setOnClickListener(this::onClick);
        binding.buttonAddObject.setOnClickListener(this::onClick);
        binding.buttonMergeImages.setOnClickListener(this::onClick);
        binding.buttonSaveImage.setOnClickListener(this::onClick);

    }

    private void onClick(View v) {

        if (v.getId() == binding.buttonImportImage.getId()) {

            Intent intent = Intent.createChooser(FileUtils.createGetContentIntent().setType(FileUtils.MIME_TYPE_IMAGE), "Select image");
            startActivityForResult(intent, 101);

        }

        if (v.getId() == binding.buttonAddObject.getId()) {

            Intent intent = Intent.createChooser(FileUtils.createGetContentIntent().setType(FileUtils.MIME_TYPE_IMAGE), "Select image");
            startActivityForResult(intent, 102);

        }

        if (v.getId() == binding.buttonMergeImages.getId()) {

            if (imageBitmap != null && objectBitmap != null) {
                mergeImages();
                binding.imageViewObject.setImageURI(null);
                binding.imageViewObject.setVisibility(View.GONE);
                objectBitmap.recycle();
                objectBitmap = null;
            }

        }

        if (v.getId() == binding.buttonSaveImage.getId()) {

            if (imageBitmap != null) {

                if (checkStoragePermission()) {
                    if (saveImage()) {
                        Toast.makeText(MainActivity.this, "File saved successfully in download folder.", Toast.LENGTH_LONG).show();
                    }
                }

            }

        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                try {
                    Uri imageUri = intent.getData();

                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                    binding.imageView.setImageURI(imageUri);
                } catch (Exception e) {
                    Log.e(TAG, "onActivityResult: Exception: ", e);
                }
            }
        }

        if (requestCode == 102) {
            if (resultCode == RESULT_OK) {

                try {
                    Uri objectUri = intent.getData();

                    objectBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), objectUri);

                    binding.imageViewObject.setImageURI(objectUri);

                    binding.imageViewObject.setTag(objectUri);
                    binding.imageViewObject.setVisibility(View.VISIBLE);

                    binding.imageViewObject.setX(0);
                    binding.imageViewObject.setY(0);

                } catch (Exception e) {
                    Log.e(TAG, "onActivityResult: Exception: ", e);
                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 201) {
            if (checkStoragePermission()) {
                if (saveImage()) {
                    Toast.makeText(MainActivity.this, "File saved successfully in download folder.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean onImageViewTouch(View v, MotionEvent event) {

        if (binding.imageViewObject.getVisibility() == View.VISIBLE) {
            x = (int) event.getX();
            y = (int) event.getY();

            binding.imageViewObject.setX(event.getX());
            binding.imageViewObject.setY(event.getY());
        }

        return false;
    }

    private void mergeImages() {
        try {
            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, binding.imageView.getWidth(), binding.imageView.getHeight(), false);
            objectBitmap = Bitmap.createScaledBitmap(objectBitmap, binding.imageViewObject.getLayoutParams().width, binding.imageViewObject.getLayoutParams().height, false);

            Mat mat1 = new Mat(), mat2 = new Mat();
            Utils.bitmapToMat(imageBitmap, mat1);
            Utils.bitmapToMat(objectBitmap, mat2);

            mat2.copyTo(mat1.submat(y, mat2.rows() + y, x, mat2.cols() + x));

            Bitmap bitmap3 = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), imageBitmap.getConfig());

            Utils.matToBitmap(mat1, bitmap3);

            binding.imageView.setImageBitmap(bitmap3);

            imageBitmap.recycle();

            imageBitmap = bitmap3;

        } catch (Exception e) {
            Log.e(TAG, "Exception:", e);
        }

    }

    protected Boolean checkStoragePermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 201);
            return false;
        } else {
            return true;
        }
    }

    private Boolean saveImage() {
        File imageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS, System.currentTimeMillis() + ".jpg");

        if (imageFile.exists()) imageFile.delete();

        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            imageBitmap.recycle();
            imageBitmap = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}