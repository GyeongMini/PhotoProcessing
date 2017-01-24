package com.example.tacademy.photoprocessing;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.miguelbcr.ui.rx_paparazzo.RxPaparazzo;
import com.miguelbcr.ui.rx_paparazzo.entities.size.SmallSize;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import cn.pedant.SweetAlert.SweetAlertDialog;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView)findViewById(R.id.imageView);
    }
    SweetAlertDialog alert;
    public void onPhoto(View view)
    {
        alert =
                new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("사진선택")
                        .setContentText("사진을 선택할 방법을 고르세요!!")
                        .setConfirmText("카메라")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                onCamera();
                            }
                        })
                        .setCancelText("포토앨범")
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                onGallery();
                            }
                        })
        ;
        alert.setCancelable(true);
        alert.show();
    }
    public void onCamera()
    {
        // 크롭작업을 하기 위해 옵션 설정(편집)
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        options.setMaxBitmapSize(1024*1024*2); // 2MB 언더

        RxPaparazzo.takeImage(this)
                .size(new SmallSize())     // 사이즈(SmallSize, ScreenSize, OriginalSize,CustomMaxSize)
                .crop(options)              // 편집
                .useInternalStorage()       // 내부저장 (않쓰면 외부 공용 공간에 앱이름으로 생성됨)
                .usingCamera()              // 카메라 사용
                .subscribeOn(Schedulers.io())   // IO
                .observeOn(AndroidSchedulers.mainThread())  // 스레드 설정
                .subscribe(response -> {    // 결과 처리
                    // 실패 처리
                    if (response.resultCode() != RESULT_OK) {
                        //response.targetUI().showUserCanceled();
                        return;
                    }
                    Log.i("camera", response.data());
                    loadImage(response.data());
                    //response.targetUI().loadImage(response.data());
                });
    }

    public void onGallery()
    {
        // 크롭작업을 하기 위해 옵션 설정(편집)
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        options.setMaxBitmapSize(1024 * 1024 * 2); // 2MB 언더

        RxPaparazzo.takeImage(this)
                .size(new SmallSize())     // 사이즈(SmallSize, ScreenSize, OriginalSize,CustomMaxSize)
                .crop(options)              // 편집
                .useInternalStorage()       // 내부저장 (않쓰면 외부 공용 공간에 앱이름으로 생성됨)
                .usingGallery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    // See response.resultCode() doc
                    // 실패 처리
                    if (response.resultCode() != RESULT_OK) {
                        //response.targetUI().showUserCanceled();
                        return;
                    }
                    Log.i("PH", response.data());
                    loadImage(response.data());
                    //response.targetUI().loadImage(response.data());
                });
    }
    ImageView imageView;
    public void loadImage(String path) {
        alert.dismissWithAnimation();
        // 이미지뷰에 이미지를 세팅
        // url  = camera /data/user/0/com.example.tacademy.photoprocessing/files/PhotoProcessing/IMG-19012017_054534_081.jpeg
        // url  = gallery /data/user/0/com.example.tacademy.photoprocessing/files/PhotoProcessing/IMG-19012017_060459_105.jpg
        String url = "file://" + path;
        Picasso.with(this).setLoggingEnabled(true);
        Picasso.with(this).setIndicatorsEnabled(true);
        Picasso.with(this).invalidate(url);
        Picasso.with(this).load(url).into(imageView);

        //파일 삭제 ================================================================
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference delFileRef =
                storage.getReferenceFromUrl("gs://photoprocessing-a0bfa.appspot.com");
        delFileRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("kk","성공");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("kk","실패");
            }
        });
        //파일 삭제 =========================================================================
        /* // 나중에 기능을 나누면 된다.
        // 파일 업로드 ( image view, string,
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // 나무 기둥의 주소
        StorageReference storageRef
                = storage.getReferenceFromUrl("gs://photoprocessing-a0bfa.appspot.com");
        // 내 프로필 사진이 등록되는 최종 경로
        Uri uri = Uri.fromFile(new File(path));
        // 내 프로필 사진의 경로 gs:..../ profile/....
        String uploadName = "profile/" + uri.getLastPathSegment();
        // 기둥에 가지 등록
        StorageReference riversRef = storageRef.child("images/"+uri.getLastPathSegment());
        // 업로드
        UploadTask uploadTask = riversRef.putFile(uri);
        // 이벤트 등록 및 처리
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // 실패 -> 재시도를 하게 유도!!
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                // downloadUrl.toString() => 프로필 정보로 업데이트!!
                Log.i("KK", downloadUrl.toString());
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                // 진행율!!
                float reat = (taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount())*100.0f;
                Log.i("KK", "진행율:" + reat);
            }
        });
        */

    }
}
