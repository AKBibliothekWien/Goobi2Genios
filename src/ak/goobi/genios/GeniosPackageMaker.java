package ak.goobi.genios;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import betullam.goobi.oaihelper.classes.Id;
import betullam.goobi.oaihelper.main.GoobiOaiHelper;
import betullam.goobi.oaihelper.network.Network;
import betullam.xmlhelper.XmlParser;



public class GeniosPackageMaker {

	private GoobiOaiHelper goh;
	private XmlParser xmlParser;
	private Document document;
	private String pathCreatedFiles;
	private String pathZipFile;

	private String pathToProcessFolder; // e. g.: /opt/digiverso/goobi/metadata/123
	private String pathToPdfFolder; // Folder of the singel-paged PDFs (00000001.pdf, 00000002.pdf, etc.)
	private String pathToSourceFolder; // Folder of original whole PDF file of journal issue
	private String processTitle;
	private String prefix;
	private String identifier;
	private String ftpDirectory;
	private String pubDate;
	private String zipFileName; // Use this in the info-eMail sent to Genios (see Main.java)




	List<StructureElement> structureElements = new ArrayList<StructureElement>();;

	public GeniosPackageMaker(String processFolder, String pathToDestination, String processTitle, String structureTypes, String prefix, String ftpDirectory, String pubDate) throws Exception {
		List<String> typesToParse = new ArrayList<String>();
		String[] arrStructureTypes = structureTypes.split(",");

		for (String st : arrStructureTypes) {
			typesToParse.add(st.trim());
		}


		this.goh = new GoobiOaiHelper("http://emedien.arbeiterkammer.at/viewer/oai/");
		this.xmlParser = new XmlParser();
		//this.pathToProcessFolder = processFolder + File.separator; // Must be before getIdentifier()!!!
		this.pathToProcessFolder = stripFileSeperatorFromPath(processFolder); // Must be before getIdentifier()!!!
		this.processTitle = processTitle;
		this.identifier = getIdentifier();
		this.document = new Network().getMetsXmlRecord(goh.getOaiPmh(), this.identifier);
		this.pubDate = pubDate; // Must be before getStructureElements()!!!
		this.structureElements = getStructureElements(typesToParse);
		this.pathToPdfFolder = this.pathToProcessFolder + File.separator + "ocr" + File.separator + this.processTitle + "_pdf" + File.separator;
		this.pathToSourceFolder = this.getViewerSourceFolder(pathToProcessFolder);
		this.pathCreatedFiles = pathToDestination + File.separator + this.processTitle + File.separator;
		this.pathZipFile = pathToDestination + File.separator;
		this.prefix = prefix;
		this.ftpDirectory = ftpDirectory;
	}

