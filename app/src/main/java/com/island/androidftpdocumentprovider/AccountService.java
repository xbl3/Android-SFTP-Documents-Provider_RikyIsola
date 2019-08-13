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
		public Bundle editProperties(AccountAuthenticatorResponse p1,String p2)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,String accountType,String authTokenType,String[]requiredFeatures,Bundle options)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" accountType="+accountType+" authTokenType="+authTokenType+" requiredFeatures="+Arrays.toString(requiredFeatures)+" options="+options);
			return null;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse p1,Account p2,Bundle p3) throws NetworkErrorException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse p1,Account p2,String p3,Bundle p4) throws NetworkErrorException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public String getAuthTokenLabel(String p1)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse p1,Account p2,String p3,Bundle p4) throws NetworkErrorException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse p1,Account p2,String[] p3) throws NetworkErrorException
		{
			throw new UnsupportedOperationException();
		}
		
	}
}
