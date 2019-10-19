package com.island.sftp;
import java.io.*;
public interface FileOperator
{
	long length(File file)throws IOException;
	long lastModified(File file)throws IOException;
	boolean isDirectory(File file)throws IOException;
	File[]listFiles(File file)throws IOException;
	void delete(File file)throws IOException;
	void write(File file,InputStream input)throws IOException;
	InputStream read(File file)throws IOException;
	void newFile(File file)throws IOException;
	void setLastModified(File file,long lastModified)throws IOException;
	void mkdirs(File file)throws IOException;
}
