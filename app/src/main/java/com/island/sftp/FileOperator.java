package com.island.sftp;
import java.io.*;
public interface FileOperator
{
	/**
	 * Get the size of the file
	 * @param file The file to use
	 * @return The size of the file
	 * @throws IOException If any error happens
	 */
	long length(File file)throws IOException;
	/**
	 * Get the last modification time of the file
	 * @param file The file to use
	 * @return The last modification time of the file
	 * @throws IOException If any error happens
	 */
	long lastModified(File file)throws IOException;
	/**
	 * Get if the file is a directory
	 * @param file The file to use
	 * @return If the file is a directory
	 * @throws IOException If any error happens
	 */
	boolean isDirectory(File file)throws IOException;
	/**
	 * Get if the file is a file
	 * @param file The file to use
	 * @return If the file is a file
	 * @throws IOException If any error happens
	 */
	boolean isFile(File file)throws IOException;
	/**
	 * List the file of a folder
	 * @param file The file to use
	 * @return A list containing all the childs of the folder
	 * @throws IOException If any error happens
	 */
	File[]listFiles(File file)throws IOException;
	/**
	 * Delete a file or folder
	 * @param file The file to use
	 * @throws IOException If any error happens
	 */
	void delete(File file)throws IOException;
	OutputStream write(File file)throws IOException;
	void renameTo(File oldPath,File newPath)throws IOException;
	/**
	 * Read the content of a file
	 * @param file The file to use
	 * @return An input stream containing the content of the file
	 * @throws IOException If any error happens
	 */
	InputStream read(File file)throws IOException;
	/**
	 * Create a new file
	 * @param file The file to use
	 * @throws IOException If any error happens
	 */
	void newFile(File file)throws IOException;
	/**
	 * Set the last modification time of a file or folder
	 * @param file The file to use
	 * @param lastModified The last modification time to set
	 * @throws IOException If any error happens
	 */
	void setLastModified(File file,long lastModified)throws IOException;
	/**
	 * Create a directory
	 * @param file The file to use
	 * @throws IOException If any error happens
	 */
	void mkdirs(File file)throws IOException;
	/**
	 * Check if the file exists
	 * @param file The file to use
	 * @return If the file exists
	 * @throws IOException If any error happens
	 */
	boolean exists(File file)throws IOException;
}
