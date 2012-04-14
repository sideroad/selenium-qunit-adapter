package jp.secret.sideroad;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.util.Date;

public class FileServer {
	int port;
	String wwwhome;

	Socket con;
	BufferedReader in;
	OutputStream out;
	PrintStream pout;
	Thread t;
	Boolean isRunning;

	public class FileServerRunnable implements Runnable {
		@Override
		public void run() {

			ServerSocket ss = null;
			try {
				ss = new ServerSocket(port);
			} catch (IOException e) {
				System.err.println("Could not start server: " + e);
				System.exit(-1);
			}
			System.out.println("FileServer accepting connections on port "
					+ port);
			while (isRunning) {
				try {
					con = ss.accept();
					in = new BufferedReader(new InputStreamReader(
							con.getInputStream()));
					out = new BufferedOutputStream(con.getOutputStream());
					pout = new PrintStream(out);

					String request = in.readLine();
					con.shutdownInput(); // ignore the rest
					log(con, request);

					processRequest(request);

					pout.flush();
				} catch (IOException e) {
					System.err.println(e);
				}
				try {
					if (con != null) {
						con.close();
					}
				} catch (IOException e) {
					System.err.println(e);
				}
			}
		}
	}

	public FileServer(int port, String wwwhome) {
		this.port = port;
		this.wwwhome = wwwhome;
	}

	public void start() {
		isRunning = true;
		t = new Thread(new FileServerRunnable());
		t.start();
	}

	public void stop() {
		try {
			isRunning = false;
			con.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void processRequest(String request) throws IOException {
		if (!request.startsWith("GET")
				|| request.length() < 14
				|| !(request.endsWith("HTTP/1.0") || request
						.endsWith("HTTP/1.1")) || request.charAt(4) != '/') {
			errorReport(pout, con, "400", "Bad Request",
					"Your browser sent a request that "
							+ "this server could not understand.");
		} else {
			String req = request.substring(4, request.length() - 9).trim();
			if (req.indexOf("/.") != -1 || req.endsWith("~")) {
				errorReport(pout, con, "403", "Forbidden",
						"You don't have permission to access "
								+ "the requested URL.");
			} else {
				String path = wwwhome + "/" + req;
				File f = new File(path);
				if (f.isDirectory() && !path.endsWith("/")) {
					pout.print("HTTP/1.0 301 Moved Permanently\r\n"
							+ "Location: http://"
							+ con.getLocalAddress().getHostAddress() + ":"
							+ con.getLocalPort() + req + "/\r\n\r\n");
					log(con, "301 Moved Permanently");
				} else {
					if (f.isDirectory()) {
						path = path + "index.html";
						f = new File(path);
					}
					try {
						InputStream file = new FileInputStream(f);
						String contenttype = URLConnection
								.guessContentTypeFromName(path);
						pout.print("HTTP/1.0 200 OK\r\n");
						if (contenttype != null) {
							pout.print("Content-Type: " + contenttype + "\r\n");
						}
						pout.print("Date: " + new Date() + "\r\n"
								+ "Server: IXWT FileServer 1.0\r\n\r\n");
						sendFile(file, out); // send raw file
						log(con, "200 OK");
					} catch (FileNotFoundException e) {
						errorReport(pout, con, "404", "Not Found",
								"The requested URL was not found "
										+ "on this server.");
					}
				}
			}
		}
	}

	void log(Socket con, String msg) {
//		System.err.println(new Date() + " ["
//				+ con.getInetAddress().getHostAddress() + ":" + con.getPort()
//				+ "] " + msg);
	}

	void errorReport(PrintStream pout, Socket con, String code, String title,
			String msg) {
		pout.print("HTTP/1.0 " + code + " " + title + "\r\n" + "\r\n"
				+ "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n"
				+ "<HTML><HEAD><TITLE>" + code + " " + title + "</TITLE>\r\n"
				+ "</HEAD><BODY>\r\n" + "<H1>" + title + "</H1>\r\n" + msg
				+ "<P>\r\n" + "<HR><ADDRESS>IXWT FileServer 1.0 at "
				+ con.getLocalAddress().getHostName() + " Port "
				+ con.getLocalPort() + "</ADDRESS>\r\n" + "</BODY></HTML>\r\n");
		log(con, code + " " + title);
	}

	void sendFile(InputStream file, OutputStream out) throws IOException {
		byte[] buffer = new byte[1000];
		while (file.available() > 0) {
			out.write(buffer, 0, file.read(buffer));
		}
	}
}
