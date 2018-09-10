package camerademo.nqbaoptit.camerademo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import static android.Manifest.permission.CAMERA;

public class PreviewDemo extends Activity implements View.OnClickListener {

    static final int REQUEST_CODE_USE_CAMERA = 164;
    static final int REQUEST_CODE_WRITE_STORAGE = 165;
    static final int REQUEST_CODE_READ_STORAGE = 166;
    static final int RESULT_LOAD_IMAGE = 203;

    static Bitmap mutableBitmap;
    ImageView mImage;
    ImageView image;

    RelativeLayout rl_camera;
    RelativeLayout rl_image;

    // Copy stack overflow
    private SurfaceView preview = null;
    private SurfaceHolder previewHolder = null;
    private Camera camera = null;
    private boolean inPreview = false;

    // Bitmap lưu ảnh
    Bitmap bmp, itembmp; // không dùng tới
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    // File
    File imageFileName = null;
    File imageFileFolder = null;
    Bitmap bitmap; // dùng để lưu lại các điểm ảnh
    byte[] dataxxx;

    boolean FRONT_CAMERA_ON = false;

    private MediaScannerConnection msConn;
    // Android Button
    private Button mBtnCapture, mBtnRotateCamera, mBtnSave, mBtnGalery;
    private Button mBtnBackFromAlbum;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview);

        // kiểm tra quyền ứng dụng đầu tiên
        requestPermissions();

    }

    Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
        public void onPictureTaken(final byte[] data, final Camera camera) {

            // Khi camera chụp xong, dữ liệu sẽ được lưu dưới dạng byte, và đổ vào biến data này.
            //Nhiệm vụ cần làm là lưu cái biến data này thành file trong bộ nhớ
            //thì sẽ được 1 cái ảnh

            // biến dataxxx để lưu lại data (để mình tùy biến sử dụng chứ không lưu lại ngay)
            dataxxx = data;
        }
    };

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        @SuppressLint("LongLogTag")
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try {

                camera.setPreviewDisplay(previewHolder);

            } catch (Throwable t) {
                Log.e("DIEULINH",
                        "Exception in setPreviewDisplay()", t);
                Toast.makeText(PreviewDemo.this, t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(width, height,
                        parameters);

                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    camera.startPreview();
                    inPreview = true;
                } else {
                    Log.e("DIEULINH", "hong o day nay");
                }
            } else {
                Log.e("DIEULINH", "camera null!");
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            // no-op
        }
    };

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // CAMERA - permission not permission group
            if (checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CODE_USE_CAMERA);
            }
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // xin quyền dùng camera
        if (requestCode == REQUEST_CODE_USE_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Đã cấp quyền - không làm gì cả
                init();
            } else {
                Toast.makeText(this, "Vui lòng cấp quyền cho ứng dụng của bạn!", Toast.LENGTH_SHORT).show();
            }
        }

        // xin quyền lưu ảnh vào gallery
        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Đã cấp quyền - không làm gì cả
                init();
            } else {
                Toast.makeText(this, "Vui lòng cấp quyền cho ứng dụng của bạn!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void init() {

        anhXa();
        setClick();
        settingCamera();
    }

    private void settingCamera() {
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        previewHolder.setFixedSize(getWindow().getWindowManager().getDefaultDisplay().getWidth(),
                getWindow().getWindowManager().getDefaultDisplay().getHeight());

        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            FRONT_CAMERA_ON = true;
        } catch (RuntimeException e) {
            Log.e("onResume", e.getLocalizedMessage());
        }
    }

    private void setClick() {

        mBtnCapture.setOnClickListener(this);

        mBtnRotateCamera.setOnClickListener(this);

        mBtnSave.setOnClickListener(this);
        mBtnSave.setEnabled(false);
        mBtnSave.setVisibility(View.INVISIBLE);

        mBtnGalery.setOnClickListener(this);

        mBtnBackFromAlbum.setOnClickListener(this);
    }

    private void anhXa() {

        image = findViewById(R.id.image);
        preview = findViewById(R.id.surface);

        rl_camera = findViewById(R.id.rl_camera);

        mBtnCapture = findViewById(R.id.btn_capture);

        mBtnRotateCamera = findViewById(R.id.btn_rotatecamera);

        mBtnSave = findViewById(R.id.btn_save);

        mBtnGalery = findViewById(R.id.btn_gallery);

        mImage = findViewById(R.id.img_mImageView);

        mBtnBackFromAlbum = findViewById(R.id.btn_backFromAlbum);

        rl_image = findViewById(R.id.rl_image);
        rl_image.setVisibility(View.GONE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e("DIEULINH", "chay vao ham onResume");

    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                }
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;
                if (newArea > resultArea) {
                    result = size;
                }
            }
        }
        return result;
    }

    public String fromInt(int val) {
        return String.valueOf(val);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {

            // chụp ảnh
            case R.id.btn_capture: {

                chupAnh();
                break;
            }

            // chuyển camera
            case R.id.btn_rotatecamera: {

                reverseCamera();
                break;
            }

            // lưu ảnh
            case R.id.btn_save: {
                // Đầu tiên xin cấp quyền lưu ảnh
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // STORAGE - permission not permission group
                    if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        btnSaveOnClicked();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_STORAGE);
                    }
                } else {
                    btnSaveOnClicked();
                }
                break;
            }

            // mở album ảnh
            case R.id.btn_gallery: {
                // tương tự với lưu ảnh, ở đây cũng phải xin quyền truy cập storage trước
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // STORAGE - permission not permission group
                    if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        getImageFromAlbum();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_STORAGE);
                    }
                } else {
                    getImageFromAlbum();
                }
                break;
            }

            // từ album quay trở lại camera
            case R.id.btn_backFromAlbum: {
                rl_camera.setVisibility(View.VISIBLE);
                rl_image.setVisibility(View.GONE);
            }
        }
    }

    private void btnSaveOnClicked() {
        saveToGallery(dataxxx);
        camera.startPreview();
        mBtnCapture.setVisibility(View.VISIBLE);
        mBtnCapture.setEnabled(true);
        mBtnRotateCamera.setVisibility(View.VISIBLE);
        mBtnRotateCamera.setEnabled(true);
        mBtnSave.setVisibility(View.INVISIBLE);
        mBtnSave.setEnabled(false);
    }

    // chụp ảnh
    private void chupAnh() {
        camera.takePicture(null, null, photoCallback);
        inPreview = false;
        mBtnCapture.setVisibility(View.INVISIBLE);
        mBtnCapture.setEnabled(false);
        mBtnRotateCamera.setVisibility(View.INVISIBLE);
        mBtnRotateCamera.setEnabled(false);
        mBtnSave.setVisibility(View.VISIBLE);
        mBtnSave.setEnabled(true);
    }

    // đổi camera trước - sau
    private void reverseCamera() {
        if (inPreview) {
            camera.stopPreview();
        }

        camera.release();
        camera = null;

        if (FRONT_CAMERA_ON) {
            try {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                FRONT_CAMERA_ON = false;
                Log.e("DIEULINH", "Bat cam sau");
                camera.startPreview();
            } catch (RuntimeException e) {
                Log.e("DIEULINH", e.getLocalizedMessage());
            }
        } else {
            try {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                FRONT_CAMERA_ON = true;
                Log.e("DIEULINH", "Bat cam truoc");
                camera.startPreview();
            } catch (RuntimeException e) {
                Log.e("DIEULINH", e.getLocalizedMessage());
            }
        }

    }

    // lưu ảnh
    private void saveToGallery(final byte[] data) {
        //Khi bat dau bam nut save image, neu dung asynctask, hanfh dong sasve image se duoc dat trong do
        //dat trong do in backgroun
        //khi qua trinh luu dang duoc thuc hien, no se nhay vaof ham onPreExecute
        //Khi luu xong , nhay vao ham onPostExecute
//
//        new AsyncTask<Bitmap, Integer, String>() {
//            @Override
//            protected String doInBackground(Bitmap... bitmaps) {
//                //Giá trị đầu tiên trong asynctask, sẽ làm đối số của hàm này
//                //vì thế khi return ở dây, bắt buộc phải return về 1 string
//                //để đúng với giá trị đầu vào của hàm onPostExcute
//
//                String s = "DIEULINh";
//                return s;
//            }
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
////                Khi hamfdoInbackground đang được thực hiện, thì giá trị ở hàm này sẽ được cập nhật liên tục
//                //đối số có kiểu interger
//            }
//
//            @Override
//            protected void onPostExecute(String s) {
//                super.onPostExecute(s);
//                //Khi hàm doinbackgroun thực hiện xong, nó sẽ trả về trong đối số string s.
//            }
//        }.execute();

        new AsyncTask<byte[], String, String>() {
            @Override
            protected String doInBackground(byte[]... bytes) {
                bitmap = BitmapFactory.decodeByteArray(bytes[0], 0, bytes[0].length);

                // try convert bitmap to imageView
//                mImage.setImageBitmap(bitmap);
//                mImage.setVisibility(View.VISIBLE);

//                bitmap = ImageFilter.applyFilter(bitmap, ImageFilter.Filter.GRAY);

                Calendar c = Calendar.getInstance();
                String date = fromInt(c.get(Calendar.MONTH))
                        + fromInt(c.get(Calendar.DAY_OF_MONTH))
                        + fromInt(c.get(Calendar.YEAR))
                        + fromInt(c.get(Calendar.HOUR_OF_DAY))
                        + fromInt(c.get(Calendar.MINUTE))
                        + fromInt(c.get(Calendar.SECOND));

//                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures",
//                        date.toString() + ".jpg");

                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures",
                        "photo_0" + ".jpg");

                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "photo_0.jpg", "demo");

                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    //Nen ảnh dưới dạng jpg để làm giảm dung lượng ảnh
                    //Giảm chất lượng ảnh xuống còn 80%
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);

