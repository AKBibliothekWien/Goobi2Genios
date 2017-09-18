package ak.goobi.genios;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import main.java.ak.goobi.akconfig.AkConfig;

public class Main {

	public static Map<String, String> akConfig;
	static String processFolder;
	static String pathToDestination;
	static String processTitle;
	static String identifier;
	static String structureElements;
	static String prefix;
	static String processid;
	static boolean exportToGenios;
	static String ftpDirectory;
	static String pubDate;
	static String publication;

	
	public static void main(String[] args) throws Exception {
		
		// Get AK configs in goobi config folder:
		akConfig = new AkConfig().getAkConfig();

		if (args.length < 4) {
			System.err.println("\nError: You have to supply 4 arguments:\n\n" +
					"1. The path to the metadata folder of the goobi-process (e. g. \"/opt/digiverso/goobi/metadata/123/\". You can use {processpath} in a script-step in Goobi.\n" +
					"2. The path where you want to save the package for Genios locally (e. g. \"/home/userfolder\").\n" +
					"3. The title of the Goobi process (e. g. \"infoeuin_AC05712646_2014_003\"). You can use {processtitle} in a script-step in Goobi.\n" +
					"4. The ID of the process (e. g. \"1037\"). You can use {processid} in a script-step in Goobi.\n"
					);
		} else {
			
			processFolder = args[0];
			pathToDestination = args[1];
			processTitle = args[2];
			processid = args[3];
			
			//System.out.println("processFolder: " + processFolder);
			//System.out.println("pathToDestination: " + pathToDestination);
			//System.out.println("processTitle: " + processTitle);
			//System.out.println("processid: " + processid);

			exportToGenios = exportToGenios(processid);
			
			if (exportToGenios) {
				structureElements = (akConfig.get("Genios."+publication+".StructureElements") != null) ? akConfig.get("Genios."+publication+".StructureElements") : null;
				prefix = (akConfig.get("Genios."+publication+".Prefix") != null) ? akConfig.get("Genios."+publication+".Prefix") : prefix;
				ftpDirectory = (akConfig.get("Genios."+publication+".FtpPath") != null) ? akConfig.get("Genios."+publication+".FtpPath") : ftpDirectory;

				if (structureElements != null && prefix != null && ftpDirectory != null) {
					GeniosPackageMaker gpm = new GeniosPackageMaker(processFolder, pathToDestination, processTitle, structureElements, prefix, ftpDirectory, pubDate);
					boolean geniosPackageCreated = gpm.makeGeniosPackage();
					if (geniosPackageCreated == true) {
						System.out.println("Genios-Daten in " + pathToDestination + " erstellt und zum Genios-FTP-Server hochgeladen.");
						
						String eMailHost = akConfig.get("Genios.General.EmailHost");
						String eMailsTo = akConfig.get("Genios.General.EmailsTo");
						String eMailFrom = akConfig.get("Genios.General.EmailFrom");
						String eMailReplyTo = akConfig.get("Genios.General.EmailReplyTo");
						boolean sendMail = new SendMail().send(eMailHost, eMailsTo, eMailFrom, eMailReplyTo, "Neue Publikation der AK Wien", "Eine neue Publikation der AK Wien wurde auf dem FTP-Server bereitgestellt.\n\nOrdner: "+ ftpDirectory + "\nDateiname: " + gpm.getZipFileName() + "\n\nMit freundlichen Grüßen,\nAK Bibliothek Wien");
						if (sendMail) {
							System.out.println("Info-eMail an Genios erfolgreich gesendet.");
						} else {
							System.err.println("Fehler beim Senden der Info-eMail an Genios.");
						}
					} else {
						System.err.println("Fehler beim Erstellen der Genios-Daten!\n");
					}
				} else {
					System.err.println("Fehler beim Erstellen der Genios-Daten!\nPrüfen Sie, ob in der Datei [GOOBI]/config/goobi_ak.xml die Werte im Abschnitt <Genios><"+publication+"> ... </"+publication+"></Genios> korrekt gesetzt sind!");
				}
				
				
			} else {
				System.out.println("Genios-Daten wurden nicht exportiert.");
			}
		}
	}

	
	private static boolean exportToGenios(String processid) {
		// Check if data should be exported to genios.
		// We use a "Prozesseigenschaft" of the proccess for it.
		// Example SQL Query with process ID 1043: SELECT Titel, WERT FROM prozesseeigenschaften WHERE prozesseID=1043 AND (Titel="Genios" OR Titel="Genios Kürzel" OR Titel="Genios FTP-Ordner");
		boolean exportToGenios = false;

		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			exportToGenios = false;
			e.printStackTrace();
			return exportToGenios;
		}

