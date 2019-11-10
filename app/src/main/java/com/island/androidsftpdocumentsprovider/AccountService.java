package com.island.androidsftpdocumentsprovider;
import android.app.*;
import android.content.*;
import android.os.*;
public class AccountService extends Service
{
	private AccountAuthenticator authenticator;
	@Override
	public IBinder onBind(Intent intent)
	{
		Log.i("Binding service");
		return authenticator.getIBinder();
	}
	@Override
	public void onCreate()
	{
		Log.i("Service created");
		authenticator=new AccountAuthenticator(this);
	}
	@Override
	public void onDestroy()
	{
		Log.i("Service destroyed");
	}
}
