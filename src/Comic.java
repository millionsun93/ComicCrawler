import com.mongodb.BasicDBObject;
import org.bson.Document;

import java.util.Date;
import java.util.List;

public class Comic {
	private String title;
	private String source;
	private String status;
	private String url;
	private String description;
	private String viewers;
	private Date updateTime;;
	private String thumbnail;
	private List<String> authors;
	private List<String> categories;
	private List<Chapter> chapter;
		
	public Comic() {
		super();
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}
	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
	public void setChapter(List<Chapter> chapter) {
		this.chapter = chapter;
	}
	
	public void setViewers(String viewers) {
		this.viewers = viewers;
	}
	public Document toDocument(){
		BasicDBObject dbAuthors = new BasicDBObject();
		dbAuthors.append("authors", authors);
		BasicDBObject dbCategory = new BasicDBObject();
		dbCategory.append("categories", categories);
		return new Document().append("title", title)
				.append("source", source)				
				.append("status", status)
				.append("viewers", viewers)
				.append("url", url)
				.append("thumbnail", thumbnail)
				.append("description", description)
				.append("updateTime", updateTime)
				.append("authors", (this.authors))
				.append("categories",(this.categories));
	}
}
