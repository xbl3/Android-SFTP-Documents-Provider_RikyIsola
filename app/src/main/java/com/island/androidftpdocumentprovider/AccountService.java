package com.island.androidftpdocumentprovider;
import android.app.*;
import android.content.*;
import android.os.*;
import android.accounts.*;
import android.util.*;
import java.util.*;
public class AccountService extends Service
{
	private Authenticator authenticator;
	@Override
	public IBinder onBind(Intent p1)
	{
		return authenticator.getIBinder();
	}
	@Override
	public void onCreate()
	{
		Log.i(MainActivity.LOG_TAG,"Service created");
		authenticator=new Authenticator(this);
	}
	@Override
	public void onDestroy()
	{
		Log.i(MainActivity.LOG_TAG,"Service destroyed");
	}
	public class Authenticator extends AbstractAccountAuthenticator
	{
		public Authenticator(Context context)
		{
			super(context);
		}
		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response,String accountType)
		{
			Log.i(MainActivity.LOG_TAG,"Edit account properties: response="+response+" accountType="+accountType);
			throw new UnsupportedOperationException();
		}
		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,String accountType,String authTokenType,String[]requiredFeatures,Bundle options)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" accountType="+accountType+" authTokenType="+authTokenType+" requiredFeatures="+Arrays.toString(requiredFeatures)+" options="+options);
			return null;
		}
		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response,Account account,Bundle options)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" account="+account+" options="+options);
			throw new UnsupportedOperationException();
		}
		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response,Account account,String authTokenType,Bundle options)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" account="+account+" authTokenType="+authTokenType+" options="+options);
			throw new UnsupportedOperationException();
		}
		@Override
		public String getAuthTokenLabel(String authTokenType)
		{
			Log.i(MainActivity.LOG_TAG,"Add account: authTokenType="+authTokenType);
			throw new UnsupportedOperationException();
		}
		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response,Account account,String authTokenType,Bundle options) throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" account="+account+" authTokenType="+authTokenType+" options="+options);
			throw new UnsupportedOperationException();
		}
		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response,Account account,String[]features)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" account="+account+" features="+Arrays.toString(features));
			throw new UnsupportedOperationException();
		}
	}
}
