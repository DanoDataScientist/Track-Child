package com.bmh.trackchild.Tools;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.app.AlertDialog;
import android.util.DisplayMetrics;



import com.bmh.trackchild.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class Images {
    public Uri outputFileUri;
    AppPermission appPermission ;

    public void openImageIntent(Activity currentActivity, int requestCode, String imgName) {
        // Determine Uri of camera image to save.
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + currentActivity.getResources().getString(R.string.app_name) + File.separator);
        root.mkdirs();

        final File sdImageMainDirectory = new File(root, imgName);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera.
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

        final PackageManager packageManager = currentActivity.getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }
        /**/
        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
        currentActivity.startActivityForResult(chooserIntent, requestCode);
    }

    public void getImageSource(final Activity currentActivity, final int requestCode, final String imgName) {
        appPermission = new AppPermission(currentActivity);
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
        builder.setTitle("Choose Image Source");

        builder.setItems(new CharSequence[]{"Gallery", "Camera"},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!appPermission.checkAndRequestPermissions( new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, StaticValues.ACTION_REQUEST_GALLERY)) {
                                        selectGalleryImage(currentActivity,requestCode);
                                    }
                                } else {
                                    selectGalleryImage(currentActivity,requestCode);
                                }
                                break;
                            case 1:
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!appPermission.checkAndRequestPermissions( new String[]{
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    }, StaticValues.ACTION_REQUEST_CAMERA)) {
                                        takePhotoByCamera(currentActivity,requestCode, imgName);
                                    }
                                } else {
                                    takePhotoByCamera(currentActivity,requestCode, imgName);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
        builder.create();
        builder.show();
    }

    public void selectGalleryImage(Activity currentActivity,int requestCode) {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");
        currentActivity.startActivityForResult(chooserIntent, requestCode);
    }

    public void takePhotoByCamera(Activity currentActivity,int requestCode, String imgName) {
        // Determine Uri of camera image to save.
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "MyDir" + File.separator);
        root.mkdirs();
        final File sdImageMainDirectory = new File(root, imgName);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        Intent cameraChooser = Intent.createChooser(captureIntent,"Capture photo");
        currentActivity.startActivityForResult(cameraChooser, requestCode);
    }

    public Uri closeImageIntent(Activity CurrentActivity, Intent data, String img_name) {
        final boolean isCamera;
        if (data == null) {
            isCamera = true;
        } else {
            final String action = data.getAction();
            if (action == null) {
                isCamera = false;
            } else {
                isCamera = action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
            }
        }

        Uri selectedImageUri;
        if (isCamera) {
            ///////////////////////////////////////////////
            selectedImageUri = outputFileUri;
            //////////////////////////////////////////////
        } else {
            selectedImageUri = data == null ? null : data.getData();
        }

        if (selectedImageUri == null) {
            final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "MyDir" + File.separator);
            root.mkdirs();
            final File sdImageMainDirectory = new File(root, img_name);
            selectedImageUri = getImageContentUri(CurrentActivity, sdImageMainDirectory);
        }
        return selectedImageUri;
    }

    public String UriToPath(Activity CurrentActivity, Uri ImageURI, Intent data) {
        if (ImageURI.getPath().contains(".")) {
            return ImageURI.getPath();
        } else if (ImageURI.getPath().contains(":")) {
            ImageURI = data.getData();

    /* now extract ID from Uri path using getLastPathSegment() and then split with ":"
    then call get Uri to for Internal storage or External storage for media I have used getUri()
    */
            String id = ImageURI.getLastPathSegment().split(":")[1];
            final String[] imageColumns = {MediaStore.Images.Media.DATA};
            final String imageOrderBy = null;
            Uri uri = getUri();
            String selectedImagePath = "path";

            Cursor imageCursor = CurrentActivity.managedQuery(uri, imageColumns,
                    MediaStore.Images.Media._ID + "=" + id, null, imageOrderBy);

            if (imageCursor.moveToFirst()) {
                selectedImagePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            return selectedImagePath.toString();
        } else {
            return convertMediaUriToPath(CurrentActivity, ImageURI);
        }
    }

    private Uri getUri() {
        String state = Environment.getExternalStorageState();
        if (!state.equalsIgnoreCase(Environment.MEDIA_MOUNTED))
            return MediaStore.Images.Media.INTERNAL_CONTENT_URI;

        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }

    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    public Drawable Uri_To_Drawable(Context context, Uri uri) {
        Drawable D = null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            D = Drawable.createFromStream(inputStream, uri.toString());
        } catch (Exception e) {

            e.printStackTrace();
        }
        return D;
    }

    public String decodeImage(Context context, Uri uri) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return new String(byteBuffer.toByteArray(), "UTF-8");
    }

    public byte[] readBytes(Context context, Uri uri) throws IOException {
        // this dynamically extends to take the bytes you read
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
        int charsRead;
        while ((charsRead = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, charsRead);
        }
        // and then we can return your byte array.

        byte[] items = builder.toString().getBytes();
        return builder.toString().getBytes();
    }

    public Bitmap HTTP_Request_Image(String imageURL) {
        // to get image profile
        Bitmap bitmap = null;
        try {
            URL imgUrl = new URL(imageURL);
            InputStream in = (InputStream) imgUrl.getContent();
            bitmap = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private String convertMediaUriToPath(Context context, Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    public float convertDpToPixel(Context activity, float dp) {
        Resources resources = activity.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    public float convertSPToPixel(Context activity,float spValue)
    {
        Resources r = activity.getResources();
        float px =spValue/(r.getDisplayMetrics().scaledDensity);

        return px;
    }

    /**
     * This method is responsible for solving the rotation issue if exist. Also scale the images to
     * 1024x1024 resolution
     *
     * @param context       The current context
     * @param selectedImage The Image URI
     * @return Bitmap image results
     * @throws IOException
     */
    public Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage) throws IOException {
        int MAX_HEIGHT = 1024;
        int MAX_WIDTH = 1024;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(context, img, selectedImage);
        return img;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     *                  method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        // Log.i("TAG", "Images ..calculateInSampleSize..height: " + height + " width: " + width);

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        //  Log.i("TAG", "Images ..calculateInSampleSize..inSampleSize: " + inSampleSize);
        return inSampleSize;
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        /*
        1	top	left side           >> ORIENTATION_NORMAL
        2	top	right side          >> ORIENTATION_FLIP_HORIZONTAL
        3	bottom	right side      >> ORIENTATION_ROTATE_180
        4	bottom	left side       >> ORIENTATION_FLIP_VERTICAL
        5	left side	top         >> ORIENTATION_TRANSPOSE
        6	right side	top         >> ORIENTATION_ROTATE_90
        7	right side	bottom      >> ORIENTATION_TRANSVERSE
        8	left side	bottom      >> ORIENTATION_ROTATE_270
        */
        //   Log.i("TAG", "Images ..rotateImageIfRequired..orientation: " + orientation);
        if (orientation == 0) {
            orientation = getOrientation(context, selectedImage);
            //      Log.i("TAG", "Images ..rotateImageIfRequired..orientation: " + orientation);
            if (orientation == 0) {
                return img;
            } else {
                return rotateImage(img, orientation);
            }
        } else {

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        }
    }

    private int getOrientation(Context context, Uri selectedImage) {
// rotate the Bitmap (there a problem with exif so we'll query the mediaStore for orientation

        Cursor cursor = context.getContentResolver().query(selectedImage,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);
        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
        return 0;
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        // Log.i("TAG", "Images ..rotateImage..img.getWidth(): " + img.getWidth() + " img.getHeight(): " + img.getHeight());

        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        if (img != null && !img.isRecycled()) {
            img.recycle();
            img = null;
        }
        return rotatedImg;
    }
}
