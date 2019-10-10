package com.island.androidsftpdocumentsprovider;
import java.io.*;
public interface FileOperator
{
	long length(File file)throws IOException;
	long lastModified(File file)throws IOException;
	boolean isDirectory(File file)throws IOException;
	File[]listFiles(File file)throws IOException;
}
