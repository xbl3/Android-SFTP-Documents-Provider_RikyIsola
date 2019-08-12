package com.island.androidftpdocumentprovider;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.webkit.*;
import com.enterprisedt.net.ftp.*;
import com.island.androidftpdocumentprovider.*;
import java.io.*;
import java.util.*;
public class FTPFile
{
	private final String path;
	private final long size;
	private final long lastModified;
	private final boolean directory;
	private final String ip;
	private final int port;
	private final String user;
	private final String password;
	public FTPFile(String ip,int port,String user,String password,String path,long lastModified,long size,boolean directory)
	{
		this.path=path;
		this.size=size;
		this.lastModified=lastModified;
		this.directory=directory;
		this.ip=ip;
		this.port=port;
		this.user=user;
		this.password=password;
	}
	public FTPFile(String ip,int port,String user,String password,String path)
	{
		this(ip,port,user,password,path,0,0,true);
	}
	public FTPFile(FTPFile file,String path)
	{
		this(file.ip,file.port,file.user,file.password,fipe.path+"/"+path);
	}
	public String getIp()
	{
		return ip;
	}
	public int getPort()
	{
		return port;
	}
	public String getPath()
	{
		return path;
	}
	public long getSize()
	{
		return size;
	}
	public long lastModified()
	{
		return lastModified;
	}
	public boolean isDirectory()
	{
		return directory;
	}
	public FTPFile[]listFiles()throws IOException
	{
		try
		{
			String path=this.path;
			FTPClient client=new FTPClient(ip,port);
			client.login(user,password);
			String[]files=client.dir(path,false);
			String[]fileDetails=client.dir(path,true);
			if(!path.isEmpty()&&path.charAt(path.length()-1)!='/')path+="/";
			FTPFile[]list=new FTPFile[files.length];
			for(int a=0;a<files.length;a++)
			{
				String file=files[a];
				String fileDetail=fileDetails[a];
				Scanner scanner=new Scanner(fileDetail);
				for(int b=0;b<4;b++)scanner.next();
				String filePath=path+file;
				long modTime=client.modtime(path+file).getTime();
				long size=scanner.nextLong();
				boolean directory=fileDetail.charAt(0)=='d';
				list[a]=new FTPFile(ip,port,user,password,filePath,modTime,size,directory);
				scanner.close();
			}
			client.quit();
			return list;
		}
		catch(FTPException e)
		{
			throw new IOException("Cannot list files",e);
		}
	}
	public String getName()
	{
		return path.substring(path.lastIndexOf("/")+1);
	}
	public void download(File local)throws IOException
	{
		try
		{
			FTPClient client=new FTPClient(ip,port);
			client.login(user,password);
			client.get(local.getPath(),path);
			client.quit();
		}
		catch(FTPException e)
		{
			throw new IOException("Error downloading file "+toString(),e);
		}
	}
	public void asyncUpload(final File local)
	{
		new AsyncTask()
		{
			@Override
			protected Object doInBackground(Object[]parameter)
			{
				try
				{
					FTPClient client=new FTPClient(ip,port);
					client.login(user,password);
					client.put(local.getPath(),path,false);
				}
				catch(Exception e)
				{
					Log.e(MainActivity.LOG_TAG,"Error saving document "+toString(),e);
				}
				return null;
			}
		}.doInBackground(null);
	}
	@Override
	public String toString()
	{
		return path;
	}
	public String getMimeType()
	{
        if(isDirectory())
		{
			return DocumentsContract.Document.MIME_TYPE_DIR;
		}
		else
		{
			String name=getName();
			int lastDot=name.lastIndexOf('.');
			if(lastDot>=0)
			{
				String extension=name.substring(lastDot+1);
				final String mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
				if(mime!=null)return mime;
			}
			return"application/octet-stream";
		}
    }
}
