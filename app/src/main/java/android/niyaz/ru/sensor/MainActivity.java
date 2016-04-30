package android.niyaz.ru.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;

    private float[] rotationMatrix;     //Матрица поворота
    private float[] accelData;           //Данные с акселерометра
    private float[] magnetData;       //Данные геомагнитного датчика
    private float[] OrientationData; //Матрица положения в пространстве
    private Integer speed = 0;

    private TextView xzView;

    private Button buttonRight;
    private Button buttonLeft;
    private Button buttonGas;

    private Timer timer = new Timer();

    private Integer smartPhoneID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров

        rotationMatrix = new float[16];
        accelData = new float[3];
        magnetData = new float[3];
        OrientationData = new float[3];

        setContentView(R.layout.activity_main);

        xzView = (TextView) findViewById(R.id.xzValue);

        buttonRight = (Button) findViewById(R.id.buttonRight);
        buttonLeft = (Button) findViewById(R.id.buttonLeft);
        buttonGas = (Button) findViewById(R.id.buttonGas);

        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.OrientationData[1] = MainActivity.this.OrientationData[1] - 1;
            }
        });

        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.OrientationData[1] = MainActivity.this.OrientationData[1] + 1;
            }
        });

        buttonGas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speed = speed + 5;
            }
        });

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    MainActivity.this.smartPhoneID = getSmartPhoneID();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                URL url = new URL("http://192.168.43.118:8080/angles");
                                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                urlConnection.setRequestMethod("POST");
                                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                                urlConnection.setRequestProperty("charset", "utf-8");
                                urlConnection.setUseCaches(false);
                                String postData = "";
                                postData = "id=" + smartPhoneID.toString() +
                                        "&xy=" + String.valueOf(Math.round(Math.toDegrees(OrientationData[0]))) +
                                        "&xz=" + String.valueOf(Math.round(Math.toDegrees(OrientationData[1]))) +
                                        "&zy=" + String.valueOf(Math.round(Math.toDegrees(OrientationData[2]))) +
                                        "&speed=" + Integer.toString(speed);
                                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                                wr.write(postData.getBytes("UTF-8"));
                                urlConnection.getResponseCode();
                                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                            } catch (Exception ex) {
                                return;
                            }
                        }
                    }, 1000, 500);
                } catch (Exception ex) {

                }
            }
        }, 3000);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        loadNewSensorData(event); // Получаем данные с датчика
        SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magnetData); //Получаем матрицу поворота
        SensorManager.getOrientation(rotationMatrix, OrientationData); //Получаем данные ориентации устройства в пространстве

        if (xzView == null) {  //Без этого работать отказалось.
            xzView = (TextView) findViewById(R.id.xzValue);
        }

        //Выводим результат

        xzView.setText(String.valueOf(Math.round(Math.toDegrees(OrientationData[1]))));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private void loadNewSensorData(SensorEvent event) {
        final int type = event.sensor.getType(); //Определяем тип датчика
        if (type == Sensor.TYPE_ACCELEROMETER) { //Если акселерометр
            accelData = event.values.clone();
        }

        if (type == Sensor.TYPE_MAGNETIC_FIELD) { //Если геомагнитный датчик
            magnetData = event.values.clone();
        }
    }

    private Integer getSmartPhoneID() throws Exception {
        URL url = new URL("http://192.168.43.118:8080/reg");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.getResponseCode();
        return Integer.parseInt(urlConnection.getHeaderField("id"));
    }

}
