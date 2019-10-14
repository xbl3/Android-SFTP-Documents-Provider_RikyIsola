package com.island.androidsftpdocumentsprovider;
import android.accounts.*;
import android.content.*;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.webkit.*;
import com.island.sftp.*;
import java.io.*;
import java.util.*;
public class SyncAdapter extends AbstractThreadedSyncAdapter
{
	private final ContentResolver contentResolver;
	public SyncAdapter(Context context,boolean autoInitiate)
	{
		super(context,autoInitiate);
		contentResolver=context.getContentResolver();
		Log.i("Created sync adapter");
	}
	public SyncAdapter(Context context,boolean autoInitiate,boolean allowParallelSyncs)
	{
		super(context,autoInitiate,allowParallelSyncs);
		contentResolver=context.getContentResolver();
		Log.i("Created sync adapter");
	}
	@Override
	public void onPerformSync(Account account,Bundle extras,String authority,ContentProviderClient provider,SyncResult syncResult)
	{
		try
		{
			Log.i(String.format("OnPerformSync: account=%s extras=%s authority=%s provider=%s syncResult=%s",account,extras,authority,provider,syncResult));
			AccountManager accountManager=(AccountManager)Objects.requireNonNull(getContext()).getSystemService(Context.ACCOUNT_SERVICE);
			Log.d("Got account manager instance");
			String token=accountManager.getAuthToken(account,AuthenticationActivity.TOKEN_TYPE,null,false,null,null).getResult().getString(AccountManager.KEY_AUTHTOKEN);
			Log.d("Got token");
			String startDirectory=accountManager.getUserData(account,AuthenticationActivity.START_DIRECTORY);
			Log.d(String.format("Got start directory %s",startDirectory));
			Log.i("Connecting to the server");
			try(SFTP sftp=new SFTP(token,AuthenticationActivity.TIMEOUT,Log.logger))
			{
				File root=new File("/");
				Log.d("Connected");
				Log.i("Downloading and uploading data");
				sync(sftp,new Cache(getContext(),account.name,Log.logger),root);
				Log.i("Clean up");
			}
		}
		catch(Exception e)
		{
			Log.e("Exception when syncing",e);
			syncResult.stats.numAuthExceptions++;
		}
	}
	private static void sync(SFTP sftp,Cache cache,File folder)throws IOException
	{
		List<File>remotes=new ArrayList<File>(Arrays.asList(sftp.listFiles(folder)));
		List<File>locals=new ArrayList<File>(Arrays.asList(cache.listFiles(folder)));
		List<File>localsCopy=new ArrayList<File>(locals);
		locals.removeAll(remotes);
		remotes.removeAll(localsCopy);
		for(File file:locals)cache.delete(file);
		for(File file:remotes)
		{
			copy(sftp,cache,file);
		}
		localsCopy.removeAll(remotes);
		localsCopy.removeAll(locals);
		for(File file:localsCopy)if(cache.isDirectory(file))sync(sftp,cache,file);
	}
	private static void copy(SFTP sftp,Cache cache,File file)throws IOException
	{
		if(sftp.isDirectory(file))
		{
			for(File child:sftp.listFiles(file))copy(sftp,cache,child);
		}
		else
		{
			cache.newFile(file);
			if(getMimeType(file,sftp).startsWith("image/"))
			{
				cache.write(file,sftp.read(file));
			}
		}
	}
	/**
	 * Return the mime type of the file
	 * @return The mime type of the file
	 */
	static String getMimeType(File file,FileOperator fo)throws IOException
	{
        if(fo.isDirectory(file))
		{
			return DocumentsContract.Document.MIME_TYPE_DIR;
		}
		else
		{
			//Get the extension
			String name=file.getName();
			int lastDot=name.lastIndexOf('.');
			if(lastDot>=0)
			{
				String extension=name.substring(lastDot+1);

				//Get the mime type from android library
				final String mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
				if(mime!=null)return mime;
			}

			//Return generic type if there is no extension
			return"application/octet-stream";
		}
    }
}
