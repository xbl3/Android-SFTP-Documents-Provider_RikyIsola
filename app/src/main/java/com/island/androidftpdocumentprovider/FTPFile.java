package com.island.androidftpdocumentprovider;
import com.enterprisedt.net.ftp.*;
import java.io.*;
public class FTPFile
{
	public FTPFile(String ip,int port,String path)
	{
		
	}
	public static FTPFile[]list(FTPClient client,String path)throws IOException,FTPException
	{
		String[]files=client.dir();
	}
}
