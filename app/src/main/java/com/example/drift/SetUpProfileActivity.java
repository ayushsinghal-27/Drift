package com.example.drift;

import android.app.ComponentCaller;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.drift.databinding.ActivitySetUpProfileBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class SetUpProfileActivity extends AppCompatActivity {

    ActivitySetUpProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    Uri selectedImage;

    ProgressDialog dialog;
    private static boolean isMediaManagerInitialized = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetUpProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating profile...");
        dialog.setCancelable(false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // cloundinary storage
        if (!isMediaManagerInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dhkwakj0f");
            config.put("upload_preset", "Drift Chat App"); // 🔒 make sure this preset is UNSIGNED
            MediaManager.init(this, config);
            isMediaManagerInitialized = true;
        }


        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 45);

            }
        });

        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.nameBox.getText().toString();

                if(name.isEmpty()) {
                    binding.nameBox.setError("Please type a name");
                    return;
                }
                dialog.show();

                if (selectedImage != null) {
                    MediaManager.get().upload(selectedImage)
                            .unsigned("Drift Chat App") // Replace with your unsigned preset
                            .callback(new UploadCallback() {
                                @Override
                                public void onStart(String requestId) { }

                                @Override
                                public void onProgress(String requestId, long bytes, long totalBytes) { }

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    String imageUrl = resultData.get("secure_url").toString();

                                    String uid = auth.getUid();
                                    String phone = auth.getCurrentUser().getPhoneNumber();

                                    User user = new User(uid, name, phone, imageUrl);

                                    database.getReference()
                                            .child("users")
                                            .child(uid)
                                            .setValue(user)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    dialog.dismiss();
                                                    Intent intent = new Intent(SetUpProfileActivity.this, MainActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            });
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    dialog.dismiss();
                                    Toast.makeText(SetUpProfileActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onReschedule(String requestId, ErrorInfo error) { }
                            }).dispatch();

                } else {
                    String uid = auth.getUid();
                    String phone = auth.getCurrentUser().getPhoneNumber();

                    User user = new User(uid, name, phone, "No Image");

                    database.getReference()
                            .child("users")
                            .child(uid)
                            .setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(SetUpProfileActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                }

            }
        });




    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && data.getData() != null) {
            Uri uri = data.getData();
            selectedImage = uri;
            binding.imageView.setImageURI(uri);

            Map<String, Object> options = new HashMap<>();
            options.put("folder", "Profiles");
            options.put("upload_preset", "Drift Chat App");

            MediaManager.get().upload(uri)
                    .unsigned("Drift Chat App") // 🔁 Replace with your unsigned preset
                    .option("folder", "Profiles")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            // Optional: show progress dialog
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Optional: update progress
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = resultData.get("secure_url").toString();

                            // ✅ Save to Firebase
                            saveUserToFirebase(imageUrl);

                            HashMap<String, Object> obj = new HashMap<>();
                            obj.put("image", imageUrl);

                            database.getReference()
                                    .child("users")
                                    .child(FirebaseAuth.getInstance().getUid())
                                    .updateChildren(obj)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Optional: show success toast
                                        }
                                    });
                        }


                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(SetUpProfileActivity.this,
                                    "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            // Optional: handle reschedule
                        }
                    })
                    .dispatch();
        }
    }
    private void saveUserToFirebase(String imageUrl) {
        String uid = FirebaseAuth.getInstance().getUid();
        String phone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        String name = binding.nameBox.getText().toString();

        User user = new User(uid, name, phone, imageUrl);

        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uid)
                .setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        dialog.dismiss();
                        Intent intent = new Intent(SetUpProfileActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }


}