package android.com.cameraframes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends Activity implements SensorEventListener {

  private Camera mCamera;
  private CameraPreview mPreview;
  private Sensor gyro;
  private SensorManager mgr;
  private TextView textView2;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    gyro = mgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    txt = (TextView) findViewById(R.id.textView);
    textView2= (TextView) findViewById(R.id.textView2);

    mCamera = Camera.open();


    // Create our Preview view and set it as the content of our activity.
    mPreview = new CameraPreview(this, mCamera);
    FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
    preview.addView(mPreview);
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    //if sensor is unreliable, return void
    if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
      return;
    }

    int data = Float.valueOf(event.values[1]).intValue();

    if (data >= 20) {
      mPreview.setCapture(true);
    } else {
      mPreview.setCapture(false);
    }

    //else it will output the Roll, Pitch and Yawn values
    txt.setText("Orientation X (Roll) :" + Float.toString(event.values[2]) + "\n" +
      "Orientation Y (Pitch) :" + Float.toString(event.values[1]) + "\n" +
      "Orientation Z (Yaw) :" + Float.toString(event.values[0]));
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }

  public void showData(final int i ){
   runOnUiThread(new Runnable() {
     @Override
     public void run() {
       textView2.setText("CANT: " + i);
     }
   });
  }

  private TextView txt = null;

  private int findFrontFacingCamera() {
    int cameraId = -1;
    // Search for the front facing camera
    int numberOfCameras = Camera.getNumberOfCameras();
    for (int i = 0; i < numberOfCameras; i++) {
      Camera.CameraInfo info = new Camera.CameraInfo();
      Camera.getCameraInfo(i, info);
      if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        cameraId = i;
        break;
      }
    }
    return cameraId;
  }


  @Override
  protected void onResume() {
    mgr.registerListener(this, mgr.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
    super.onResume();
  }

  @Override
  protected void onPause() {
    mgr.unregisterListener(this);
    super.onPause();
  }

}
