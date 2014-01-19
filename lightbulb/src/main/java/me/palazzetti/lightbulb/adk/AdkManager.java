package me.palazzetti.lightbulb.adk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Allows to manager your ADK Usb Interface. It exposes
 * methods to close/open connected accessory and to read/write
 * from/to it.
 */

public class AdkManager implements IAdkManager {
    private static final String LOG_TAG = "AdkManager";

    private UsbManager mUsbManager;
    private UsbAccessory mUsbAccessory;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    private IntentFilter mDetachedFilter;
    private BroadcastReceiver mUsbReceiver;

    public AdkManager(UsbManager mUsbManager) {
        // Store Android UsbManager reference
        this.mUsbManager = mUsbManager;

        // Filter for detached events
        mDetachedFilter = new IntentFilter();
        mDetachedFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);

        // Broadcast Receiver
        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                    UsbAccessory usbAccessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (usbAccessory != null && usbAccessory.equals(mUsbAccessory)) {
                        closeAccessory();
                    }
                }
            }
        };
    }

    @Override
    public void openAccessory(UsbAccessory usbAccessory) {
        mParcelFileDescriptor = mUsbManager.openAccessory(usbAccessory);
        if (mParcelFileDescriptor != null) {
            mUsbAccessory = usbAccessory;
            FileDescriptor fileDescriptor = mParcelFileDescriptor.getFileDescriptor();
            mFileInputStream = new FileInputStream(fileDescriptor);
            mFileOutputStream = new FileOutputStream(fileDescriptor);

            // TODO: start reading from USB (AsyncTask)
        }
    }

    @Override
    public void closeAccessory() {
        if (mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        // TODO reset
        mParcelFileDescriptor = null;
        mUsbAccessory = null;
    }

    @Override
    public void readText() {

    }

    @Override
    public void sendText(String text) {
        byte[] buffer = text.getBytes();
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.write(buffer);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    @Override
    public void pauseAdk() {
        closeAccessory();
    }

    @Override
    public void resumeAdk() {
        if (mFileInputStream == null || mFileOutputStream == null) {
            UsbAccessory[] usbAccessoryList = mUsbManager.getAccessoryList();
            if (usbAccessoryList != null) {
                openAccessory(usbAccessoryList[0]);
            }
        }
    }

    public IntentFilter getDetachedFilter() {
        return mDetachedFilter;
    }

    public BroadcastReceiver getUsbReceiver() {
        return mUsbReceiver;
    }
}