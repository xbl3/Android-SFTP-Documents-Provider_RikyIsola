package com.island.androidsftpdocumentsprovider;
import android.content.*;
import android.os.*;
import com.island.sftp.*;
import java.io.*;
import java.util.logging.*;

import android.os.Handler;
import android.app.*;
public class AsyncCopy extends AsyncTask<File,Long,Void>
{
	AsyncCopy(Context context,Cache cache,String token,int timeout,Logger logger)
	{
		this.cache=cache;
		this.token=token;
		this.timeout=timeout;
		this.logger=logger;
		this.context=context;
	}
	private final Cache cache;
	private final String token;
	private final int timeout;
	private final Logger logger;
	private final Context context;
	private File processing;
	@Override
	protected Void doInBackground(File[]files)
	{
		try
		{
			try(SFTP sftp=new SFTP(token,timeout,logger))
			{
				long total=0;
				for(File file:files)total+=cache.length(file);
				long wrote=0;
				long update=total/100;
				long step=update;
				byte[]buffer=new byte[FileOperation.BUFFER];
				int bytesRead;
				for(File file:files)
				{
					processing=file;
					InputStream input=cache.read(file);
					OutputStream output=sftp.write(file);
					while((bytesRead=FileOperation.write(input,output,buffer))!=-1)
					{
						wrote+=bytesRead;
						if(wrote>step)
						{
							publishProgress(total,wrote);
							while(wrote>step)step+=update;
						}
					}
				}
			}
		}
		catch(IOException e)
		{
			Log.e("Can't asynchronously copy",e);
		}
		return null;
	}
	@Override
	protected void onPostExecute(Void result)
	{
		Intent intent=new Intent(context,UploaderService.class);
		context.stopService(intent);
	}
	@Override
	protected void onProgressUpdate(Long[]values)
	{
		int progress=(int)(values[1]*100/values[0]);
		NotificationManager notificationManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification=UploaderService.getNotification(context,processing.getName(),progress);
		notificationManager.notify(UploaderService.ONGOING_NOTIFICATION_ID,notification);
	}
}