		Connection jdbcConnection = null;
		
		try {
			String jdbc = Main.akConfig.get("General.Jdbc");
			String dbUser = Main.akConfig.get("General.DbUser");
			String dbPass = Main.akConfig.get("General.DbPass");

			// Disable SSL warning by adding "useSSL=false" as a parameter to the connection string
			jdbcConnection = DriverManager.getConnection("jdbc:"+jdbc+"?useSSL=false", dbUser, dbPass);
			
			Statement statement = jdbcConnection.createStatement();
			//String sql = "SELECT Titel, WERT FROM prozesseeigenschaften WHERE prozesseID=" + processid + " AND (Titel=\"Genios\" OR Titel=\"Genios Kürzel\" OR Titel=\"Genios FTP-Ordner\" OR Titel=\"Genios Veröff.-Datum\")";
			String sql = "SELECT Titel, WERT FROM prozesseeigenschaften WHERE prozesseID=" + processid + " AND (Titel=\"Genios\" OR Titel=\"Genios Publikation\" OR Titel=\"Genios Veröff.-Datum\")";
			
			ResultSet resultSet = statement.executeQuery(sql);

			while(resultSet.next()){
				//Retrieve by column name
				String title = resultSet.getString("Titel");
				String value = resultSet.getString("WERT");

				if (title.equals("Genios") && value.equals("Ja")) {
					exportToGenios = true;
				} else if (title.equals("Genios") && value.equals("Nein")) {
					exportToGenios = false;
				}

				/*
				if (title.equals("Genios Kürzel")) {
					prefix = value;
				}
				if (title.equals("Genios FTP-Ordner")) {
					ftpDirectory = value;
				}
				*/
				
				if (title.equals("Genios Veröff.-Datum")) {
					pubDate = value;
				}
				if (title.equals("Genios Publikation")) {
					publication = value;
				}
			}

		} catch (SQLException ex) {
			// Error:
			exportToGenios = false;
			ex.printStackTrace();
			System.out.println("\n");
			System.err.println("SQLException: " + ex.getMessage());
			System.err.println("SQLState: " + ex.getSQLState());
			System.err.println("ErrorCode: " + ex.getErrorCode());
		}

		if (exportToGenios) {
			/*
			if (prefix.equals("") || prefix == null) {
				System.err.println("Es muss ein Genios-Kürzel angegeben werden!");
				exportToGenios = false;
			}

			if (ftpDirectory.equals("") || ftpDirectory == null) {
				System.err.println("Ein Ordner, in den die Daten auf dem Genios-Server gespeichert werden sollen, muss angegeben werden!");
				exportToGenios = false;
			}
			*/
			if (pubDate.equals("") || pubDate == null) {
				System.err.println("In den Prozesseigenschften muss ein Veröffentlichungsdatum im Format dd.mm.yyyy angegeben werden!");
				exportToGenios = false;
			}
			if (publication.equals("") || publication == null) {
				System.err.println("In den Prozesseigenschften muss eine Publikation (Kürzel) ausgewählt werden! Dieses muss gleich lauten wie der Konfigurationsabschnitt für die Publikation in der Konfigurationsdatei \"goobi_ak.xml\" (z. B. \"WuG\" oder \"MWuG\").");
				exportToGenios = false;
			}
		}

		return exportToGenios;
	}

	
}
