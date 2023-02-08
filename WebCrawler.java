import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class Request {
    public URL url;

    public Request(String url) {
        try {
            this.url = new URL(url);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public Request(URL url) {
        this.url = url;
    }

    public String getContent() {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if (con.getContentType() != null && !con.getContentType().startsWith("text/html")) {
                return null;
            }
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            return content.toString();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }

    }

    public Page getPage() {
        String content = this.getContent();
        if (content != null)
            return new Page(this.url, getContent());
        else
            return null;
    }
}

class Page {
    public URL url;
    public String html;
    public Document doc;

    public Page(URL url, String htmlContent) {
        this.url = url;
        html = htmlContent;
        doc = getDoc();
    }

    public ArrayList<URL> getLinks() {
        Elements links = doc.select("a[href]");
        ArrayList<URL> urls = new ArrayList<>();
        for (Element link : links) {
            try {
                urls.add(new URL(url, link.attr("href")));
            } catch (Exception e) {
                System.out.println("Invalid url in a href: " + e);
            }
        }
        return urls;
    }

    public String getTextContent() {
        String textContent = "";
        Elements elements = doc.select("p,h1,h2,h3,h4,h5,h6");
        for (Element e : elements) {
            textContent = e.text() + "\n";
        }
        return textContent;
    }

    private Document getDoc() {
        if (html == null)
            return null;

        Document doc = Jsoup.parse(html);
        return doc;
    }
}

public class WebCrawler {
    public static void main(String[] args) {
        try {
            Document doc = Jsoup.parse("<p>a<span> dog</span>mogo</p><p>sog</p>");
            System.out.println(doc.text());
            Elements e = doc.select("p, span");
            System.out.println(e.text());

            HashMap<String, Boolean> alreadyCrawled = new HashMap<>();
            ArrayList<String> linksCrawled = new ArrayList<>();
            ArrayList<String> linksToCrawl = new ArrayList<>();

            File linksToCrawlFile = null;
            Path linksToCrawlPath = null;
            Path pagesDir = null;

            if (args.length >= 3) {
                linksToCrawlPath = Paths.get(args[1]);
                linksToCrawlFile = new File(args[1]);
                pagesDir = Paths.get(args[2]);

                Scanner reader = new Scanner(linksToCrawlFile);
                while (reader.hasNextLine()) {
                    String line = reader.nextLine().trim();
                    if (line.length() > 0) {
                        // URL url = new URL(line);
                        linksToCrawl.add(line);
                        alreadyCrawled.put(line, true);
                    }
                }
                reader.close();

                // toCrawlWriter = new FileWriter(linksToCrawlFile);
                // toCrawlWriterAppend = new FileWriter(linksToCrawlFile, true);
                // toCrawlReader = new FileReader(linksToCrawlFile);
                // crawledWriter = new FileWriter(linksCrawledFile, true);

            } else {
                System.out.println(
                        "Please provide arguments: crawled pages, pages to crawl, directory to store crawled pages.");
                return;
            }

            while (linksToCrawl.size() > 0) {
                // Crawl
                String link = linksToCrawl.remove(0);
                alreadyCrawled.put(link, true);
                linksCrawled.add(link);

                List<String> lines = Files.readAllLines(linksToCrawlPath);
                if (lines.size() > 0) {
                    lines.remove(0);
                }
                Files.write(linksToCrawlPath, (String.join("\n", lines)).getBytes());

                Request req = new Request(link);
                Page page = req.getPage();

                if (page != null) {
                    System.out.println(link);

                    // System.out
                    // .println(pagesDir.resolve(Base64.getEncoder().encodeToString(link.toString().getBytes())));

                    System.out.println(page.getTextContent());
                    // Files.write(pagesDir.resolve(Base64.getEncoder().encodeToString(link.toString().getBytes())),
                    // page.getTextContent().getBytes());

                    Files.write(Paths.get(args[0]), (link.toString() + "\n").getBytes(), StandardOpenOption.APPEND);

                    for (URL linkToAdd : page.getLinks()) {
                        if (alreadyCrawled.get(linkToAdd.toString()) == null) {
                            linksToCrawl.add(linkToAdd.toString());
                            alreadyCrawled.put(linkToAdd.toString(), true);

                            Files.write(Paths.get(args[1]), (linkToAdd.toString() + "\n").getBytes(),
                                    StandardOpenOption.APPEND);
                        }
                    }
                }
            }
        } catch (

        Exception e) {
            e.printStackTrace();
            return;
        }

    }
}