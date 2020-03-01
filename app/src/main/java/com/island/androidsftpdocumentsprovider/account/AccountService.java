package com.island.androidsftpdocumentsprovider.account;
import android.app.*;
import android.content.*;
import android.os.*;
public class AccountService extends Service
{
	private AccountAuthenticator authenticator;
	@Override
	public IBinder onBind(Intent intent)
	{
		return authenticator.getIBinder();
	}
	@Override
	public void onCreate()
	{
		authenticator=new AccountAuthenticator(this);
	}
}
