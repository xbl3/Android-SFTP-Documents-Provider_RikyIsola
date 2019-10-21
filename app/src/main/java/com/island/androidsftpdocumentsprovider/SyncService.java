package com.island.androidsftpdocumentsprovider;
import android.app.*;
import android.content.*;
import android.os.*;
public class SyncService extends Service
{
	private SyncAdapter sSyncAdapter;
	private static final Object sSyncAdapterLock=new Object();
	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.i("Created sync service");
		synchronized(sSyncAdapterLock)
		{
			if(sSyncAdapter==null)
			{
				sSyncAdapter=new SyncAdapter(getApplicationContext(),true);
			}
		}
	}
	@Override
	public IBinder onBind(Intent intent)
	{
		return sSyncAdapter.getSyncAdapterBinder();
	}
}
