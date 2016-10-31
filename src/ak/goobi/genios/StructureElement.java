package ak.goobi.genios;

import java.util.List;

import betullam.goobi.oaihelper.classes.Id;

public class StructureElement {

	private Id id;
	private String structureElementType;
	private String title;
	private String subTitle;
	private List<String> authors;
	private String artAbstract;
	private String language;
	private String year;
	private String volumeNo;
	private String issueNo;
	private String pageLabel;
	private int counter;

	public StructureElement(Id id, String structureElementType, String title, String subTitle, List<String> authors, String artAbstract, String language, String year, String volumeNo, String issueNo, String pageLabel, int counter) {
		this.id = id;
		this.structureElementType = structureElementType;
		this.title = title;
		this.subTitle = subTitle;
		this.authors = authors;
		this.artAbstract = artAbstract;
		this.language = language;
		this.year = year;
		this.volumeNo = volumeNo;
		this.issueNo = issueNo;
		this.pageLabel = pageLabel;
		this.counter = counter;
	}


	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}
	public String getStructureElementType() {
		return structureElementType;
	}
	public void setStructureElementType(String structureElementType) {
		this.structureElementType = structureElementType;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getSubTitle() {
		return subTitle;
	}
	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}
	public List<String> getAuthors() {
		return authors;
	}
	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}
	public String getArtAbstract() {
		return artAbstract;
	}
	public void setArtAbstract(String artAbstract) {
		this.artAbstract = artAbstract;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public String getVolumeNo() {
		return volumeNo;
	}
	public void setVolumeNo(String volumeNo) {
		this.volumeNo = volumeNo;
	}
	public String getIssueNo() {
		return issueNo;
	}
	public void setIssueNo(String issueNo) {
		this.issueNo = issueNo;
	}
	public String getPageLabel() {
		return pageLabel;
	}
	public void setPageLabel(String pageLabel) {
		this.pageLabel = pageLabel;
	}
	public int getCounter() {
		return counter;
	}
	public void setCounter(int counter) {
		this.counter = counter;
	}

}
