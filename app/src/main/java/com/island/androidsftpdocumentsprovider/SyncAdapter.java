package com.island.androidsftpdocumentsprovider;
import android.accounts.*;
import android.content.*;
import android.os.*;
import android.provider.*;
import android.webkit.*;
import com.island.sftp.*;
import java.io.*;
import java.util.*;
public class SyncAdapter extends AbstractThreadedSyncAdapter
{
	SyncAdapter(Context context,boolean autoInitiate)
	{
		super(context,autoInitiate);
		Log.i("Created sync adapter");
	}
	@Override
	public void onPerformSync(Account account,Bundle extras,String authority,ContentProviderClient provider,SyncResult syncResult)
	{
		try
		{
			//Get the authentication token and performs sync
			Log.i(String.format("OnPerformSync: account=%s extras=%s authority=%s provider=%s syncResult=%s",account,extras,authority,provider,syncResult));
			AccountManager accountManager=Objects.requireNonNull((AccountManager)Objects.requireNonNull(getContext()).getSystemService(Context.ACCOUNT_SERVICE));
			Log.d("Got account manager instance");
			String token=Objects.requireNonNull(accountManager.getAuthToken(account,AuthenticationActivity.TOKEN_TYPE,null,false,null,null).getResult().getString(AccountManager.KEY_AUTHTOKEN));
			Log.d("Got token");
			String startDirectory=accountManager.getUserData(account,AuthenticationActivity.START_DIRECTORY);
			Log.d(String.format("Got start directory %s",startDirectory));
			boolean hiddenFolders=Boolean.valueOf(accountManager.getUserData(account,AuthenticationActivity.HIDDEN_FOLDERS));
			Log.d(String.format("Sync hidden folders %s",hiddenFolders));
			Log.i("Connecting to the server");
			try(SFTP sftp=new SFTP(token,AuthenticationActivity.TIMEOUT,Log.logger))
			{
				File root=new File("/");
				Log.d("Connected");
				Log.i("Downloading and uploading data");
				sync(sftp,new Cache(getContext().getExternalCacheDir(),account.name,Log.logger),root,hiddenFolders);
				Log.i("Clean up");
			}
		}
		catch(Exception e)
		{
			Log.e("Exception when syncing",e);
			syncResult.stats.numAuthExceptions++;
		}
	}
	/**
	 * Sync a folder with a remote server
	 * @param sftp The remote sftp server
	 * @param cache The local files
	 * @param folder The folder to sync
	 * @param hiddenFolders If the hidden folders should be included
	 * @throws IOException A network exception
	 */
	private static void sync(SFTP sftp,Cache cache,File folder,boolean hiddenFolders)throws IOException
	{
		//Sync the folder with the remote server
		Log.d(String.format("Syncing %s",folder));
		List<File>remotes=new ArrayList<>(Arrays.asList(sftp.listFiles(folder)));
		List<File>locals=new ArrayList<>(Arrays.asList(cache.listFiles(folder)));
		List<File>localsCopy=new ArrayList<>(locals);
		locals.removeAll(remotes);
		remotes.removeAll(localsCopy);
		
		//Delete all files that no longer exist
		for(File file:locals)cache.delete(file);
		
		//Copy any missing file
		for(File file:remotes)
		{
			copy(sftp,cache,file,hiddenFolders);
		}
		
		//Control every folder
		localsCopy.removeAll(remotes);
		localsCopy.removeAll(locals);
		for(File file:localsCopy)
		{
			if(cache.isDirectory(file))sync(sftp,cache,file,hiddenFolders);
			else
			{
				long size=cache.length(file);
				if(size!=0&&size!=sftp.length(file))
				{
					Log.d(String.format("Updating %s",file));
					cache.delete(file);
					cache.newFile(file);
					if(getMimeType(file,sftp).startsWith("image/"))
					{
						FileOperation.copy(sftp,cache,file);
					}
					cache.setLastModified(file,sftp.lastModified(file));
				}
			}
		}
	}
	/**
	 * Copy a folder from a remote server
	 * @param sftp The remote sftp server
	 * @param cache The local files
	 * @param file The folder to sync
	 * @param hiddenFolders If the hidden folders should be included
	 */
	private static void copy(SFTP sftp,Cache cache,File file,boolean hiddenFolders)
	{
		try
		{
			//Skip hidden files
			if(file.getName().startsWith(".")&&!hiddenFolders)
			{
				Log.d(String.format("Skipping %s",file));
				return;
			}
			Log.d(String.format("Copying %s",file));
			if(sftp.isDirectory(file))
			{
				//Copy the content of the folder
				for(File child:sftp.listFiles(file))copy(sftp,cache,child,hiddenFolders);
			}
			else
			{
				//Copy the meta data of the file and eventually the thumbnail
				cache.newFile(file);
				if(getMimeType(file,sftp).startsWith("image/"))
				{
					FileOperation.copy(sftp,cache,file);
				}
				cache.setLastModified(file,sftp.lastModified(file));
			}
		}
		catch(IOException e)
		{
			Log.e(String.format("Error copying %s",file),e);
		}
	}
	/**
	 * Return the mime type of the file
	 * @param file The file to get the mime type
	 * @param fo The file operator to get the info from
	 * @return The mime type of the file
	 * @throws IOException A network exception
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
