package com.island.androidsftpdocumentsprovider.account;
import android.accounts.*;
import android.content.*;
import android.os.*;
import android.provider.*;
import android.util.*;
import com.island.androidsftpdocumentsprovider.provider.*;
import java.util.*;
public class AccountAuthenticator extends AbstractAccountAuthenticator
{
	AccountAuthenticator(Context context)
	{
		super(context);
		this.context=context;
	}
	private final Context context;
	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response,String accountType)
	{
		return null;
	}
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response,String accountType,String authTokenType,String[]requiredFeatures,Bundle options)
	{
		Log.i(SFTPProvider.TAG,String.format("AccountAuthenticator addAccount %s %s %s %s %s",response,accountType,authTokenType,Arrays.toString(requiredFeatures),options));
		Objects.requireNonNull(response);
		Objects.requireNonNull(accountType);
		Intent intent=new Intent(context,AuthenticationActivity.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,accountType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,response);
		Bundle bundle=new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT,intent);
		return bundle;
	}
	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,Account account,Bundle options)
	{
		return null;
	}
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,Account account,String authTokenType,Bundle options)
	{
		Log.i(SFTPProvider.TAG,String.format("AccountAuthenticator addAccount %s %s %s %s",response,account,authTokenType,options));
		Objects.requireNonNull(account);
		Objects.requireNonNull(authTokenType);
		AccountManager accountManager=AccountManager.get(context);
		String authToken=accountManager.peekAuthToken(account,authTokenType);
		if(authToken==null)authToken=accountManager.getPassword(account);
		Bundle result=new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME,account.name);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE,account.type);
		result.putString(AccountManager.KEY_AUTHTOKEN,authToken);
		return result;
	}
	@Override
	public String getAuthTokenLabel(String authTokenType)
	{
		return"full";
	}
	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,Account account,String authTokenType,Bundle options)
	{
		return null;
	}
	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response,Account account,String[]features)
	{
		Bundle result=new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,false);
		return result;
	}
	@Override
	public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response,Account account)throws NetworkErrorException
	{
		Log.i(SFTPProvider.TAG,String.format("AccountAuthenticator getAccountRemoval %s %s",response,account));
		context.getContentResolver().notifyChange(DocumentsContract.buildRootsUri(AuthenticationActivity.AUTHORITY),null);
		return super.getAccountRemovalAllowed(response,account);
	}
}
