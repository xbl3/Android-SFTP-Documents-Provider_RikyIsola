package com.island.androidftpdocumentsprovider;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.webkit.*;
import com.island.androidftpdocumentsprovider.*;
import java.io.*;
import java.util.*;
import com.jcraft.jsch.*;
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
	private final ChannelSftp channel;
	public FTPFile(ChannelSftp channel,String ip,int port,String user,String password,String path,long lastModified,long size,boolean directory) throws IOException
	{
		if(channel==null)channel=createChannel(ip,port,user,password);
		this.channel=channel;
		this.channel=channel;
		this.path=path;
		this.size=size;
		this.lastModified=lastModified;
		this.directory=directory;
		this.ip=ip;
		this.port=port;
		this.user=user;
		this.password=password;
	}
	public FTPFile(ChannelSftp channel,String ip,int port,String user,String password,String path)throws IOException
	{
		if(channel==null)channel=createChannel(ip,port,user,password);
		this.channel=channel;
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
		this(file.channel,file.ip,file.port,file.user,file.password,file.path+"/"+path);
	}
	public FTPFile(FTPFile file,String path,long lastModified,long size,boolean directory)throws IOException
	{
		this(file.channel,file.ip,file.port,file.user,file.password,file.path+"/"+path,lastModified,size,directory);
	}
	private static ChannelSftp createChannel(String ip,int port,String user,String password)throws IOException
	{
		try
		{
			JSch jsch=new JSch();
			Session session=jsch.getSession(user, ip, port);
			session.setPassword(password);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setTimeout(AuthenticationActivity.TIMEOUT);
			session.setConfig("PreferredAuthentications", "password");
			session.connect();
			ChannelSftp channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			return channel;
		}
		catch(JSchException e)
		{
			throw new IOException("Cannot create a sftp channel",e);
		}
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
	public String getUser()
	{
		return user;
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
			
			Vector files=channel.ls("/"+path);
            List<FTPFile>list=new ArrayList<>();
			
			if(!path.isEmpty()&&path.charAt(path.length()-1)!='/')path+="/";
			for(Object obj:files)
			{
                ChannelSftp.LsEntry file=(ChannelSftp.LsEntry)obj;
				String filePath=path+file.getFilename();
                SftpATTRS attrs=file.getAttrs();
				long modTime=attrs.getMTime();
				long size=attrs.getSize();
				boolean directory=attrs.isDir();
				list.add(new FTPFile(channel,ip,port,user,password,filePath,modTime,size,directory));
			}
			return list.toArray(new FTPFile[list.size()]);
		}
        catch(SftpException e)
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
			channel.get(path,local.getPath());
		}
		catch(SftpException e)
		{
			throw new IOException("Error downloading file "+toString(),e);
		}
	}
	public void upload(File local)throws IOException
	{
		try
		{
			channel.put(local.getPath(),path);
		}
		catch(SftpException e)
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
					Log.e(AuthenticationActivity.LOG_TAG,"Error uploading document "+FTPFile.this.toString(),e);
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
	public FTPFile getParentFile()throws IOException
	{
		int end=path.lastIndexOf("/");
		if(end==-1)return null;
		else return new FTPFile(channel,ip,port,user,password,path.substring(0,end),0,0,true);
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
			channel.put(path);
		}
		catch(SftpException e)
		{
			throw new IOException("Error creating new file "+toString(),e);
		}
	}
	public void mkdir()throws IOException
	{
		try
		{
			if(exist())throw new IOException("Directory already exist");
			channel.mkdir(path);
		}
		catch(SftpException e)
		{
			throw new IOException("Error creating directory "+toString(),e);
		}
	}
	public void delete()throws IOException
	{
		try
		{
			if(!exist())throw new FileNotFoundException("File "+toString()+" not found");
			if(directory)
			{
				FTPFile[]files=listFiles();
				for(FTPFile file:files)file.delete();
				channel.rmdir(path);
			}
			else channel.rm(path);
		}
		catch(SftpException e)
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