//                    ViewRootImpl$CalledFromWrongThreadException
//                    mImage.setImageBitmap(bitmap);
//                    mImage.setVisibility(View.VISIBLE);

                    Log.e("DIEULINH", bitmap.toString());
                    fos.flush();
                    fos.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("DIEULINH", "EXCEPTION: " + e);
                }
                return (file.getAbsolutePath());
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Log.e("DIEULINH", "FilePath: " + s);
                Log.e("DIEULINH", "data: " + data);
            }
        }.execute(data);

        //AsyncTask<Params, Progress, Result>
//Giá trị đầu tiên là cho vào hàm doinbackground
//giá trị thứ 2 là đối số của hàm onPreexecute
//Giá trị thú 3 là đối số của hàm onPostExcute

//VD:
//    class SavePhotoTask extends AsyncTask<Void, String, String> {
//        @Override
//        protected String doInBackground(Void... voids) {
//            mProgress = ProgressDialog.show(getApplicationContext(), "", "Saving Photo");
//            savePhoto(mutableBitmap);
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//            String onPostExecute = null;
//            Log.d(onPostExecute, "onPostExecute: " + s.toString());
//            mProgress.dismiss();
//        }
//    }
    }

    // lấy ảnh từ gallery
    private void getImageFromAlbum() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMAGE);
    }

    // lấy ảnh từ gallery
    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (reqCode) {
                case RESULT_LOAD_IMAGE:
                    try {
                        Log.e("DIEULINH", "onActivityResult: đã hiển thị ");

                        rl_camera.setVisibility(View.GONE);
                        rl_image.setVisibility(View.VISIBLE);

                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

//                         không được
//                        ImageFilter.applyFilter(selectedImage, ImageFilter.Filter.GRAY);

                        mImage.setImageBitmap(selectedImage);

                    } catch (FileNotFoundException e) {
                        Log.e("DIEULINH", e.getLocalizedMessage());
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
                    }
                    break;
            }

        } else {
            Toast.makeText(this, "request code != result ok", Toast.LENGTH_SHORT).show();
        }
    }
}


