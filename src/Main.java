import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static DateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
    private static int MAX_PAGE = 5;

    public static void main(String[] args) throws IOException {
        //docker ip
        mongoClient = new MongoClient("172.17.0.2", 27017);
        db = mongoClient.getDatabase("comics");
        System.out.println("Connect to database successfully");
        for (int i = 1; i < MAX_PAGE + 1; i++) {
            parseWebsite("http://vietcomic.net/danh_sach_truyen?type=new&category=all&alpha=all&state=all&group=all&page=" + i);
        }
        mongoClient.close();
    }

    private static void parseWebsite(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select(".list-truyen-item-wrap > h3 > a");
        links.forEach(element -> {
            try {
                parseComic(element.attr("href"), element.text().trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void parseComic(String url, String title) throws IOException {
        Document doc = Jsoup.connect(url).get();
        org.bson.Document insertedComic = checkComicExist(title);
        String updateTime = doc.select(".manga-info-text > li:contains(Ngày cập nhât)")
                .text().substring("Ngày cập nhât :".length()).trim();
        if (insertedComic == null) {
            Comic comic = new Comic();
            comic.setTitle(title);
            comic.setUrl(url);
            String[] authorsArr = doc.select(".manga-info-text > li:contains(Tác Giả)")
                    .text().substring("Tác Giả :".length()).trim().split(",");
            Arrays.stream(authorsArr).map(String::trim).toArray(old -> authorsArr);
            List<String> authors = Arrays.asList(authorsArr);
            comic.setAuthors(authors);
            String status = doc.select(".manga-info-text > li:contains(Tình Trạng)")
                    .text().substring("Tình Trạng :".length()).trim();
            comic.setStatus(status);
            String source = doc.select(".manga-info-text > li:contains(Nguồn)")
                    .text().substring("Nguồn :".length()).trim();
            comic.setSource(source);
            String viewers = doc.select(".manga-info-text > li:contains(Lượt Xem)")
                    .text().replaceAll("[^-?0-9]+", "");
            comic.setViewers(viewers);
            String[] categoriesArr = doc.select(".manga-info-text > li:contains(Thể Loại)")
                    .text().substring("Thể Loại : ".length()).trim().split(",");
            Arrays.stream(categoriesArr).map(String::trim).toArray(old -> categoriesArr);
            List<String> categories = Arrays.asList(categoriesArr);
            comic.setCategories(categories);
            doc.select(".manga-info-content span.noidung").remove();
            String description = doc.select(".manga-info-content")
                    .text().trim();
            comic.setDescription(description);
            try {
                comic.setUpdateTime(format.parse(updateTime));
            } catch (ParseException e) {
                comic.setUpdateTime(new Date());
                e.printStackTrace();
            }
            String thumbnail = doc.select(".manga-info-pic>img").attr("src");
            comic.setThumbnail(thumbnail);
            try {
                db.getCollection("comics").insertOne(comic.toDocument());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(title + " was inserted");
        } else {
            try {
                if (insertedComic.get("updateTime", Date.class).compareTo(format.parse(updateTime)) < 0) {
                    db.getCollection("comics").updateOne(new org.bson.Document("title", title)
                            , new org.bson.Document("$set", new org.bson.Document("updateTime", format.parse(updateTime))));
                }
            } catch (Exception e) {
                //ignore
            }

        }
        Elements chapters = doc.select(".chapter-list span:first-child>a");
        Collections.reverse(chapters);
        chapters.forEach(item -> {
            if (!checkChapterExist(item.text(), title)) {
                try {
                    parseChapter(item.attr("href"), item.text(), title);
                } catch (IOException e) {
                    System.out.println("error at " + item.text() + " , " + title);
                    e.printStackTrace();
                }
            }
        });
    }

    private static org.bson.Document checkComicExist(String title) {
        return db.getCollection("comics").find(Filters.eq("title", title)).first();
    }

    private static boolean checkChapterExist(String chapterTitle, String title) {
        return db.getCollection("comics").find(Filters.and(Filters.eq("title", title), Filters.eq("chapters.title", chapterTitle))).first() != null;
    }

    private static void parseChapter(String url, String chapterTitle, String comicTitle) throws IOException {
        Document document = Jsoup.connect(url).get();
        Element element = document.select("body > script:nth-child(11)").first();
        Pattern pattern = Pattern.compile("'(.*?)'");
        Matcher matcher = pattern.matcher(element.toString());
        Chapter chapter = new Chapter();
        chapter.setTitle(chapterTitle);
        if (matcher.find()) {
            String chaptersAsString = matcher.group(1);
            chapter.setUrls(Arrays.asList(chaptersAsString.split("\\|")));
            try {
                db.getCollection("comics").updateOne(new org.bson.Document("title", comicTitle),
                        new org.bson.Document("$push", new org.bson.Document("chapters", chapter.toDocument())));
            } catch (Exception e) {
                throw new IOException("error when insert " + chapterTitle + ", " + chapter);
            }
        } else {
            throw new NullPointerException("img not found");
        }
    }
}
