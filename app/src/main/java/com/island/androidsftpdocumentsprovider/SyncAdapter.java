package com.island.androidsftpdocumentsprovider;
import android.accounts.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.util.*;
import java.io.*;
import com.jcraft.jsch.*;
public class SyncAdapter extends AbstractThreadedSyncAdapter
{
	private final ContentResolver contentResolver;
	public SyncAdapter(Context context,boolean autoInitiate)
	{
		super(context,autoInitiate);
		contentResolver=context.getContentResolver();
		Log.i(AuthenticationActivity.LOG_TAG,"Created sync adapter");
	}
	public SyncAdapter(Context context,boolean autoInitiate,boolean allowParallelSyncs)
	{
		super(context,autoInitiate,allowParallelSyncs);
		contentResolver=context.getContentResolver();
		Log.i(AuthenticationActivity.LOG_TAG,"Created sync adapter");
	}
	@Override
	public void onPerformSync(Account account,Bundle extras,String authority,ContentProviderClient provider,SyncResult syncResult)
	{
		try
		{
			Log.i(AuthenticationActivity.LOG_TAG,String.format("OnPerformSync: account=%s extras=%s authority=%s provider=%s syncResult=%s",account,extras,authority,provider,syncResult));
			AccountManager accountManager=(AccountManager)Objects.requireNonNull(getContext()).getSystemService(Context.ACCOUNT_SERVICE);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Got account manager instance");
			String token=accountManager.getAuthToken(account,AuthenticationActivity.TOKEN_TYPE,null,false,null,null).getResult().getString(AccountManager.KEY_AUTHTOKEN);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Got token");
			String startDirectory=accountManager.getUserData(account,AuthenticationActivity.START_DIRECTORY);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Got start directory %s",startDirectory));
			Log.i(AuthenticationActivity.LOG_TAG,"Connecting to the server");
			try(SFTP sftp=new SFTP(token))
			{
				File root=new File("/");
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Connected");
				Log.i(AuthenticationActivity.LOG_TAG,"Downloading and uploading data");
			
				Log.i(AuthenticationActivity.LOG_TAG,"Handling data conflicts");
				Log.i(AuthenticationActivity.LOG_TAG,"Clean up");
			}
		}
		catch(Exception e)
		{
			Log.e(AuthenticationActivity.LOG_TAG,"Exception when syncing",e);
			syncResult.stats.numAuthExceptions++;
		}
	}
}
