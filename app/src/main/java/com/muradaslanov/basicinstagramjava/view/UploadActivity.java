package com.muradaslanov.basicinstagramjava.view;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.muradaslanov.basicinstagramjava.databinding.ActivityUploadBinding;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {

    private FirebaseStorage firebaseStorage;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;
    Bitmap selectedBitmap;
    Bitmap reducedBitmap;
    Uri imageData;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    private ActivityUploadBinding binding;
//    Bitmap selectedBitmap;
    ImageDecoder.Source source;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        firebaseStorage = FirebaseStorage.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = firebaseStorage.getReference();

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public void publishButton(View view){

        Uri reducedImage = getImageUri(this,reducedBitmap);

        String caption = binding.captionText.getText().toString().trim();

//        Universal Unique id
        UUID uuid = UUID.randomUUID();
//        Could be used for Randomized pictures
        String imagePath ="images/"+firebaseAuth.getCurrentUser().getEmail()+"/"+binding.captionText.getText().toString().trim()+".jpg";

        if(reducedBitmap != null){
            storageReference.child(imagePath).putFile(reducedImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(UploadActivity.this, "Your picture is uploaded, Congrats", Toast.LENGTH_SHORT).show();

//                    Download URL
                    StorageReference newReference = firebaseStorage.getReference(imagePath);
                    newReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();

                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            String email = user.getEmail();

                            HashMap<String, Object> postData = new HashMap<>();
                            postData.put("useremail",email);
                            postData.put("downloadurl",downloadUrl);
                            postData.put("caption",caption);
                            postData.put("date", FieldValue.serverTimestamp());

                            firebaseFirestore.collection("Posts").add(postData).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Intent intent = new Intent(UploadActivity.this,FeedActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });


                        }
                    });


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }

    }

    public void selectImage(View view){

        if(ContextCompat.checkSelfPermission(UploadActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
//           Denied
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission needed to go to the gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                        ask permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

                    }
                }).show();
            }else{
//                ask permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }else{
//            permission granted
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }

    }

    private void registerLauncher(){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                 if(result.getResultCode() == RESULT_OK){
                     Intent resultIntent = result.getData();
                     if(resultIntent != null){
                         imageData = resultIntent.getData();
//                         binding.imageView.setImageURI(imageData);

                            try{

                                if(Build.VERSION.SDK_INT >= 28){
                                    source = ImageDecoder.createSource(UploadActivity.this.getContentResolver(),imageData);
                                    selectedBitmap = ImageDecoder.decodeBitmap(source);
                                    reducedBitmap = compressImage(selectedBitmap,500);
                                    binding.imageView.setImageBitmap(reducedBitmap);
                                }else{
                                     selectedBitmap = MediaStore.Images.Media.getBitmap(UploadActivity.this.getContentResolver(),imageData);
                                     reducedBitmap = compressImage(selectedBitmap,500);
                                     binding.imageView.setImageBitmap(reducedBitmap);
                                }

                            }catch (Exception e){
                                e.printStackTrace();
                            }

                     }
                 }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
             if(result){

                 Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                 activityResultLauncher.launch(intentToGallery);

             }else{
                 Toast.makeText(UploadActivity.this, "Permission Required", Toast.LENGTH_SHORT).show();
             }
            }
        });

    }

    public Bitmap compressImage(Bitmap selectedImg,int maximumSize){
        int width = selectedImg.getWidth();
        int height = selectedImg.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if(bitmapRatio > 1){
//            landscape
            width = maximumSize;
            height = (int) (width/bitmapRatio);
        }else{
//            portrait
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }
        return selectedImg.createScaledBitmap(selectedImg,width,height,true);
    }


}