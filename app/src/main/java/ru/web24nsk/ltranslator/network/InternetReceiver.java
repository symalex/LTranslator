package ru.web24nsk.ltranslator.network;

import java.io.IOException;
import java.util.LinkedHashSet;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class InternetReceiver extends BroadcastReceiver
{
	private Context mCtx;
	private final LinkedHashSet<InternetReceiverListener> mListeners;
	private boolean mIsInitialized = false;
	private boolean mIsInternetPermissionOk = false;
	private boolean mIsConnectionOk = false;

	public interface InternetReceiverListener
	{
		void onInternetConnectionChange(InternetReceiver receiver);
	}

	public static final String FILTER_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

	public InternetReceiver()
	{
		mListeners = new LinkedHashSet<>();
	}

	public void setContext(Context ctx)
	{
		mCtx = ctx;
	}

	public void addInternetReceiverListener(InternetReceiverListener listener)
	{
		mListeners.add(listener);
	}

	public void removeInternetReceiverListener(InternetReceiverListener listener)
	{
		mListeners.remove(listener);
	}

	public boolean isInternetPermissionOk()
	{
		return mIsInternetPermissionOk;
	}

	public boolean isConnectionOk()
	{
		return mIsConnectionOk;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		updateConnectionPermission();
		boolean connected = isConnected();

		if (!mIsInitialized || connected != mIsConnectionOk)
		{
			mIsInitialized = true;
			mIsConnectionOk = connected;

			for (InternetReceiverListener listener : mListeners)
			{
				if (listener != null)
				{
					listener.onInternetConnectionChange(this);
				}
			}
		}
	}

	private boolean isConnected()
	{
		if (!isInternetPermissionOk())
		{
			return false;
		}

		Runtime runtime = Runtime.getRuntime();

		try
		{
			Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
			int exitValue = ipProcess.waitFor();
			return (exitValue == 0);
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
		}

		return false;
	}

	private boolean isNetworkAvailable()
	{
		if (!isInternetPermissionOk())
		{
			return false;
		}
		ConnectivityManager connectivityManager = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	private void updateConnectionPermission()
	{
		mIsInternetPermissionOk = ContextCompat.checkSelfPermission(mCtx, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
				ContextCompat.checkSelfPermission(mCtx, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
	}

	public void updateConnectionState()
	{
		updateConnectionPermission();
		mIsConnectionOk = isNetworkAvailable();
	}

}
