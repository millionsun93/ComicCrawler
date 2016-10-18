import com.mongodb.BasicDBList;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class Chapter {
	private String title;
	private List<String> urls;
	
	public Chapter() {
		urls = new ArrayList<>();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		this.urls = urls;
	}

	public Document toDocument(){
		BasicDBList list = new BasicDBList();
		list.addAll(urls);
		return new Document().append("title", title)
				.append("urls", list);
	}
}
