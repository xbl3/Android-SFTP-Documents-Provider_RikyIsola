package com.island.androidsftpdocumentsprovider;
import android.accounts.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.util.*;
public class AccountAuthenticator extends AbstractAccountAuthenticator
{
	AccountAuthenticator(Context context)
	{
		super(context);
		Log.d("Created authenticator");
		this.context=context;
	}
	private final Context context;
	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response,String accountType)
	{
		Log.i(String.format("Edit account properties: response=%s accountType=%s",response,accountType));
		return null;
	}
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response,String accountType,String authTokenType,String[]requiredFeatures,Bundle options)
	{
		Log.i(String.format("Add account: response=%s accountType=%s authTokenType=%s requiredFeatures=%s options=%s",response,accountType,authTokenType,Arrays.toString(requiredFeatures),options));
		Intent intent=new Intent(context,AuthenticationActivity.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,AuthenticationActivity.ACCOUNT_TYPE);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,response);
		Bundle bundle=new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT,intent);
		return bundle;
	}
	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,Account account,Bundle options)
	{
		Log.i(String.format("Confirm account: response=%s account=%s options=%s",response,account,options));
		return null;
	}
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,Account account,String authTokenType,Bundle options)
	{
		Log.i(String.format("Get account token: response=%s account=%s authTokenType=%s options=%s",response,account,authTokenType,options));
		//Check if an athentication token already exist
		AccountManager accountManager=AccountManager.get(context);
		String authToken=accountManager.peekAuthToken(account,authTokenType);
		if(authToken==null)
		{
			//Create a new token using the password and the start directory
			Log.d("Creating a new authentication token");
			String password=accountManager.getPassword(account);
			if(password!=null)
			{
				String startDirectory=accountManager.getUserData(account,AuthenticationActivity.START_DIRECTORY);
				if(!startDirectory.startsWith("/"))startDirectory="/"+startDirectory;
				authToken=account.name+"?"+password+startDirectory;
			}
		}
		else Log.d("Using an old authentication token");
		//Post the result with a bundle
		Bundle result=new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME,account.name);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE,account.type);
		result.putString(AccountManager.KEY_AUTHTOKEN,authToken);
		return result;
	}
	@Override
	public String getAuthTokenLabel(String authTokenType)
	{
		Log.i(String.format("Get account token label: authTokenType=%s",authTokenType));
		return"full";
	}
	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,Account account,String authTokenType,Bundle options)
	{
		Log.i(String.format("Update account: response=%s account=%s authTokenType=%s options=%s",response,account,authTokenType,options));
		return null;
	}
	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response,Account account,String[]features)
	{
		Log.i(String.format("Account has feature: response=%s account=%s features=%s",response,account,Arrays.toString(features)));
		Bundle result=new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,false);
		return result;
	}
}
