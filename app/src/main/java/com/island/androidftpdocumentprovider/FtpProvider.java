package com.island.androidftpdocumentprovider;
import android.database.*;
import android.os.*;
import android.provider.*;
import android.provider.DocumentsContract.*;
import java.io.*;
public class FtpProvider extends DocumentsProvider
{
	private static final String[] DEFAULT_ROOT_PROJECTION =
	new String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_MIME_TYPES,
        Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_AVAILABLE_BYTES,};
	private static final String[] DEFAULT_DOCUMENT_PROJECTION = new
	String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS, Document.COLUMN_SIZE,};
	
	@Override
	public boolean onCreate()
	{
		// TODO: Implement this method
		return false;
	}
	@Override
	public Cursor queryRoots(String[] projection)throws FileNotFoundException
	{
		final MatrixCursor result=new MatrixCursor(resolveRootProjection(projection));
		return null;
	}
	@Override
	public Cursor queryDocument(String p1,String[] p2) throws FileNotFoundException
	{
		// TODO: Implement this method
		return null;
	}
	@Override
	public Cursor queryChildDocuments(String p1,String[] p2,String p3) throws FileNotFoundException
	{
		// TODO: Implement this method
		return null;
	}
	@Override
	public ParcelFileDescriptor openDocument(String p1,String p2,CancellationSignal p3) throws FileNotFoundException
	{
		// TODO: Implement this method
		return null;
	}
}
