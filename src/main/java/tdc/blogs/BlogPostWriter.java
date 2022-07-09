package tdc.blogs;

import joshlong.client.BlogPost;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
class BlogPostWriter {

	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private final String template = """
			---
			date: %s
			description: %s
			featured: false
			patterns:
			- Deployment
			tags:
			- Spring
			- Kubernetes
			- DevOps
			- Microservices
			- Integration
			- Data
			- Batch
			- Cloud
			team:
			- Josh Long
			title: '%s'
			---

			%s

			""";

	@SneakyThrows
	public List<File> writeAllBlogs(File blogContent, Collection<BlogPost> blogs) {
		var listOfFiles = new ArrayList<File>();
		for (var blog : blogs) {
			var fileName = buildBlogPostFileName(blog);
			var blogFile = new File(blogContent, fileName + ".md");
			var url = blog.url();
			if (log.isDebugEnabled()) {
				log.debug("------------------------------------------");
				log.debug("the final title is [" + fileName + "]");
				log.debug("the blog post Markdown file will live at " + blogFile.getAbsolutePath());
				log.debug("the blog post url is " + url);
			}
			var written = this.writeBlog(blog, blogFile);
			if (written) {
				listOfFiles.add(blogFile);
			}
		}
		return listOfFiles;
	}

	@SneakyThrows
	private String readBlogPostContentFrom(URL url) {
		var doc = Jsoup.parse(url, 5000);
		var body = doc.getElementsByClass("blog--post");
		var bodyHtml = body.html();
		if (log.isDebugEnabled()) {
			log.debug("trying to open the url " + url.toString());
			log.debug("the body is " + bodyHtml);
		}
		return bodyHtml;
	}

	@SneakyThrows
	private String buildContentsForBlogPost(BlogPost post) {
		var description = post.description();
		var title = this.cleanTitleForMetadata(post.title());
		var content = readBlogPostContentFrom(post.url());
		var templateDate = this.simpleDateFormat.format(post.published());
		return String.format(this.template, templateDate, description, title, content);
	}

	private String cleanTitleForMetadata(String title) {
		var nb = new StringBuilder();
		for (var c : title.toCharArray()) {
			if (c == '\'')
				nb.append("");
			else
				nb.append(c);
		}
		return nb.toString();
	}

	private String buildBlogPostFileName(BlogPost post) {
		var title = post.title();
		var date = post.published();
		Assert.notNull(date, () -> "the date can't be null");
		Assert.notNull(title, () -> "the title can't be null");
		var dateString = this.simpleDateFormat.format(date);
		var cleanTitle = this.cleanTitleForFileName(title).toLowerCase(Locale.ROOT);
		return dateString + '-' + cleanTitle;
	}

	private String cleanTitleForFileName(String title) {
		var nb = new StringBuffer();
		for (var c : title.toCharArray()) {
			if (Character.isDigit(c) || Character.isAlphabetic(c))
				nb.append(c);
			if (Character.isSpaceChar(c))
				nb.append('-');
		}
		return nb.toString();
	}

	@SneakyThrows
	private boolean writeBlog(BlogPost post, File file) {
		var content = this.buildContentsForBlogPost(post);
		var existingContent = (String) null;
		var exists = file.exists();
		if (exists) {
			try (var in = new FileReader(file)) {
				existingContent = FileCopyUtils.copyToString(in);
			}
		}

		var changed = false;
		if (!exists || !existingContent.equals(content)) {
			try (var out = new FileWriter(file)) {
				FileCopyUtils.copy(content, out);
				changed = true;
			}
		}
		return changed;
	}

}
