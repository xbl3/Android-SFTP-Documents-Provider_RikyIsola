package com.island.androidftpdocumentprovider;
import android.app.*;
import android.content.*;
import android.os.*;
import android.accounts.*;
import android.util.*;
import java.util.*;
import com.enterprisedt.net.ftp.*;
public class AccountService extends Service
{
	private AccountAuthenticator authenticator;
	@Override
	public IBinder onBind(Intent intent)
	{
		Log.i(MainActivity.LOG_TAG,"Binding service");
		return authenticator.getIBinder();
	}
	@Override
	public void onCreate()
	{
		Log.i(MainActivity.LOG_TAG,"Service created");
		authenticator=new AccountAuthenticator(this);
	}
	@Override
	public void onDestroy()
	{
		Log.i(MainActivity.LOG_TAG,"Service destroyed");
	}
}
