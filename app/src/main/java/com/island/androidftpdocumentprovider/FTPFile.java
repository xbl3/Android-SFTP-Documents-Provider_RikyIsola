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
	public FTPFile(String ip,int port,String user,String password,String path)throws IOException
	{
		this.path=path;
		this.ip=ip;
		this.port=port;
		this.user=user;
		this.password=password;
		long size=0;
		long lastModified=0;
		boolean directory=true;
		FTPFile parent=getParentFile();
		if(parent!=null)
		{
			FTPFile[]files=parent.listFiles();
			for(FTPFile file:files)if(equals(file))
			{
				size=file.size;
				lastModified=file.lastModified;
				directory=file.directory;
				break;
			}
		}
		this.size=size;
		this.lastModified=lastModified;
		this.directory=directory;
	}
	public FTPFile(FTPFile file,String path)throws IOException
	{
		this(file.ip,file.port,file.user,file.password,file.path+"/"+path);
	}
	public FTPFile(FTPFile file,String path,long lastModified,long size,boolean directory)
	{
		this(file.ip,file.port,file.user,file.password,file.path+"/"+path,lastModified,size,directory);
	}
	@Override
	public String toString()
	{
		return path;
	}
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof FTPFile)
		{
			FTPFile file=(FTPFile)obj;
			return path.equals(file.path);
		}
		return false;
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
			throw new IOException("Cannot list files of "+toString(),e);
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
	public void upload(File local)throws IOException
	{
		try
		{
			FTPClient client=new FTPClient(ip,port);
			client.login(user,password);
			client.put(local.getPath(),path,false);
			client.quit();
		}
		catch(FTPException e)
		{
			throw new IOException("Error uploading document "+FTPFile.this.toString(),e);
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
					upload(local);
				}
				catch(Exception e)
				{
					Log.e(MainActivity.LOG_TAG,"Error uploading document "+FTPFile.this.toString(),e);
				}
				return null;
			}
		}.execute();
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
	public FTPFile getParentFile()
	{
		int end=path.lastIndexOf("/");
		if(end==-1)return null;
		else return new FTPFile(ip,port,user,password,path.substring(0,end),0,0,true);
	}
	public boolean exist()throws IOException
	{
		FTPFile parent=getParentFile();
		FTPFile[]files=parent.listFiles();
		for(FTPFile file:files)if(equals(file))return true;
		return false;
	}
	public void createNewFile()throws IOException
	{
		try
		{
			if(exist())throw new IOException("File already exist");
			FTPClient client=new FTPClient(ip,port);
			client.login(user,password);
			client.put(new byte[0],path);
			client.quit();
		}
		catch(FTPException e)
		{
			throw new IOException("Error creating new file "+toString(),e);
		}
	}
	public void mkdir()throws IOException
	{
		try
		{
			if(exist())throw new IOException("Directory already exist");
			FTPClient client=new FTPClient(ip,port);
			client.login(user,password);
			client.mkdir(path);
			client.quit();
		}
		catch(FTPException e)
		{
			throw new IOException("Error creating directory "+toString(),e);
		}
	}
	public void delete()throws IOException
	{
		try
		{
			if(!exist())throw new FileNotFoundException("File "+toString()+" not found");
			FTPClient client=new FTPClient(ip,port);
			client.login(user,password);
			if(directory)
			{
				FTPFile[]files=listFiles();
				for(FTPFile file:files)file.delete();
				client.rmdir(path);
			}
			else client.delete(path);
			client.quit();
		}
		catch(FTPException e)
		{
			throw new IOException("Error deleting file "+toString(),e);
		}
	}
	public void copy(FTPFile dest,File cacheFolder,boolean move)throws IOException
	{
		if(isDirectory())
		{
			dest.mkdir();
			FTPFile[]files=listFiles();
			for(FTPFile file:files)
			{
				FTPFile newDest=new FTPFile(dest,file.getName(),file.lastModified,file.size,file.directory);
				file.copy(newDest,cacheFolder,false);
			}
		}
		else
		{
			File cache=new File(cacheFolder,getName());
			download(cache);
			dest.upload(cache);
			cache.delete();
		}
		if(move)delete();
	}
}
