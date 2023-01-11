package xtr.keymapper;

import xtr.keymapper.IRemoteServiceCallback;

interface IRemoteService {
    void injectEvent(float x, float y, int type, int pointerId);
    void injectScroll(float x, float y, int value);
    void startServer(float sensitivity);
    int tryOpenDevice(String device);
    void closeDevice();
    void registerCallback(IRemoteServiceCallback cb);
    void unregisterCallback(IRemoteServiceCallback cb);
}