package android.com.cameraframes;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Runnable {
  private SurfaceHolder mHolder;
  private Camera mCamera;
  private String TAG = "";
  private boolean processImage = false;
  private int DEGREE = 90;
  private MainActivity m;

  public CameraPreview(final Context context, Camera camera) {
    super(context);
    m = (MainActivity) context;

    new Thread(this).start();

    mCamera = camera;
    Camera.Parameters mParam = mCamera.getParameters();
    mParam.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
    mCamera.setParameters(mParam);


    processImage = false;

    // Install a SurfaceHolder.Callback so we get notified when the
    // underlying surface is created and destroyed.
    mHolder = getHolder();
    mHolder.addCallback(this);
    // deprecated setting, but required on Android versions prior to 3.0
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


  }

  public void setCapture(boolean capture) {
    processImage = capture;
  }

  public void surfaceCreated(SurfaceHolder holder) {
    // The Surface has been created, now tell the camera where to draw the preview.
    try {
      mCamera.setPreviewDisplay(holder);
      mCamera.setPreviewCallback(this);
//      mCamera.setOneShotPreviewCallback(this);
      mCamera.setDisplayOrientation(DEGREE);
      mCamera.startPreview();

    } catch (IOException e) {
      Log.d(TAG, "Error setting camera preview: " + e.getMessage());
    }
  }

  private int i = 1;

  @Override
  public void onPreviewFrame(byte[] data, Camera camera) {
//    try {
//      Camera.Parameters parameters = camera.getParameters();
//      Camera.Size size = parameters.getPreviewSize();
//
//      YuvImage image = new YuvImage(rotateYUV420Degree90(data, size.width, size.height), parameters.getPreviewFormat(),
//        size.height, size.width, null);
//      File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//      String filename = "/test/img" + System.currentTimeMillis() + ".png";
//      File file = new File(path, filename);
//      FileOutputStream filecon = new FileOutputStream(file);
//
//      int q = image.getHeight() / 4;
//
//      image.compressToJpeg(
//        new Rect(0, 0, image.getWidth(), image.getHeight()), 25,
//        filecon);
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//    }


    if (processImage) {
      m.showData(i);
      i++;
      Camera.Parameters parameters = camera.getParameters();
      Camera.Size size = parameters.getPreviewSize();
      int pFormat = parameters.getPreviewFormat();
      Message msg = new Message();
      Bundle args = new Bundle();
      args.putByteArray("data", data);
      args.putInt("w", size.width);
      args.putInt("h", size.height);
      args.putInt("pF", pFormat);
      msg.setData(args);
      mHandler.sendMessage(msg);
    }
  }

  class Frame {

    Frame(byte[] data,
          Camera camera) {
      this.data = data;
      this.camera = camera;
    }


    byte[] data;
    Camera camera;
  }

  private Handler mHandler;

  @Override
  public void run() {

    Looper.prepare();

    mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        try {

          byte[] data = msg.getData().getByteArray("data");
          int w = msg.getData().getInt("w");
          int h = msg.getData().getInt("h");
          int pF = msg.getData().getInt("pF");

          if (null != data) {

            YuvImage image = new YuvImage(data, pF,
              w, h, null);
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String filename = "/test/img" + System.currentTimeMillis() + ".png";
            File file = new File(path, filename);
            FileOutputStream filecon = new FileOutputStream(file);

            image.compressToJpeg(
              new Rect(0, 0, image.getWidth(), image.getHeight()), 25,
              filecon);

            m.showData(i);
            i--;
          }
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
    };

    Looper.loop();
  }

//  private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
//    byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
//    // Rotate the Y luma
//    int i = 0;
//    for (int x = 0; x < imageWidth; x++) {
//      for (int y = imageHeight - 1; y >= 0; y--) {
//        yuv[i] = data[y * imageWidth + x];
//        i++;
//      }
//    }
//    // Rotate the U and V color components
//    i = imageWidth * imageHeight * 3 / 2 - 1;
//    for (int x = imageWidth - 1; x > 0; x = x - 2) {
//      for (int y = 0; y < imageHeight / 2; y++) {
//        yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
//        i--;
//        yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
//        i--;
//      }
//    }
//    return yuv;
//  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    // empty. Take care of releasing the Camera preview in your activity.
    processImage = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    // If your preview can change or rotate, take care of those events here.
    // Make sure to stop the preview before resizing or reformatting it.

    if (mHolder.getSurface() == null) {
      // preview surface does not exist
      return;
    }

    // stop preview before making changes
    try {
      mCamera.stopPreview();
    } catch (Exception e) {
      // ignore: tried to stop a non-existent preview
    }

    // set preview size and make any resize, rotate or
    // reformatting changes here

    // start preview with new settings
    try {
      mCamera.setPreviewDisplay(mHolder);
      mCamera.setPreviewCallback(this);
      mCamera.setDisplayOrientation(DEGREE);
      mCamera.startPreview();
    } catch (Exception e) {
      Log.d(TAG, "Error starting camera preview: " + e.getMessage());
    }
  }

}
