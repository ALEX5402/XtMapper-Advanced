package com.xtr.keymapper;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import static android.content.Context.WINDOW_SERVICE;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TouchPointer {

    // declaring required variables
    private final Context context;
    private final View cursorView;
    private final WindowManager.LayoutParams mParams;
    private final WindowManager mWindowManager;
    Button stopPointer;
    int x1 = 100;
    int x2 = 100;
    int y1 = 100;
    int y2 = 100;
    public TouchPointer(Context context){
        this.context=context;
        stopPointer = ((MainActivity)context).findViewById(R.id.stop_pointer);
        stopPointer.setOnClickListener(v -> hideCursor());
        // set the layout parameters of the cursor
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // Don't let the cursor grab the input focus
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                // Make the underlying application window visible
                // through the cursor
                PixelFormat.TRANSLUCENT);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        cursorView = layoutInflater.inflate(R.layout.cursor, null);
        mParams.gravity = Gravity.CENTER;
        mWindowManager = (WindowManager)context.getSystemService(WINDOW_SERVICE);
    }
public void open() {
    try {
        if(cursorView.getWindowToken()==null) {
            if(cursorView.getParent()==null) {
                mWindowManager.addView(cursorView, mParams);
            }
        }
    } catch (Exception e) {
        Log.d("Error1",e.toString());
    }

    new Thread(() -> {
        try{
            Socket socket = new Socket("127.0.0.1", MainActivity.DEFAULT_PORT);
            DataOutputStream Xout = new DataOutputStream(socket.getOutputStream());
            Process sh = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(sh.getOutputStream());
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(sh.getInputStream()));

            outputStream.writeBytes("getevent -ql"+"\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            String line;
            boolean pointer_down = false;
            while ((line = stdInput.readLine()) != null) {
                String []xy = line.split("\\s+");
                switch (xy[2]) {
                    case "REL_X": {
                        x2 += (int) Utils.hexToDec(xy[3]);
                        if (pointer_down)
                            Xout.writeBytes(x1 + " " + y1 + " " + "MOVE " + x2 + " " + y2 + "\n");
                        x1 = x2;
                        break;
                    }
                    case "REL_Y": {
                        y2 += (int) Utils.hexToDec(xy[3]);
                        if (pointer_down)
                            Xout.writeBytes(x1 + " " + y1 + " " + "MOVE " + x2 + " " + y2 + "\n");
                        y1 = y2;
                        break;
                    }
                    case "BTN_MOUSE": {
                        pointer_down = xy[3].equals("DOWN");
                        Xout.writeBytes(x1 + " " + y1 + " " + xy[3] + "\n");
                        break;
                    }
                }
                cursorView.setX(x1);
                cursorView.setY(y1);
                /*cursorView.animate()
                        .x(x1)
                        .y(y1)
                        .setDuration(0)
                        .start(); */

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }).start();
}
    public void hideCursor() {
        try {
            ((WindowManager)context.getSystemService(WINDOW_SERVICE)).removeView(cursorView);
            cursorView.invalidate();
            // remove all views
            ((ViewGroup) cursorView.getParent()).removeAllViews();

            // the above steps are necessary when you are adding and removing
            // the view simultaneously, it might give some exceptions
        } catch (Exception e) {
            Log.d("Error2",e.toString());
        }
    }
}
