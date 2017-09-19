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
import java.util.StringTokenizer;
import javax.activation.MimetypesFileTypeMap;

public class WebWorker implements Runnable
{
// Socket object variable socket
private Socket socket;

// Boolean variable fileExists
private boolean fileExists = false;

// FileInputStream object variable input
private FileInputStream input;

// String variable fileName
private String fileName;

// Static String variable fileType
private String fileType;

// Static String variable type
private String type;

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
      // Print the following information to the termial window.
      InputStream is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
      writeHTTPHeader(os, type);
      writeContent(os);
      os.flush();
      socket.close();
      input.close();
   } catch (Exception e) {
      // Print output error to terminal window. 
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private void readHTTPRequest(InputStream is)
{
   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   int checkForFile = 1;
   
   while (true) {
      try {
         while (!r.ready()) {
            Thread.sleep(1);
         }
         line = r.readLine();
         if (checkForFile > 0) {
            // Divide each line into tokens.
            StringTokenizer tokens = new StringTokenizer(line);
            tokens.nextToken();
            fileName = tokens.nextToken();
            fileName = "." + fileName;
            input = null;
            
            //Change fileExists to true.
            fileExists = true;
            
            // Determine the type of the image file 
            if (fileName.endsWith(".png")) {
                fileType = "image/png";
            } else if (fileName.endsWith(".ico")){
                fileType = "image/x-con";
            } else {
                MimetypesFileTypeMap copyType = new MimetypesFileTypeMap();
                fileType = copyType.getContentType(fileName);
            }
            type = fileType.split("/")[0];
            
            // Try to load the corresponding html file.
            try {
                input = new FileInputStream(fileName);
            } catch (Exception fileNotFound) {
            	// Change fileExists to false if corresponding file isn't found.
                fileExists = false;
            }
            // Decrement checkForFile.
            checkForFile--;
        }
        System.err.println("Request line: (" + line + ")");
        if (line.length() == 0)
          break;
      } catch (Exception e) {
        System.err.println("Request error: " + e);
        break;
      }
    }
    return;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   if(fileExists) {
   	// If the file exists, write the following to the output string.
        os.write("HTTP/1.1 200 OK\n".getBytes());
        os.write("Content-Type: ".getBytes());
        os.write(contentType.getBytes());
        os.write("\n\n".getBytes());
   } else { 
        // If the file doesn't exist, write 404 File Not Found to the output
        // string.
        os.write("HTTP/1.1 404 File Not Found\n".getBytes());
        os.write("Content-Type: ".getBytes());
        os.write(contentType.getBytes());
        os.write("\n\n".getBytes());
   }
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os) throws Exception
{
   if (fileExists) {
      // If the file exists load the corresponding html file.
      byte[] buffer = new byte[1024];
      int bytes = 0;
      
      // Create a new date object variable.
      Date date = new Date();
      // Create a new string variable for the file name.
      String file;         
      // Name the server.
      String server = "Elena's Server!";
      
      while ((bytes = input.read(buffer)) != -1) {
        // Load the images of the page
        if (type.equals("image")) {
        os.write(buffer, 0, bytes);
        } else{
            // Load the head of the page and favicon
            os.write("<html><head><title>Elena's Server!</title>".getBytes());
            os.write("<link rel=\"icon\" href=\"images/favicon.ico\" type=\"image/x-icon\" >".getBytes());
            os.write("</head>".getBytes());
        
        
        file = new String(buffer, 0, bytes);

        // Replace all <cs371date> tags with a date string.
        if (file.contains("<cs371date>")) {
          file = file.replace("<cs371date>", date.toString());
        }

        // Replace all <cs371server> tags with the name of the server.
        if (file.contains("<cs371server>")) {
          file = file.replace("<cs371server>", server);
        }
        
        // Load the corresponding html file.
        buffer = file.getBytes();
        os.write(buffer, 0, buffer.length);
        }
      }
    } else {
      // If the file isn't found load a 404 File Not Found response.
      os.write("<html><head></head><body>\n".getBytes());
      os.write("<h3>404: File not found.</h3>\n".getBytes());
      os.write("</body></html>\n".getBytes());
    }
  }

} // end class
