package android.com.cameraframes;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.SurfaceHolder;

import java.io.FileOutputStream;
import java.io.IOException;

public class CameraPreview extends Application implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private Camera mCamera = null;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private String NowPictureFileName;
    private Boolean TakePicture = false;
    private static Context mContext;
    private int imageFormat;

//    public static Context getContext() {
//        return mContext;
//    }
//
//    public static void setContext(Context mContext) {
//        CameraPreview.mContext = mContext;
//    }

    public CameraPreview(int PreviewlayoutWidth, int PreviewlayoutHeight, Context context)
    {
        PreviewSizeWidth = PreviewlayoutWidth;
        PreviewSizeHeight = PreviewlayoutHeight;
        this.mContext = context;
    }

    @Override
//    public void onPreviewFrame(byte[] data, Camera camera) {
//        // TODO Auto-generated method stub
//        Camera.Parameters parameters = camera.getParameters();
//        parameters.setPreviewFormat(ImageFormat.NV21);
//        Camera.Size size = parameters.getPreviewSize();
//        YuvImage image = new YuvImage(data, ImageFormat.NV21,
//                size.width, size.height, null);
//        Rect rectangle = new Rect();
//        rectangle.bottom = size.height;
//        rectangle.top = 0;
//        rectangle.left = 0;
//        rectangle.right = size.width;
//        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
//        image.compressToJpeg(rectangle, 100, out2);
//        DataInputStream in = new DataInputStream();
//        in.write(out2.toByteArray());
//
//    }


//    public void onPreviewFrame(byte[] data, Camera camera)
//    {
//        Parameters parameters = camera.getParameters();
//        imageFormat = parameters.getPreviewFormat();
//        if (imageFormat == ImageFormat.NV21)
//        {
//            Rect rect = new Rect(0, 0, PreviewSizeWidth, PreviewSizeHeight);
//            YuvImage img = new YuvImage(data, ImageFormat.NV21, PreviewSizeWidth, PreviewSizeHeight, null);
//            OutputStream outStream = null;
//            File file = new File("test");
//            try
//            {
//                outStream = new FileOutputStream(file);
//                img.compressToJpeg(rect, 100, outStream);
//                outStream.flush();
//                outStream.close();
//            }
//            catch (FileNotFoundException e)
//            {
//                e.printStackTrace();
//            }
//            catch (IOException e)
//            {
//                e.printStackTrace();
//            }
//        }
//    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        Bitmap bitmap = Bitmap.createBitmap(PreviewSizeWidth, PreviewSizeHeight, Bitmap.Config.ARGB_8888);
        Allocation bmData = renderScriptNV21ToRGBA888(
                mContext,
                PreviewSizeWidth,
                PreviewSizeHeight,
                data);
        bmData.copyTo(bitmap);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public Allocation renderScriptNV21ToRGBA888(Context context, int width, int height, byte[] nv21) {
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        return out;
    }


    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3)
    {
        Parameters parameters;

        parameters = mCamera.getParameters();
        // Set the camera preview size
        parameters.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);
        // Set the take picture size, you can set the large size of the camera supported.
        parameters.setPictureSize(PreviewSizeWidth, PreviewSizeHeight);

        // Turn on the camera flash.
        String NowFlashMode = parameters.getFlashMode();
        if ( NowFlashMode != null )
            parameters.setFlashMode(Parameters.FLASH_MODE_ON);
        // Set the auto-focus.
        String NowFocusMode = parameters.getFocusMode ();
        if ( NowFocusMode != null )
            parameters.setFocusMode("auto");

        mCamera.setParameters(parameters);

        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0)
    {
        mCamera = Camera.open();
        try
        {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
            mCamera.setPreviewCallback(this);
        }
        catch (IOException e)
        {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    // Take picture interface
    public void CameraTakePicture(String FileName)
    {
        TakePicture = true;
        NowPictureFileName = FileName;
        mCamera.autoFocus(myAutoFocusCallback);
    }

    // Set auto-focus interface
    public void CameraStartAutoFocus()
    {
        TakePicture = false;
        mCamera.autoFocus(myAutoFocusCallback);
    }


    //=================================
    //
    // AutoFocusCallback
    //
    //=================================
    AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback()
    {
        public void onAutoFocus(boolean arg0, Camera NowCamera)
        {
            if ( TakePicture )
            {
                NowCamera.stopPreview();//fixed for Samsung S2
                NowCamera.takePicture(shutterCallback, rawPictureCallback, jpegPictureCallback);
                TakePicture = false;
            }
        }
    };
    ShutterCallback shutterCallback = new ShutterCallback()
    {
        public void onShutter()
        {
            // Just do nothing.
        }
    };

    PictureCallback rawPictureCallback = new PictureCallback()
    {
        public void onPictureTaken(byte[] arg0, Camera arg1)
        {
            // Just do nothing.
        }
    };

    PictureCallback jpegPictureCallback = new PictureCallback()
    {
        public void onPictureTaken(byte[] data, Camera arg1)
        {
            // Save the picture.
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,data.length);
                FileOutputStream out = new FileOutputStream(NowPictureFileName);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    };

}