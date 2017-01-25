package com.asmedia.checkin_android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.zxing.Result;

import org.json.JSONObject;

import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanActivityZXing extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView mScannerView;

    TextView eventStatus;

    String loadedEvent;

    int attendeesCount;

    Realm realm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_zxing);

        eventStatus = (TextView) findViewById(R.id.textViewEventStatus);
        mScannerView = (ZXingScannerView) findViewById(R.id.scannerView);
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();

        Realm.init(this);
        realm = Realm.getDefaultInstance();


        RealmResults<AttendeeObject> results = realm.where(AttendeeObject.class).findAll();

        if (results.isEmpty()){

            eventStatus.setText("NO EVENT SYNCED");

        }else {

            loadedEvent = results.get(0).getEventName();
            attendeesCount = results.size();
            Log.d("Event: ",loadedEvent);

            eventStatus.setText("Event: " + loadedEvent + " | " + "Attendees: " + attendeesCount);

        }

        //mScannerView = new ZXingScannerView(this);
        //setContentView(mScannerView);



    }

    @Override
    protected void onPause(){
        super.onPause();
        mScannerView.stopCamera();

    }

    @Override
    public void handleResult(Result result) {

        Log.d("SCAN: ", result.getText());

        int private_reference_number = 0;
        try {
            private_reference_number = Integer.parseInt(result.getText());
        } catch (NumberFormatException nfe) {
            Log.d("Parse: ", "Could not parse " + nfe);
        }

        if (ticketExists(private_reference_number)) {


            if(ticketIsValid(private_reference_number)){

                String attendeeName = getAttendeeName(private_reference_number);

                AlertDialog.Builder a_builder = new AlertDialog.Builder(this);
                final int finalPrivate_reference_number = private_reference_number;
                a_builder.setMessage("Gast: " + attendeeName)
                        .setCancelable(false)
                        .setPositiveButton("Einchecken", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                checkIn(finalPrivate_reference_number);

                                mScannerView.resumeCameraPreview(ScanActivityZXing.this);


                            }
                        })
                        .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                                mScannerView.resumeCameraPreview(ScanActivityZXing.this);
                            }
                        });

                AlertDialog alert = a_builder.create();
                alert.setTitle("Ticket gültig!");
                alert.show();
            } else{

                String checkinTime = getCheckinTime(private_reference_number);

                AlertDialog.Builder a_builder = new AlertDialog.Builder(this);
                final int finalPrivate_reference_number1 = private_reference_number;
                a_builder.setMessage("Checkinzeit: " + checkinTime)
                        .setCancelable(false)
                        .setPositiveButton("Auschecken", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                checkOut(finalPrivate_reference_number1);

                                mScannerView.resumeCameraPreview(ScanActivityZXing.this);


                            }
                        })
                        .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                                mScannerView.resumeCameraPreview(ScanActivityZXing.this);
                            }
                        });

                AlertDialog alert = a_builder.create();
                alert.setTitle("Ticket bereits verwendet!");
                alert.show();





            }

        }else{

            AlertDialog.Builder a_builder = new AlertDialog.Builder(this);
            a_builder.setMessage("Dieses Ticket existiert nicht in der Datenbank")
                    .setCancelable(false)
                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            mScannerView.resumeCameraPreview(ScanActivityZXing.this);
                        }
                    });

            AlertDialog alert = a_builder.create();
            alert.setTitle("Ticketcode ungültig!");
            alert.show();


        }
    }
    public boolean ticketExists(int private_reference_number){


        RealmQuery<AttendeeObject> query = realm.where(AttendeeObject.class);
        query.equalTo("private_reference_number",private_reference_number);

        RealmResults<AttendeeObject> results = query.findAll();

        if(results.isEmpty()){

            Log.d("DBController","Ticket does not exist!");
            return false;

        } else{

            Log.d("DBController","Ticket exists!");
            return true;

        }

    }

    public boolean ticketIsValid(int private_reference_number){


        RealmQuery<AttendeeObject> query = realm.where(AttendeeObject.class);
        query.equalTo("private_reference_number",private_reference_number);
        query.equalTo("arrived", false);

        RealmResults<AttendeeObject> results = query.findAll();

        if(results.isEmpty()){

            Log.d("DBController","Ticket does not exist!");
            return false;

        } else{

            Log.d("DBController","Ticket exists!");
            return true;

        }

    }

    public void checkIn(int private_reference_number){


        RealmQuery<AttendeeObject> query = realm.where(AttendeeObject.class);
        query.equalTo("private_reference_number",private_reference_number);


        RealmResults<AttendeeObject> results = query.findAll();

        realm.beginTransaction();
        results.get(0).setArrived(true);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm");
        String format = simpleDateFormat.format(new Date());
        results.get(0).setCheckinTime(format);

        Log.d("MainAct", "Current Timestamp " + format);

        //Log.d("Hours", String.valueOf(android.icu.util.Calendar.HOUR));

        realm.commitTransaction();




    }

    public void checkOut(int private_reference_number){

        RealmQuery<AttendeeObject> query = realm.where(AttendeeObject.class);
        query.equalTo("private_reference_number",private_reference_number);


        RealmResults<AttendeeObject> results = query.findAll();

        realm.beginTransaction();
        results.get(0).setArrived(false);
        realm.commitTransaction();





    }

    public String getCheckinTime(int private_reference_number){


        RealmQuery<AttendeeObject> query = realm.where(AttendeeObject.class);
        query.equalTo("private_reference_number",private_reference_number);
        query.equalTo("arrived", true);

        RealmResults<AttendeeObject> results = query.findAll();
        return (results.get(0).getCheckinTime());



    }

    public String getAttendeeName(int private_reference_number){

        RealmQuery<AttendeeObject> query = realm.where(AttendeeObject.class);
        query.equalTo("private_reference_number",private_reference_number);


        RealmResults<AttendeeObject> results = query.findAll();
        String attendeeName = (results.get(0).getFirstName() + " " + results.get(0).getLastName());

        return attendeeName;



    }
}
