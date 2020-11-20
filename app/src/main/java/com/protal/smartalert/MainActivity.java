package com.protal.smartalert;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {
    private static final String TAG = "MainActivity";
    //Button fireBtn;
    private SensorManager sensorManager;
    Sensor accelerometer;
    private TextView text_x;
    private TextView text_y;
    private TextView text_z;
    private TextView timerTxt;
    TextView textViewGreeting;
    final static int REQ_CODE = 765;
    private boolean inEmergency=false;
    private long lastEmergency=0;
    private long timePassedFromTimerFinish =0;
    private boolean timerHasFinished=false;
    private boolean mustCancel=false;
   private boolean messageSent=false;
   private long previousTimestamp;
    ArrayList<String> emergenciesList=new ArrayList();
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    private String phoneNo;
    private String message;

    private boolean itIsNotFirstTime=false;
    private  float currentX,currentY,currentZ,lastX,lastY,lastZ,xDifference,yDifference,zDifference;
    private float shakeThreshold=5f;

    private long lastAbort;



    private CountDownTimer countDownTimer;
    private long timeLeftMillis =10000 ;//30000
    private static final long START_TIME_IN_MILLIS = 10000;//30000
    private boolean timerRunning = false;
    private static long lastStop = -10000;
    LocationManager locationManager;
    FirebaseDatabase rootNode;
    private DatabaseReference refMaxSpeed;
    private DatabaseReference myRefUsers;
    private DatabaseReference emRef;
    private DatabaseReference myRefEmergencies;
    FirebaseAuth fAuth;
    private FirebaseUser userF;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button fireBtn =findViewById(R.id.fireBtn);
        Log.d(TAG, "onCreate: Initializing sensor services");
        text_x = (TextView) findViewById(R.id.text_x);
        text_y = (TextView) findViewById(R.id.text_y);
        text_z = (TextView) findViewById(R.id.text_z);
        timerTxt = (TextView) findViewById(R.id.timerTxt);
        textViewGreeting = findViewById(R.id.textViewGreeting);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lastAbort=0;

        fAuth = FirebaseAuth.getInstance();
        rootNode = FirebaseDatabase.getInstance();
        myRefUsers = rootNode.getReference("users");
        userF = fAuth.getCurrentUser();
        textViewGreeting.setText("Hello " + userF.getDisplayName());
        Log.d(TAG, "onCreate: myRefUsers" + myRefUsers.toString());
        //myRefEmergencies = rootNode.getReference("users").child(userUid).child("emergencies");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // USE SENSOR
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onCreate: Registered accelerometer Listener");

        fireBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,image.class));

            }
        });

        String uid = userF.getUid();
        DatabaseReference myRef = rootNode.getReference("users").child(uid).child("emergencies");
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Map<String, Object> value = (Map<String, Object>) dataSnapshot.getValue();
               emergenciesList.add(value.toString());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException());
            }
        };

        myRef.addChildEventListener(childEventListener);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    public void runpermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE);

        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            // if you can't get speed because reasons :)
        } else {

            if(inEmergency&(System.currentTimeMillis()-lastEmergency>20000)) {//timer has finished and 20 have passed since lastEmergency


                String status = "Fall";
                long timestamp = location.getTime();
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                Log.d(TAG, "onLocationChanged: latitude" + latitude);
                String uid = userF.getUid();
                Emergency emergency = new Emergency(status, longitude, latitude, timestamp);
                myRefUsers.child(uid).child("emergencies").child(String.valueOf(timestamp)).setValue(emergency);
                lastEmergency = System.currentTimeMillis();

                }


            }
        }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //Log.d(TAG, "onSensorChanged: X:" + event.values[0] + " Y:" + event.values[1] + " Z:" + event.values[2]);
        text_x.setText("X:" + event.values[0]);
        text_y.setText("Y:" + event.values[1]);
        text_z.setText("Z:" + event.values[2]);

        currentX=event.values[0];
        currentY=event.values[1];
        currentZ=event.values[2];

        if(itIsNotFirstTime){

            xDifference=Math.abs(currentX-lastX);
            yDifference=Math.abs(currentY-lastY);
            zDifference=Math.abs(currentZ-lastZ);
            if((xDifference>shakeThreshold)&(yDifference>shakeThreshold)||
                    (xDifference>shakeThreshold)&(zDifference>shakeThreshold)||
                    (yDifference>shakeThreshold)&(zDifference>shakeThreshold)){

                phoneNo = "6943809357";
                message = "Σεισμός";
                sendSMSMessage();


            }

        }


        lastX=currentX;
        lastY=currentY;
        lastZ=currentZ;
        itIsNotFirstTime=true;


        /*Log.d(TAG, "onSensorChanged: LAst abort"+lastAbort);
        Log.d(TAG, "onSensorChanged: timerRunning"+timerRunning);
        Log.d(TAG, "onSensorChanged: has finished for"+(System.currentTimeMillis()- timePassedFromTimerFinish));
        Log.d(TAG, "onSensorChanged: time since last emergency"+(System.currentTimeMillis()- lastEmergency));
        Log.d(TAG, "onSensorChanged: time passed since timer finished"+(System.currentTimeMillis()- timePassedFromTimerFinish));*/

        if ((event.values[2] > 9.5||event.values[2] < -9.5) & timerRunning == false
                &(System.currentTimeMillis()-lastAbort>10000)
                &(System.currentTimeMillis()-lastEmergency>10000)&(System.currentTimeMillis()- timePassedFromTimerFinish >10000)) {//
            //user is down&timer isn't running & he hasn't pressed abort for 20 seconds & last emergency was 20 secs ago
            timerRunning = true;
            timerHasFinished=false;
            startTimer();

        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.mymenu,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.item1:

                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, Login.class));
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void startTimer() {


            countDownTimer = new CountDownTimer(timeLeftMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                    timeLeftMillis = millisUntilFinished;
                    updateTimer();

                }
                @Override
                public void onFinish() {
                    //on the ground for 30'
                    messageSent=true;

                        Log.d(TAG, "onFinish: user has been 30' on the ground");
                        timerHasFinished=true;
                        timePassedFromTimerFinish =System.currentTimeMillis();

                        inEmergency = true;//send location to firebase
                         runpermission();
                         stopTimer();
                         timerTxt.setText("SOS!");
                         //MUST SEND MESSAGE FOR HELP

                    phoneNo="6943809357";
                    message="SOS";
                    sendSMSMessage();
                         //return;
                }
            }.start();



    }

    public void stopTimer(){
        if(timerRunning){
        countDownTimer.cancel();
        timeLeftMillis=START_TIME_IN_MILLIS;
        updateTimer();
        timerRunning=false;
        }

    }
    public void updateTimer(){
        int minutes = (int)timeLeftMillis/60000;
        int seconds=(int)timeLeftMillis%60000/1000;
        String timeLeftText;
        timeLeftText=""+minutes;
        timeLeftText+=":";
        if(seconds<10){timeLeftText+="0";}
        timeLeftText+=seconds;
        timerTxt.setText(timeLeftText);
    }

    public void AbortBtn(View view){

    if(!timerHasFinished) {//timer still running and user pressed abort

        lastAbort = System.currentTimeMillis();
        stopTimer();
    }else{
          cancelEmergency();
    }
        inEmergency = false;

    }
    public void cancelEmergency(){
        String uid = userF.getUid();
        int size=emergenciesList.size();
        Log.d(TAG, "cancelEmergency: size of list is"+size);
        if(size!=0){
            Log.d(TAG, "cancelEmergency: size!=0");
            String previousEmergency= emergenciesList.get(size - 1);
            Log.d(TAG, "onLocationChanged: previousEmergency before replacement"+previousEmergency);
            previousEmergency=previousEmergency
                    .replace("{","")
                    .replace("}","")
                    .replace("="," ")
                    .replace(","," ")
                    .replace("  "," ");
            String array[]=previousEmergency.split(" ");
            Log.d(TAG, "onLocationChanged: previousEmergency"+previousEmergency);
            //Log.d(TAG, "onLocationChanged: array"+array[0]+"/"+array[1]+"/"+array[2]+"/");
            if((array.length)<9) {//if not already canceled
                previousTimestamp = Long.parseLong(array[7]);
                Log.d(TAG, "onLocationChanged: previoustimestamp" + previousTimestamp);
                DatabaseReference ref = myRefUsers.child(uid).child("emergencies").child(String.valueOf(previousTimestamp));// cancel previousTimestamp
                ref.child("Canceled").setValue("canceled");
                //MUST SEND MESSAGE TO INFORM THAT THE EMERGENCY IS CANCELED
                phoneNo = "6943809357";
                message = "Άκυρος ο συναγερμός.'Ολα καλά";
                sendSMSMessage();

            }
            timerRunning=false;
            //mustCancel=false;

        }

    }
    protected void sendSMSMessage() {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNo, null, message, null, null);
        Toast.makeText(getApplicationContext(), "SMS sent.",
                Toast.LENGTH_LONG).show();
    }




}