	public boolean makeGeniosPackage() {

		boolean packageCreated = false;

		try {
			// Get date issued:						
			Date dtPubDate = null;
			try {
				dtPubDate = new SimpleDateFormat("dd.MM.yyyy").parse(this.pubDate);
			} catch (ParseException e) {
				// If there is no date to parse, print error-message and stop executing!
				System.err.print("Publication date (Erscheinungsdatum-/jahr) is not parseable: " + this.pubDate + "\nFor Genios, you have to use format dd.mm.yyyy!\n");
				packageCreated = false;
				return packageCreated;
			}
			String strPubDate = new SimpleDateFormat("yyyyMMdd").format(dtPubDate);

			// Create directory for the single article PDF files and the metadata file for this "Vorgang" if it does not exist:
			File destPath = new File(this.pathCreatedFiles);
			if (destPath.exists() == false && destPath.isDirectory() == false) {
				destPath.mkdirs();
			}

			String mdTempFileName = "mdTempFileName";
			String mdFileName = "mdFileName";
			String wholePdfName = "wholePdfName";
			File tempMdFile = new File(mdTempFileName);
			//FileWriter mdFileWriter = new FileWriter(this.pathCreatedFiles+tempMdFile, false);
			//writer.write("\ufeff"); // Write BOM to beginning of file - is needed in RePEc
			
			FileOutputStream fileStream = new FileOutputStream(new File(this.pathCreatedFiles+tempMdFile));
			OutputStreamWriter mdFileWriter = new OutputStreamWriter(fileStream, "UTF-8");
			 
			

			// Write headings and separate them with TAB:
			mdFileWriter.write("Seite");
			mdFileWriter.write("\t");
			mdFileWriter.write("Autor");
			mdFileWriter.write("\t");
			mdFileWriter.write("Titel");
			mdFileWriter.write("\t");
			mdFileWriter.write("Abstract");
			mdFileWriter.write("\t");
			mdFileWriter.write("pdf");
			mdFileWriter.write(System.getProperty("line.separator"));

			for (StructureElement structureElement : structureElements) {
				String title = structureElement.getTitle();
				String abstr = structureElement.getArtAbstract();
				abstr = (abstr != null) ? abstr.replaceAll(System.getProperty("line.separator"), " ") : ""; // Remove all possible line breaks!
				String year = structureElement.getYear();
				String volume = structureElement.getVolumeNo();
				String issue = structureElement.getIssueNo();
				String pages = (structureElement.getPageLabel() != "-") ? structureElement.getPageLabel() : "";

				int counter = structureElement.getCounter();
				String strCounter = String.format("%03d", counter);
				List<String> lstAuthors = structureElement.getAuthors();
				String authors = (lstAuthors != null) ? StringUtils.join(lstAuthors, ";").trim() : "";
				List<String> imageNos = goh.getOrderNoByPhysId(document, structureElement.getId().getPhysIds());

				
				// Treat issue name for filenames:
				issue = issue.replaceAll(" ", "_"); // Replace whitespaces chars with "_" (underscore)
				issue = issue.replaceAll("[^A-Za-z0-9_]", ""); // Replace everything that is not alphanumeric or "_" with "" (nothing)
				
				// Metadata filename:
				List<String> mdAndZipFile = new ArrayList<String>();
				mdAndZipFile.add(strPubDate);
				mdAndZipFile.add(year);				
				mdAndZipFile.add(issue);
				mdFileName = this.prefix + StringUtils.join(mdAndZipFile, "_") + ".txt";
				this.zipFileName = this.prefix + StringUtils.join(mdAndZipFile, "_") + ".zip";
						
				// PDF single article filename:
				List<String> pdfSingleFile = new ArrayList<String>();
				if (!year.isEmpty()) {pdfSingleFile.add(year);}
				if (!volume.isEmpty()) {pdfSingleFile.add(volume);}
				if (!issue.isEmpty()) {pdfSingleFile.add(issue);}
				if (!pages.isEmpty()) {pdfSingleFile.add(pages);}
				if (!strCounter.isEmpty()) {pdfSingleFile.add(strCounter);}
				String pdfSingleArticleFilename = this.prefix + StringUtils.join(pdfSingleFile, "_"); //+ ".pdf"
				
				// Whole PDF filename:
				List<String> pdfWholeFile = new ArrayList<String>();
				pdfWholeFile.add(year);
				pdfWholeFile.add(volume);
				pdfWholeFile.add(issue);
				wholePdfName = this.prefix + StringUtils.join(pdfWholeFile, "_") + ".pdf";

				
				// Write one line per article (seperate data with TAB):
				if (title != null) {
					mdFileWriter.write(pages);
					mdFileWriter.write("\t");
					mdFileWriter.write(authors);
					mdFileWriter.write("\t");
					mdFileWriter.write(title);
					mdFileWriter.write("\t");
					mdFileWriter.write(abstr);
					mdFileWriter.write("\t");
					mdFileWriter.write(pdfSingleArticleFilename+".pdf");
					mdFileWriter.write(System.getProperty("line.separator")); // New line
				}

				new betullam.goobi.pdfmaker.PdfByOrderNos(this.pathToPdfFolder, imageNos, pathCreatedFiles, pdfSingleArticleFilename);

			}
			mdFileWriter.flush();
			mdFileWriter.close();
			fileStream.flush();
			fileStream.close();

			// Rename (= move) metadata-file to real filename (= prefix_year_volumeNo_issueNo.txt):
			Path tempMdPath = Paths.get(this.pathCreatedFiles+tempMdFile);
			Path mdPath = Paths.get(this.pathCreatedFiles+mdFileName);
			Files.move(tempMdPath, mdPath);
			
			// Get and copy whole .pdf file so that we can put it into the .zip file:
			Path wholePdfFileSource = Paths.get(this.getWholePdf(this.pathToSourceFolder));
			Path wholePdfFileTarget = Paths.get(this.pathCreatedFiles+wholePdfName);
			Files.copy(wholePdfFileSource, wholePdfFileTarget);

			// Create .zip-file:
			String zipFile = this.pathZipFile+this.zipFileName;
			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);
			byte[] buffer = new byte[1024]; // byte buffer		
			File[] filesForZip = destPath.listFiles();
			for (int i = 0; i < filesForZip.length; i++) {
				FileInputStream fis = new FileInputStream(filesForZip[i]);
				zos.putNextEntry(new ZipEntry(filesForZip[i].getName()));
				int length;
				while ((length = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}
				zos.closeEntry();
				fis.close(); // Close InputStream
			}
			zos.flush();
			zos.close(); // Close ZipOutputStream

			// Delete created files and folder. We don't need them because they are in the .zip-file now
			for (int i = 0; i < filesForZip.length; i++) { // First delete all created files
				if (filesForZip.length > 0) {
					filesForZip[i].delete();
				}
			}
			filesForZip = destPath.listFiles(); // Check again if the folder contains any files!
			if (filesForZip.length == 0) { // If not, delete the folder itself
				destPath.delete();
			}

			// Upload to FTP-Server:
			String ftpServer = Main.akConfig.get("Genios.General.FtpServer");
			String ftpPort = Main.akConfig.get("Genios.General.FtpPort");
			String ftpUser = Main.akConfig.get("Genios.General.FtpUser");
			String ftpPass = Main.akConfig.get("Genios.General.FtpPass");
			boolean ftpOK = FtpUpload.uploadFile(zipFile, ftpDirectory, this.zipFileName, ftpServer, Integer.valueOf(ftpPort), ftpUser, ftpPass, true, false);

			if (ftpOK == true) {
				packageCreated = true;
			} else {
				System.err.println("Error with FTP-Upload: File " + this.zipFileName + " not written on FTP-Server.");
				packageCreated = false;
			}

		} catch (Exception e) {
			System.err.println("Error: " + e.getStackTrace());
			packageCreated = false;
		}

		return packageCreated;
	}


