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
	public static class Authenticator extends AbstractAccountAuthenticator
	{
		public Authenticator(Context context)
		{
			super(context);
			this.context=context;
		}
		private final Context context;
		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response,String accountType)
		{
			Log.i(MainActivity.LOG_TAG,"Edit account properties: response="+response+" accountType="+accountType);
			return null;
		}
		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,String accountType,String authTokenType,String[]requiredFeatures,Bundle options)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" accountType="+accountType+" authTokenType="+authTokenType+" requiredFeatures="+Arrays.toString(requiredFeatures)+" options="+options);
			Intent intent=new Intent(context,AuthenticationActivity.class);
			intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,MainActivity.ACCOUNT_TYPE);
			intent.putExtra(AuthenticationActivity.EXTRA_ADD_ACCOUNT,true);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,response);
			Bundle bundle=new Bundle();
			bundle.putParcelable(AccountManager.KEY_INTENT,intent);
			return bundle;
		}
		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response,Account account,Bundle options)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" account="+account+" options="+options);
			return null;
		}
		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response,Account account,String authTokenType,Bundle options)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" account="+account+" authTokenType="+authTokenType+" options="+options);
			AccountManager accountManager=AccountManager.get(context);
			String authToken=accountManager.peekAuthToken(account,authTokenType);
			if(authToken==null)
			{
				String password=accountManager.getPassword(account);
				if(password!=null)
				{
					authToken=account.name+"/"+password;
				}
			}
			Bundle result=new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME,account.name);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE,account.type);
			result.putString(AccountManager.KEY_AUTHTOKEN,authToken);
			return result;
		}
		@Override
		public String getAuthTokenLabel(String authTokenType)
		{
			Log.i(MainActivity.LOG_TAG,"Add account: authTokenType="+authTokenType);
			return"full";
		}
		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response,Account account,String authTokenType,Bundle options) throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" account="+account+" authTokenType="+authTokenType+" options="+options);
			return null;
		}
		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response,Account account,String[]features)throws NetworkErrorException
		{
			Log.i(MainActivity.LOG_TAG,"Add account: response="+response+" account="+account+" features="+Arrays.toString(features));
			Bundle result=new Bundle();
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,false);
			return result;
		}
	}
}
