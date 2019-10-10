package com.island.androidsftpdocumentsprovider;
import android.util.*;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.*;
import java.io.*;
import java.net.*;
import java.util.*;
public class SFTP implements Closeable,FileOperator
{
	//Token: username@host:port?password/startdirectory
	final String ip;
	final int port;
	final String user;
	final String password;
	final String initialPath;
	final Session session;
	final ChannelSftp channel;
	private final HashMap<File,Long>lastModified=new HashMap<>();
	private final HashMap<File,Long>size=new HashMap<>();
	private final HashMap<File,Boolean>directory=new HashMap<>();
	SFTP(String token)throws IOException
	{
		user=token.substring(0,token.indexOf("@"));
		ip=token.substring(token.indexOf("@")+1,token.indexOf(":"));
		port=Integer.valueOf(token.substring(token.indexOf(":")+1,token.indexOf("?")));
		password=token.substring(token.indexOf("?")+1,token.indexOf("/"));
		initialPath=token.substring(token.indexOf("/")+1);
		JSch jsch=new JSch();
		try
		{
			session=jsch.getSession(user,ip,port);
			session.setPassword(password);
			java.util.Properties config=new java.util.Properties();
			config.put("StrictHostKeyChecking","no");
			session.setConfig(config);
			session.setTimeout(AuthenticationActivity.TIMEOUT);
			session.setConfig("PreferredAuthentications","password");
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Connecting to %s@%s:%s",user,ip,port));
			session.connect();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,String.format("Connected to %s@%s:%s",user,ip,port));
			channel=(ChannelSftp)session.openChannel("sftp");
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Opening channel");
			channel.connect();
			if(BuildConfig.DEBUG)Log.d(AuthenticationActivity.LOG_TAG,"Channel opened");
		}
		catch(JSchException e)
		{
			throw new IOException(String.format("Can't connect to %s@%s:%s",user,ip,port));
		}
	}
	@Override
	public long lastModified(File file)
	{
		return lastModified.get(file);
	}
	@Override
	public long length(File file)
	{
		return size.get(file);
	}
	@Override
	public boolean isDirectory(File file)
	{
		return directory.get(file);
	}
	@Override
	public void close()
	{
		channel.quit();
		session.disconnect();
	}
	@Override
	public File[]listFiles(File file)throws IOException
	{
		try
		{
			Vector vector=channel.ls(file.getPath());
			File[]files=new File[vector.size()];
			for(int a=0;a<files.length;a++)
			{
				LsEntry entry=(LsEntry)vector.get(a);
				if(entry.getFilename().equals(".")||entry.getFilename().equals(".."))continue;
				File newFile=new File(file,entry.getFilename());
				SftpATTRS attributes=entry.getAttrs();
				files[a]=newFile;
				lastModified.put(newFile,attributes.getMTime());
			}
		}
		catch(SftpException e)
		{
			throw new IOException(e);
		}
	}
}
