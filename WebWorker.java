//Zachary Garcia
//CS 371
//Program 2

/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class WebWorker implements Runnable
{

private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      
	  String filePath = readHTTPRequest(is); //The file path returned from readHTTPRequest is stored
	  filePath = filePath.substring(1); //The '/' character is removed from the string
	  File f = new File(filePath); //A new file object is initialized with the given file path
	  String[] s1 = filePath.split("/");//Substring is created containing just the file name
	  String fileName = s1[s1.length-1];
	  String fileType = fileName.substring(fileName.lastIndexOf(".")+1);//Substring is created that contains the filetype
	  String contentType = "";
	  //content type is set to the appropriate type
	  switch (fileType) {
	    case "jpg": contentType = "image/jpg"; break;
		case "png": contentType = "image/png"; break;
		case "gif": contentType = "image/gif"; break;
		case "html": contentType = "text/html"; break;
	  }
	  
	  writeHTTPHeader(os,contentType, f.exists()); //The writeHTTPHeader function is called with an added parameter	                                               //indicating whether the file exists
	  if(!f.exists())
	    os.write("404 Not Found".getBytes());//An error message is written if the file is not found
	  else {
		if(fileType.equals("html"))
			write(os, filePath, fileType);//if the file is an html file, the write function is called
		else
			writeImage(os, filePath, fileType);//if the file is an image, the writeImage function is called
	  }
	
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   String line;
   String filePath = "";
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   try {
     String get = r.readLine();//the first line read from the input stream which contains the get request is stored
     String[] s = get.split(" ");//the string is split into sub strings, the split occurs at each space
     filePath = s[1]; //the file path is located in the second substring of the array
   } catch (Exception e) {
       System.err.println("Request error: "+e);
   }

   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return filePath;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType, boolean found) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   if (found)
     os.write("HTTP/1.1 200 OK\n".getBytes());//The http status is set to 200 if a file exists with the requested path
   else
     os.write("HTTP/1.1 404 Not Found\n".getBytes());//The status is changed to 404 Not Found if it does not exist
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: cs371 test server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/	
private void write(OutputStream os, String filePath, String fileType) throws Exception
{
   //The contents of the file with the requested path are written line by line
   //by initializing a BufferedReader object for the file
   
     BufferedReader in = new BufferedReader(new FileReader(filePath));
     String line;
     line = in.readLine();
     while(line != null) {
	   if (line.equals("<cs371dat>"))
	     os.write("Date: 2/2/2018".getBytes());
       if (line.equals("<cs371server>"))
	     os.write(" Server: cs371 test server\n".getBytes());
       os.write(line.getBytes());//As each line is read, it is written
	   line = in.readLine();
     }
}

//function that converts an image into a byte array
private byte[] imgToBytes(String filePath, String fileType) throws Exception{
   BufferedImage i = ImageIO.read(new File(filePath));
   ByteArrayOutputStream b = new ByteArrayOutputStream();
   if (fileType.equals("jpg") || fileType.equals("jpeg")) ImageIO.write(i, "jpg", b);
   else if (fileType.equals("png")) ImageIO.write(i, "png", b);
   else if (fileType.equals("gif")) ImageIO.write(i, "gif", b);
   byte[] bytes = b.toByteArray();
   return bytes;
}

//function that writes the byte array of an image
private void writeImage(OutputStream os, String filePath, String fileType) throws Exception
{
   os.write(imgToBytes(filePath, fileType));
}

} // end class
