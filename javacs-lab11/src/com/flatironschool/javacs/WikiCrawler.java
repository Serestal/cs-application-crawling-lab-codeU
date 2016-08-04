package com.flatironschool.javacs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;



public class WikiCrawler {
	// keeps track of where we started
	private final String source;
	
	// the index where the results go
	private JedisIndex index;
	
	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();
	
	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();	
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param b 
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
        // FILL THIS IN!
        // Choose and remove a URL from the queue in FIFO order.
        String url = queue.poll();
        Elements paragraphs;

        if (testing) {
        	// Read the contents of the page using WikiFetcher.readWikipedia, which reads cached copies of pages we have included in this repository for testing purposes (to avoid problems if the Wikipedia version changes).
			// Index pages regardless of whether they are already indexed.
			// Find all the internal links on the page and add them to the queue in the order they appear. "Internal links" are links to other Wikipedia pages.
			// Return the URL of the page it indexed.
        	paragraphs = wf.readWikipedia(url);
        } else {
			// If the URL is already indexed, it should not index it again, and should return null.
			// Otherwise it should read the contents of the page using WikiFetcher.fetchWikipedia, which reads current content from the Web.
			// Then it should index the page, add links to the queue, and return the URL of the page it indexed.
        	if (index.isIndexed(url)) return null;
        	paragraphs = wf.fetchWikipedia(url);
        }
        index.indexPage(url, paragraphs);
        queueInternalLinks(paragraphs);
        return url;
	}
	
	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {
		int i = 0;
        // FILL THIS IN!
		for (Element paragraph : paragraphs) {
			Elements links = paragraph.select("a[href]");
			for (Element link : links) {
				String url = link.attr("href");
				if(url.startsWith("/wiki/")) {
					i++;
					queue.add("https://en.wikipedia.org" + url);
				}
			}
		}
    }

  //       // for each paragraph on the page
		// for (Element paragraph : paragraphs) {
		// 	// iterate through the dom tree


		// 	Iterable<Node> iter = new WikiNodeIterable(paragraph);
		// 	for (Node node: iter) {
		// 		if (node instanceof TextNode && isLink((TextNode) node)) {
		// 			String url = ((Element)node.parent()).attr("href");
		// 			// if this link is not the current link, return it!
		// 			if(!queue.contains(url) && !url.contains("#")){
		// 				queue.add(url);
		// 			} 
		// 		}
	 //        }
	 //    }

	/** 
	 *	Given a text node, returns true if it is a link that is not 
	 * 	in italics, parenthesis, or links to the current page.
	**/

	// private static boolean isLink(TextNode node) {
	// 	Element parent = (Element) node.parent();
	// 	return (parent.tagName() == "a");
	// }

	public static void main(String[] args) throws IOException {
		
		// make a WikiCrawler
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);
		
		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);

            // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
            break;
		} while (res == null);
		
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}
}