	/**
	 * Gets information of structure elements (e. g. "Article", "Chapter", etc.) from an OAI-PMH interface. The document must be a METS-XML. An identifier of an individual record
	 * that is available over the OAI interface must be submitted. Example: http://example.com/oai/?verb=GetRecord&metadataPrefix=oai_dc&identifier=USE_THIS_ID
	 * Returns a list with "StructureElement" objects. You could iterate over the list to get title, subtitle, authors, etc. of the structure element.
	 * If an information is not found, you will get "null".
	 * 
	 * @param id					a String of the identifier of an individual record that is available over the OAI interface
	 * @param structureElements		a List<String> of stucture elements to parse, e. g. "Article", "Chapter", etc. Use "null" to parse all structure elements
	 * @return						a List<StructureElement>
	 * @throws Exception
	 */
	public List<StructureElement> getStructureElements(List<String> structureElements) throws Exception {

		List<StructureElement> lstStructureElements = new ArrayList<StructureElement>();
		List<Id> ids = goh.getIds(document, structureElements);

		for(Id metsIds : ids) {

			String logId = metsIds.getLogId();
			String dmdlogId = metsIds.getDmdlogId();

			String structureElementType = xmlParser.getAttributeValue(document, "OAI-PMH/GetRecord/record/metadata/mets/structMap[@TYPE=\"LOGICAL\"]//div[@ID='" + logId + "']", "TYPE");
			String title = xmlParser.getTextValue(document, "OAI-PMH/GetRecord/record/metadata/mets/dmdSec[@ID='" + dmdlogId + "']/mdWrap/xmlData/mods/titleInfo/title");
			String subTitle = xmlParser.getTextValue(document, "OAI-PMH/GetRecord/record/metadata/mets/dmdSec[@ID='" + dmdlogId + "']/mdWrap/xmlData/mods/titleInfo/subTitle");
			List<String> authors = xmlParser.getTextValues(document, "OAI-PMH/GetRecord/record/metadata/mets/dmdSec[@ID='" + dmdlogId + "']/mdWrap/xmlData/mods/name/displayForm");
			String artAbstract = xmlParser.getTextValue(document, "OAI-PMH/GetRecord/record/metadata/mets/dmdSec[@ID='" + dmdlogId + "']/mdWrap/xmlData/mods/abstract");
			String language = xmlParser.getTextValue(document, "OAI-PMH/GetRecord/record/metadata/mets/dmdSec[@ID='" + dmdlogId + "']/mdWrap/xmlData/mods/language/languageTerm");
			String yearIssued = null;
			String volumeNo = xmlParser.getTextValue(document, "OAI-PMH/GetRecord/record/metadata/mets//dmdSec/mdWrap/xmlData/mods/part/detail[@type='volume']/number");
			String issueNo = xmlParser.getTextValue(document, "OAI-PMH/GetRecord/record/metadata/mets//dmdSec/mdWrap/xmlData/mods/part/detail[@type='issue']/number");
			List<String> pageLabels = goh.getFirstLastLabelByPhysId(document, metsIds.getPhysIds());
			String firstPageOrig = pageLabels.get(0);
			String lastPageOrig = pageLabels.get(1);
			String pageLabel = "";
			String firstPage = null;
			String lastPage = null;

			// Get yearIssued from dateIssued:
			Date dtPubDate = null;
			try {
				dtPubDate = new SimpleDateFormat("dd.MM.yyyy").parse(this.pubDate);
			} catch (ParseException e) {
				// If there is no date to parse, print error-message and stop executing!
				System.err.print("Publication date (Erscheinungsdatum) is not parseable: " + this.pubDate + "\nFor Genios, you have to use format dd.mm.yyyy!\n");
				return null;
			}
			yearIssued = new SimpleDateFormat("yyyy").format(dtPubDate);

			if(firstPageOrig.contains("[") || firstPageOrig.contains("-")) {
				firstPage = null;
			} else {
				firstPage = firstPageOrig;
			}

			if(lastPageOrig.contains("[") || lastPageOrig.contains("-")) {
				lastPage = null;
			} else {
				lastPage = lastPageOrig;
			}

			if (firstPage != null && lastPage != null) {
				if (firstPage.equals(lastPage)) {
					pageLabel = firstPage;
				} else {
					pageLabel = firstPage + "-" + lastPage;
				}
			} else if (firstPage != null && lastPage == null) {
				pageLabel = firstPage;
			} else if (firstPage == null && lastPage != null) {
				pageLabel = lastPage;
			} else {
				pageLabel = "";
			}

			/*
			System.out.println("Ids: " + metsIds.toString());
			System.out.println("Type: " + structureElementType);
			System.out.println("Authors: " + authors);
			System.out.println("Title: " + title);
			System.out.println("Subtitle: " + subTitle);
			System.out.println("Abstract: " + artAbstract);
			System.out.println("Language: " + language);
			System.out.println("Pages: " + pageLabel);
			System.out.println("Date issued: " + dateIssued);
			System.out.print("\n");
			*/

			lstStructureElements.add(new StructureElement(metsIds, structureElementType, title, subTitle, authors, artAbstract, language, yearIssued, volumeNo, issueNo, pageLabel, ids.indexOf(metsIds)));
		}

		return lstStructureElements;
	}


