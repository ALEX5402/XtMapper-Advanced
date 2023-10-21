package xtr.keymapper.server;

import android.os.Looper;
import android.os.ServiceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import xtr.keymapper.ActivityObserver;
import xtr.keymapper.IRemoteService;
import xtr.keymapper.IRemoteServiceCallback;
import xtr.keymapper.OnKeyEventListener;
import xtr.keymapper.Utils;
import xtr.keymapper.keymap.KeymapConfig;
import xtr.keymapper.keymap.KeymapProfile;

public class RemoteService extends IRemoteService.Stub {
    private String currentDevice = "";
    private InputService inputService;
    private OnKeyEventListener mOnKeyEventListener;
    private boolean isWaylandClient = false;
    private ActivityObserverService activityObserverService;

    public static void main(String[] args) {
        Looper.prepare();
        new RemoteService(args);
        Looper.loop();
    }

    public RemoteService(String[] args) {
        super();
        Log.i("XtMapper", "starting server...");
        try {
            ServiceManager.addService("xtmapper", this);

            System.out.println("Waiting for overlay...");
            for (String arg: args) {
                if (arg.equals("--wayland-client")) {
                    isWaylandClient = true;
                    System.out.println("using wayland client");
                }
            }
            start_getevent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void start_getevent() {
        new Thread(() -> {
            try {
                final BufferedReader getevent;
                if (isWaylandClient) {
                    getevent = new BufferedReader(new InputStreamReader(System.in));
                } else {
                    getevent = Utils.geteventStream(System.getProperty("java.library.path"));
                }
                String line;
                while ((line = getevent.readLine()) != null) {
                    String[] data = line.split(":"); // split a string like "/dev/input/event2: EV_REL REL_X ffffffff"
                    if (addNewDevices(data)) {
                        if (inputService != null)
                            if (!inputService.stopEvents) {
                                if (isWaylandClient && data[0].contains("wl_pointer"))
                                    inputService.sendWaylandMouseEvent(data[1]);
                                else
                                    inputService.getKeyEventHandler().handleEvent(data[1]);
                            } else {
                                inputService.getKeyEventHandler().handleKeyboardShortcutEvent(data[1]);
                            }
                        if (mOnKeyEventListener != null) mOnKeyEventListener.onKeyEvent(line);
                    }
                }
            } catch (Exception e){
                e.printStackTrace(System.out);
            }
        }).start();
    }

    private boolean addNewDevices(String[] data) {
        String[] input_event;
        if (data.length != 2) return false;
        String evdev = data[0];

        input_event = data[1].split("\\s+");
        if (isWaylandClient) return true;
        if( !currentDevice.equals(evdev) )
            if (input_event[1].equals("EV_REL")) {
                System.out.println("add device: " + evdev);
                currentDevice = evdev;
            }
        return true;
    }

    public boolean isRoot() {
        return inputService.supportsUinput > 0;
    }

    @Override
    public void startServer(KeymapProfile profile, KeymapConfig keymapConfig, IRemoteServiceCallback cb, int screenWidth, int screenHeight) {
        if (inputService != null) stopServer();
        inputService = new InputService(profile, keymapConfig, cb, screenWidth, screenHeight, isWaylandClient);
        if (!isWaylandClient) {
            inputService.setMouseLock(true);
            inputService.openDevice(currentDevice);
        }
    }

    @Override
    public void stopServer() {
        if (inputService != null && !isWaylandClient) {
            inputService.stopEvents = true;
            inputService.stop();
            inputService.stopMouse();
            inputService.destroyUinputDev();
            inputService = null;
        }
    }

    @Override
    public void registerOnKeyEventListener(OnKeyEventListener l)  {
        mOnKeyEventListener = l;
    }

    @Override
    public void unregisterOnKeyEventListener(OnKeyEventListener l)  {
        mOnKeyEventListener = null;
    }

    @Override
    public void registerActivityObserver(ActivityObserver callback) {
        if (activityObserverService != null)
            activityObserverService.stop();
        activityObserverService = new ActivityObserverService(callback);
    }

    @Override
    public void unregisterActivityObserver(ActivityObserver callback) {
        if (activityObserverService != null)
            activityObserverService.stop();
        activityObserverService = null;
    }

    public void pauseMouse(){
        if (inputService != null) {
            if(!isWaylandClient) inputService.setMouseLock(false);
            inputService.stopEvents = true;
        }
    }

    @Override
    public void reloadKeymap() {
        if (inputService != null) {
            inputService.reloadKeymap();
            if (inputService.getKeymapProfile().disabled) stopServer();
        }
    }

    public void resumeMouse(){
        if (inputService != null) {
            if(!isWaylandClient) inputService.setMouseLock(true);
            inputService.stopEvents = false;
        }
    }

    static {
        System.loadLibrary("mouse_read");
        System.loadLibrary("mouse_cursor");
    }

    public static IRemoteService getInstance(){
        return IRemoteService.Stub.asInterface(ServiceManager.getService("xtmapper"));
    }

}