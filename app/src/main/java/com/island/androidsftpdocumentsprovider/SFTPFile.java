package com.island.androidsftpdocumentsprovider;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.webkit.*;
import java.io.*;
import java.util.*;
import com.jcraft.jsch.*;
/**
 * This is the representation of a remote file
 */
class SFTPFile
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
	/**
	 * @param channel The channel to use
	 * @param ip The ip to connect
	 * @param port The port to use
	 * @param user The user to use
	 * @param password The password to use
	 * @param path The remote path
	 * @param lastModified The last time the file was modified
	 * @param size The size of the file
	 * @param directory If the file is a directory
	 */
	private SFTPFile(ChannelSftp channel,String ip,int port,String user,String password,String path,long lastModified,long size,boolean directory)
	{
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
	/**
	 * @param channel The channel to use
	 * @param ip The ip to connect
	 * @param port The port to use
	 * @param user The user to use
	 * @param password The password to use
	 * @param path The remote path
	 * @throws IOException If can't get the missing file info
	 */
	SFTPFile(ChannelSftp channel,String ip,int port,String user,String password,String path)throws IOException
	{
		if(channel==null)channel=createChannel(ip,port,user,password);
		this.channel=channel;
		this.path=path;
		this.ip=ip;
		this.port=port;
		this.user=user;
		this.password=password;
		
		//Add missing file info
		long size=0;
		long lastModified=0;
		boolean directory=true;
		SFTPFile parent=getParentFile();
		if(parent!=null)
		{
			SFTPFile[]files=parent.listFiles();
			for(SFTPFile file:files)if(equals(file))
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
	/**
	 * @param file The parent file
	 * @param path The child path
	 * @throws IOException If can't get the missing file info
	 */
	SFTPFile(SFTPFile file,String path)throws IOException
	{
		this(file.channel,file.ip,file.port,file.user,file.password,file.path+"/"+path);
	}
	/**
	 * @param file The parent file
	 * @param path The child path
	 * @param lastModified The last time the file was modified
	 * @param size The size of the file
	 * @param directory If the file is a directory
	 */
	SFTPFile(SFTPFile file,String path,long lastModified,long size,boolean directory)
	{
		this(file.channel,file.ip,file.port,file.user,file.password,file.path+"/"+path,lastModified,size,directory);
	}
	@Override
	public String toString()
	{
		//Return the path of the file
		return path;
	}
	@Override
	public boolean equals(Object obj)
	{
		//This object is equal to another remote file with the same path
		if(obj instanceof SFTPFile)
		{
			SFTPFile file=(SFTPFile)obj;
			return path.equals(file.path);
		}
		return false;
	}
	/**
	 * Return the ip of the host
	 * @return The ip
	 */
	String getIp()
	{
		return ip;
	}
	/**
	 * Return the port of the host
	 * @return The port
	 */
	int getPort()
	{
		return port;
	}
	/**
	 * Return the user of the remote file
	 * @return The user
	 */
	String getUser()
	{
		return user;
	}
	/**
	 * Return the path of the file
	 * @return The path
	 */
	String getPath()
	{
		return path;
	}
	/**
	 * Return the size of the file
	 * @return The size
	 */
	long getSize()
	{
		return size;
	}
	/**
	 * Return the last modified time of the file
	 * @return The last modified time
	 */
	long lastModified()
	{
		return lastModified;
	}
	/**
	 * Return if the file is a directory
	 * @return If the file is a directory
	 */
	boolean isDirectory()
	{
		return directory;
	}
	/**
	 * List all the files of the folder
	 * @return The list of the files
	 * @throws IOException If the connection fails
	 */
	SFTPFile[]listFiles()throws IOException
	{
		try
		{
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Listing files of %s",this));
			String path=this.path;
			
			//Get the remote files
			Vector files=channel.ls("/"+path);
            List<SFTPFile>list=new ArrayList<>();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Found %s files",files.size()));
			if(!path.isEmpty()&&path.charAt(path.length()-1)!='/')path+="/";
			
			//Add every file to the list
			for(Object obj:files)
			{
                ChannelSftp.LsEntry file=(ChannelSftp.LsEntry)obj;
                if(file.getFilename().equals(".")||file.getFilename().equals(".."))continue;
				String filePath=path+file.getFilename();
                SftpATTRS attrs=file.getAttrs();
				long modTime=attrs.getMTime();
				long size=attrs.getSize();
				boolean directory=attrs.isDir();
				list.add(new SFTPFile(channel,ip,port,user,password,filePath,modTime,size,directory));
			}
			
			return list.toArray(new SFTPFile[0]);
		}
        catch(SftpException e)
        {
            throw new IOException(String.format("Cannot list files of %s",this),e);
		}
	}
	/**
	 * Return the name of the file
	 * @return The name of the file
	 */
	String getName()
	{
		return path.substring(path.lastIndexOf("/")+1);
	}
	/**
	 * Download the remote file
	 * @param local Where to download
	 * @throws IOException If the connection fails
	 */
	void download(File local)throws IOException
	{
		try
		{
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Downloading %s to %s",this,local));
			channel.get(path,local.getPath());
		}
		catch(SftpException e)
		{
			throw new IOException(String.format("Error downloading file %s",this),e);
		}
	}
	/**
	 * Upload to the remote file
	 * @param local The file to upload
	 * @throws IOException If the connection fails
	 */
	private void upload(File local)throws IOException
	{
		try
		{
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Uploading %s to %s",local,this));
			channel.put(local.getPath(),path);
		}
		catch(SftpException e)
		{
			throw new IOException(String.format("Error uploading document to %s",this),e);
		}
	}
	/**
	 * Upload to the remote file in another thread
	 * @param local The file to upload
	 */
	void asyncUpload(final File local)
	{
		new Upload(this,local).execute();
		if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Started async task to upload %s to %s",local,this));
	}
	/**
	 * Return the mime type of the file
	 * @return The mime type of the file
	 */
	String getMimeType()
	{
        if(isDirectory())
		{
			return DocumentsContract.Document.MIME_TYPE_DIR;
		}
		else
		{
			//Get the extension
			String name=getName();
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
	/**
	 * Return the parent of this file
	 * @return The parent of the file
	 */
	SFTPFile getParentFile()
	{
		int end=path.lastIndexOf("/");
		if(end==-1)return null;
		else return new SFTPFile(channel,ip,port,user,password,path.substring(0,end),0,0,true);
	}
	/**
	 * Check if the remote file exist
	 * @return If the file exist
	 * @throws IOException If the connection fails
	 */
	private boolean exist()throws IOException
	{
		//If the file exist it should be in the parent child list
		SFTPFile parent=getParentFile();
		SFTPFile[]files=parent.listFiles();
		for(SFTPFile file:files)if(equals(file))return true;
		return false;
	}
	/**
	 * Create the remote file
	 * @throws IOException If the connection fails
	 */
	void createNewFile()throws IOException
	{
		try
		{
			if(exist())throw new IOException("File already exist");
			channel.put(path).close();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Created remote file %s",this));
		}
		catch(SftpException e)
		{
			throw new IOException(String.format("Error creating new file %s",this),e);
		}
	}
	/**
	 * Create the remote folder
	 * @throws IOException If the connection fails
	 */
	void mkdir()throws IOException
	{
		try
		{
			if(exist())throw new IOException("Directory already exist");
			channel.mkdir(path);
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Created remote folder %s",this));
		}
		catch(SftpException e)
		{
			throw new IOException(String.format("Error creating directory %s",this),e);
		}
	}
	/**
	 * Delete the remote file
	 * @throws IOException If the connection fails
	 */
	void delete()throws IOException
	{
		try
		{
			if(!exist())throw new FileNotFoundException(String.format("File %s not found",this));
			if(directory)
			{
				//Delete all the files of the folder and then the folder itself
				SFTPFile[]files=listFiles();
				for(SFTPFile file:files)file.delete();
				channel.rmdir(path);
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Deleted remote folder %s",this));
			}
			else
			{
				channel.rm(path);
				if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Created remote file %s",this));
			}
		}
		catch(SftpException e)
		{
			throw new IOException(String.format("Error deleting file %s",this),e);
		}
	}
	/**
	 * Copy the remote file to another destination
	 * @param dest The destination of the copy
	 * @param cacheFolder A local folder to temporary save the files to copy
	 * @param move If the copied files should be deleted
	 * @throws IOException If the connection fails
	 */
	void copy(SFTPFile dest,File cacheFolder,boolean move)throws IOException
	{
		if(isDirectory())
		{
			//If the file is a folder copy its content
			dest.mkdir();
			SFTPFile[]files=listFiles();
			for(SFTPFile file:files)
			{
				SFTPFile newDest=new SFTPFile(dest,file.getName(),file.lastModified,file.size,file.directory);
				file.copy(newDest,cacheFolder,false);
			}
		}
		else
		{
			//Download the file and upload it to the new location
			File cache=new File(cacheFolder,getName());
			download(cache);
			dest.upload(cache);
			if(!cache.delete())throw new IOException("Missing cache file");
		}
		if(move)delete();
	}
	/**
	 * Create a new connection
	 * @param ip The ip to connect
	 * @param port The port to connect
	 * @param user The user to use
	 * @param password The password to use
	 * @throws IOException If the connection fails
	 */
	static ChannelSftp createChannel(String ip,int port,String user,String password)throws IOException
	{
		try
		{
			JSch jsch=new JSch();
			Session session=jsch.getSession(user,ip,port);
			session.setPassword(password);
			java.util.Properties config=new java.util.Properties();
			config.put("StrictHostKeyChecking","no");
			session.setConfig(config);
			session.setTimeout(AuthenticationActivity.TIMEOUT);
			session.setConfig("PreferredAuthentications","password");
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Connecting to %s:%s@%s",ip,port,user));
			session.connect();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Connected to %s:%s@%s",ip,port,user));
			ChannelSftp channel=(ChannelSftp)session.openChannel("sftp");
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Opening channel");
			channel.connect();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Channel opened");
			return channel;
		}
		catch(JSchException e)
		{
			throw new IOException("Cannot create a SFTP channel",e);
		}
	}
	private static class Upload extends AsyncTask<Object,Object,Object>
	{
		private Upload(SFTPFile file,File local)
		{
			this.file=file;
			this.local=local;
		}
		private final SFTPFile file;
		private final File local;
		@Override
		protected Object doInBackground(Object[] parameter)
		{
			try
			{
				file.upload(local);
			}catch(Exception e)
			{
				Log.e(AuthenticationActivity.LOG_TAG,String.format("Error uploading document to %s",file),e);
			}
			return null;
		}
	}
}