	public String getIdentifier() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {

		File metaxmlFile = new File(this.pathToProcessFolder + File.separator + "meta.xml");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document metaxmlDoc = db.parse(metaxmlFile);
		String identifier = xmlParser.getTextValue(metaxmlDoc, "/mets/dmdSec/mdWrap/xmlData/mods/extension/goobi/metadata[@name='_urn']");
		return identifier;
	}

	private String getViewerSourceFolder(String pathToProcessFolder) {
		String sourceFolder = null;

		File imgSourcePath = new File(pathToProcessFolder + File.separator + "images");
		File[] listOfFilesAndDirs = imgSourcePath.listFiles();

		for (int i = 0; i < listOfFilesAndDirs.length; i++) {

			if (listOfFilesAndDirs[i].isDirectory()) {

				Path pdfSourcePath = FileSystems.getDefault().getPath(listOfFilesAndDirs[i].getName());
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*_source"); // Get Directory that ends with _source
				if (matcher.matches(pdfSourcePath)) {
					sourceFolder = listOfFilesAndDirs[i].getAbsolutePath();
				}
			}
		}
		return sourceFolder;
	}

	private String getWholePdf(String pathToSourceFolder) {
		String wholePdf = "";

		List<String> pdfFiles = new ArrayList<String>();
		File sourceFolder = new File(pathToSourceFolder);
		for (File file : sourceFolder.listFiles()) {
			if (file.getName().endsWith((".pdf"))) {
				pdfFiles.add(file.getName());
			}
		}

		wholePdf = pathToSourceFolder + File.separator + pdfFiles.get(0);
		return wholePdf;
	}

	private String stripFileSeperatorFromPath (String path) {
		if ((path.length() > 0) && (path.charAt(path.length()-1) == File.separatorChar)) {
			path = path.substring(0, path.length()-1);
		}
		return path;
	}
	
	public String getZipFileName() {
		return this.zipFileName;
	}


}
